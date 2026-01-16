package com.example.working_with_excels.excel.application.usecase;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.working_with_excels.excel.application.dto.ImportError;
import com.example.working_with_excels.excel.application.dto.ImportMetrics;
import com.example.working_with_excels.excel.application.dto.ImportReport;
import com.example.working_with_excels.excel.application.dto.RowProcessingResult;
import com.example.working_with_excels.excel.application.dto.SheetImportResult;
import com.example.working_with_excels.excel.application.port.input.ExcelImportUseCase;
import com.example.working_with_excels.excel.application.port.output.DatabasePort;
import com.example.working_with_excels.excel.application.port.output.ExcelConfigLoaderPort;
import com.example.working_with_excels.excel.domain.model.ErrorStrategy;
import com.example.working_with_excels.excel.domain.model.FileConfig;
import com.example.working_with_excels.excel.domain.model.FileSource;
import com.example.working_with_excels.excel.domain.model.FilesConfig;
import com.example.working_with_excels.excel.domain.model.ImportMode;
import com.example.working_with_excels.excel.domain.model.SheetConfig;

import lombok.RequiredArgsConstructor;

/**
 * Use case implementation for importing Excel data into database tables.
 *
 * <p>
 * This service orchestrates the Excel import workflow, delegating specific
 * responsibilities to specialized components:
 * <ul>
 * <li>{@link ExcelRowProcessor} - Row validation and value extraction</li>
 * <li>{@link SqlBuilder} - SQL statement generation</li>
 * <li>{@link BatchExecutor} - Batch execution with dry-run support</li>
 * <li>{@link ImportProgressTracker} - Progress logging</li>
 * </ul>
 *
 * <p>
 * Supports multiple file sources: classpath, filesystem, multipart upload, URL
 * download.
 */
@Service
@RequiredArgsConstructor
public class ExcelImportService implements ExcelImportUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExcelImportService.class);

    private final ExcelConfigLoaderPort configLoader;
    private final DatabasePort databasePort;
    private final ExcelRowProcessor rowProcessor;
    private final SqlBuilder sqlBuilder;
    private final BatchExecutor batchExecutor;
    private final ImportProgressTracker progressTracker;

    @Override
    public ImportReport importExcel(String excelFileName, String yamlConfigPath, ImportMode mode)
            throws IOException {
        return importExcel(
                FileSource.classpath(excelFileName),
                FileSource.classpath(yamlConfigPath),
                mode);
    }

    @Override
    public ImportReport importExcel(FileSource excelSource, FileSource configSource, ImportMode mode)
            throws IOException {

        Instant startTime = Instant.now();
        log.info("Starting Excel import: excel={}, config={}, mode={}",
                excelSource.description(), configSource.description(), mode);

        FilesConfig filesConfig;
        try (InputStream configStream = configSource.openStream()) {
            filesConfig = configLoader.loadConfigFromStream(configStream);
        }

        FileConfig fileConfig = filesConfig.files().getFirst();

        List<SheetImportResult> sheetResults = new ArrayList<>();
        ImportMetrics totalMetrics = ImportMetrics.empty();

        try (InputStream excelStream = excelSource.openStream();
                Workbook workbook = new XSSFWorkbook(excelStream)) {

            for (SheetConfig sheetConfig : fileConfig.sheets()) {
                if (sheetConfig.table() == null) {
                    log.debug("Skipping sheet '{}' - no table configured", sheetConfig.name());
                    continue;
                }

                SheetImportResult sheetResult = processSheet(workbook, sheetConfig, mode);
                sheetResults.add(sheetResult);
                totalMetrics = totalMetrics.combine(toMetrics(sheetResult));

                if (!sheetResult.isSuccess()
                        && sheetConfig.getEffectiveErrorStrategy() == ErrorStrategy.FAIL_ALL) {
                    log.error("Stopping import due to FAIL_ALL strategy on sheet '{}'", sheetConfig.name());
                    break;
                }
            }
        }

        Duration duration = Duration.between(startTime, Instant.now());
        log.info("Import completed: mode={}, duration={}ms, inserted={}, errors={}",
                mode, duration.toMillis(), totalMetrics.insertedRows(), totalMetrics.errorRows());

        return new ImportReport(excelSource.description(), configSource.description(),
                mode, duration, sheetResults, totalMetrics);
    }

    private SheetImportResult processSheet(Workbook workbook, SheetConfig sheetConfig, ImportMode mode) {
        Sheet sheet = workbook.getSheet(sheetConfig.name());
        if (sheet == null) {
            log.error("Sheet not found: {}", sheetConfig.name());
            return new SheetImportResult(sheetConfig.name(), sheetConfig.table(), 0, 0, 0,
                    List.of(ImportError.validation(0, null, "Sheet not found: " + sheetConfig.name())));
        }

        log.info("Processing sheet '{}' -> table '{}'", sheetConfig.name(), sheetConfig.table());

        List<ImportError> errors = new ArrayList<>();
        List<Map<String, Object>> batch = new ArrayList<>();
        int totalRows = 0;
        int insertedRows = 0;
        int skippedRows = 0;
        int lastRowNum = sheet.getLastRowNum();
        int batchSize = sheetConfig.getEffectiveBatchSize();

        String sql = sheetConfig.hasCustomSql()
                ? sheetConfig.customSql()
                : sqlBuilder.buildInsertSql(sheetConfig);

        if (mode == ImportMode.EXECUTE) {
            databasePort.beginTransaction();
        }

        progressTracker.reset();

        try {
            for (int rowIdx = 1; rowIdx <= lastRowNum; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }
                totalRows++;

                progressTracker.trackProgress(totalRows, lastRowNum, sheetConfig.name());

                RowProcessingResult result = rowProcessor.processRow(row, rowIdx + 1, sheetConfig);

                if (result.skipped()) {
                    skippedRows++;
                    continue;
                }

                if (!result.isValid()) {
                    errors.addAll(result.errors());
                    skippedRows++;

                    if (sheetConfig.getEffectiveErrorStrategy() == ErrorStrategy.FAIL_SHEET) {
                        log.warn("Stopping sheet '{}' due to FAIL_SHEET strategy", sheetConfig.name());
                        break;
                    }
                    continue;
                }

                batch.add(result.namedParams());

                if (batch.size() >= batchSize) {
                    int inserted = batchExecutor.executeBatch(sql, batch, mode);
                    insertedRows += inserted;
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                int inserted = batchExecutor.executeBatch(sql, batch, mode);
                insertedRows += inserted;
            }

            if (mode == ImportMode.EXECUTE) {
                databasePort.commitTransaction();
            }

        } catch (Exception e) {
            log.error("Error processing sheet '{}': {}", sheetConfig.name(), e.getMessage());
            if (mode == ImportMode.EXECUTE) {
                databasePort.rollbackTransaction();
            }
            errors.add(ImportError.database(0, "Transaction rolled back: " + e.getMessage()));
        }

        log.info("Sheet '{}' completed: total={}, inserted={}, skipped={}, errors={}",
                sheetConfig.name(), totalRows, insertedRows, skippedRows, errors.size());

        return new SheetImportResult(sheetConfig.name(), sheetConfig.table(),
                totalRows, insertedRows, skippedRows, errors);
    }

    private ImportMetrics toMetrics(SheetImportResult result) {
        return new ImportMetrics(
                result.totalRows(),
                result.insertedRows(),
                result.skippedRows(),
                result.errors().size(),
                0,
                0);
    }
}
