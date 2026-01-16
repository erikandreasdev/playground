package com.example.working_with_excels.excel.application.usecase;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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
import com.example.working_with_excels.excel.application.dto.SheetImportResult;
import com.example.working_with_excels.excel.application.port.input.ExcelImportUseCase;
import com.example.working_with_excels.excel.application.port.output.DatabasePort;
import com.example.working_with_excels.excel.application.port.output.ExcelConfigLoaderPort;
import com.example.working_with_excels.excel.domain.model.ColumnConfig;
import com.example.working_with_excels.excel.domain.model.DbColumnMapping;
import com.example.working_with_excels.excel.domain.model.ErrorStrategy;
import com.example.working_with_excels.excel.domain.model.FileConfig;
import com.example.working_with_excels.excel.domain.model.FileSource;
import com.example.working_with_excels.excel.domain.model.FilesConfig;
import com.example.working_with_excels.excel.domain.model.ImportMode;
import com.example.working_with_excels.excel.domain.model.LookupConfig;
import com.example.working_with_excels.excel.domain.model.SheetConfig;
import com.example.working_with_excels.excel.domain.service.CellTransformer;
import com.example.working_with_excels.excel.domain.service.CellValidator;

import lombok.RequiredArgsConstructor;

/**
 * Use case implementation for importing Excel data into database tables.
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
    private final CellValidator cellValidator;
    private final CellTransformer cellTransformer;

    @Override
    public ImportReport importExcel(String excelFileName, String yamlConfigPath, ImportMode mode)
            throws IOException {
        // Delegate to FileSource-based method for backward compatibility
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

        // Load config from source
        FilesConfig filesConfig;
        try (InputStream configStream = configSource.openStream()) {
            filesConfig = configLoader.loadConfigFromStream(configStream);
        }

        // Find matching file config (use first file config if source is not classpath)
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
        int lastLoggedPercentage = 0;

        String sql = sheetConfig.hasCustomSql()
                ? sheetConfig.customSql()
                : buildInsertSql(sheetConfig);

        if (mode == ImportMode.EXECUTE) {
            databasePort.beginTransaction();
        }

        try {
            for (int rowIdx = 1; rowIdx <= lastRowNum; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }
                totalRows++;

                int percentage = (totalRows * 100) / lastRowNum;
                if (percentage >= lastLoggedPercentage + 10) {
                    lastLoggedPercentage = (percentage / 10) * 10;
                    log.info("Progress: {}% ({}/{} rows)", lastLoggedPercentage, totalRows, lastRowNum);
                }

                RowProcessingResult result = processRow(row, rowIdx + 1, sheetConfig.columns());

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
                    int inserted = executeBatch(sql, batch, mode);
                    insertedRows += inserted;
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                int inserted = executeBatch(sql, batch, mode);
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

    private RowProcessingResult processRow(Row row, int rowNumber, List<ColumnConfig> columns) {
        List<ImportError> errors = new ArrayList<>();
        Map<String, Object> namedParams = new HashMap<>();

        for (int i = 0; i < columns.size(); i++) {
            ColumnConfig colConfig = columns.get(i);
            Cell cell = row.getCell(i);

            if (colConfig.dbMapping() == null) {
                continue;
            }

            String validationError = cellValidator.validate(cell, colConfig);
            if (validationError != null) {
                errors.add(ImportError.validation(rowNumber, colConfig.name(), validationError));
                continue;
            }

            // Transform after validation passes - extract typed value with transformations
            Object cellValue = extractTypedValue(cell, colConfig);

            // Validate transformed value (allowedValues, excludedValues work on final
            // value)
            String transformedValidationError = cellValidator.validateTransformedValue(cellValue, colConfig);
            if (transformedValidationError != null) {
                errors.add(ImportError.validation(rowNumber, colConfig.name(), transformedValidationError));
                continue;
            }

            Object finalValue = cellValue;
            DbColumnMapping mapping = colConfig.dbMapping();
            if (mapping.lookup() != null) {
                LookupConfig lookup = mapping.lookup();
                String lookupKey = cellValue != null ? cellValue.toString() : null;
                Optional<Object> lookedUp = databasePort.lookup(
                        lookup.table(), lookup.matchColumn(), lookupKey, lookup.returnColumn());

                if (lookedUp.isEmpty()) {
                    errors.add(ImportError.lookup(rowNumber, colConfig.name(),
                            String.format("Lookup failed: no match for '%s' in %s.%s",
                                    lookupKey, lookup.table(), lookup.matchColumn())));
                    continue;
                }
                finalValue = lookedUp.get();
            }

            namedParams.put(mapping.dbColumn(), finalValue);
        }

        if (!errors.isEmpty()) {
            return new RowProcessingResult(false, null, errors);
        }

        return new RowProcessingResult(true, namedParams, List.of());
    }

    private Object extractTypedValue(Cell cell, ColumnConfig colConfig) {
        if (cell == null) {
            return null;
        }

        return switch (colConfig.type()) {
            case DATE -> cell.getCellType() == CellType.NUMERIC
                    ? cell.getDateCellValue()
                    : null;
            case INTEGER -> cell.getCellType() == CellType.NUMERIC
                    ? (int) cell.getNumericCellValue()
                    : null;
            case DECIMAL -> cell.getCellType() == CellType.NUMERIC
                    ? cell.getNumericCellValue()
                    : null;
            case BOOLEAN -> cell.getCellType() == CellType.BOOLEAN
                    ? (cell.getBooleanCellValue() ? 1 : 0)
                    : null;
            case STRING, EMAIL -> cellTransformer.transform(cell, colConfig.transformations());
        };
    }

    private int executeBatch(String sql, List<Map<String, Object>> batch, ImportMode mode) {
        if (mode == ImportMode.DRY_RUN) {
            log.info("[DRY_RUN] Would execute {} inserts", batch.size());
            if (log.isDebugEnabled()) {
                for (Map<String, Object> params : batch) {
                    log.debug("[DRY_RUN] SQL: {} | Params: {}", sql, params);
                }
            }
            return batch.size();
        }

        return databasePort.executeBatch(sql, batch);
    }

    private String buildInsertSql(SheetConfig sheetConfig) {
        List<String> dbColumns = sheetConfig.columns().stream()
                .filter(c -> c.dbMapping() != null)
                .map(c -> c.dbMapping().dbColumn())
                .toList();

        String columns = String.join(", ", dbColumns);
        String placeholders = dbColumns.stream()
                .map(col -> ":" + col)
                .collect(Collectors.joining(", "));

        return String.format("INSERT INTO %s (%s) VALUES (%s)", sheetConfig.table(), columns, placeholders);
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

    private record RowProcessingResult(
            boolean isValid,
            Map<String, Object> namedParams,
            List<ImportError> errors) {
    }
}
