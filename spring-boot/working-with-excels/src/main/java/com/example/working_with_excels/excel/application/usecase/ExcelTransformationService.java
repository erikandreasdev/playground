package com.example.working_with_excels.excel.application.usecase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.example.working_with_excels.excel.application.dto.ExcelTransformationResult;
import com.example.working_with_excels.excel.application.dto.SheetTransformationResult;
import com.example.working_with_excels.excel.application.dto.TransformedRow;
import com.example.working_with_excels.excel.application.port.input.ExcelTransformationUseCase;
import com.example.working_with_excels.excel.application.port.output.ExcelConfigLoaderPort;
import com.example.working_with_excels.excel.domain.model.ColumnConfig;
import com.example.working_with_excels.excel.domain.model.FileConfig;
import com.example.working_with_excels.excel.domain.model.FilesConfig;
import com.example.working_with_excels.excel.domain.model.SheetConfig;
import com.example.working_with_excels.excel.domain.service.CellTransformer;

import lombok.RequiredArgsConstructor;

/**
 * Use case implementation for transforming Excel cell values.
 *
 * <p>
 * This service implements the {@link ExcelTransformationUseCase} port and
 * orchestrates the transformation process using domain services and output
 * ports.
 */
@Service
@RequiredArgsConstructor
public class ExcelTransformationService implements ExcelTransformationUseCase {

    private final ExcelConfigLoaderPort configLoader;
    private final CellTransformer cellTransformer;

    @Override
    public ExcelTransformationResult transformExcel(String excelFileName, String yamlConfigPath)
            throws IOException {
        // 1. Load Config
        FilesConfig filesConfig = configLoader.loadConfig(yamlConfigPath);
        FileConfig fileConfig = configLoader.findFileConfig(filesConfig, excelFileName);

        List<SheetTransformationResult> sheetResults = new ArrayList<>();

        // 2. Load Excel File
        ClassPathResource excelResource = new ClassPathResource(excelFileName);
        try (InputStream excelStream = excelResource.getInputStream();
                Workbook workbook = new XSSFWorkbook(excelStream)) {

            // 3. Transform each sheet
            for (SheetConfig sheetConfig : fileConfig.sheets()) {
                sheetResults.add(transformSheet(workbook, sheetConfig));
            }
        }

        return new ExcelTransformationResult(excelFileName, yamlConfigPath, sheetResults);
    }

    private SheetTransformationResult transformSheet(Workbook workbook, SheetConfig sheetConfig) {
        Sheet sheet = workbook.getSheet(sheetConfig.name());
        if (sheet == null) {
            throw new IllegalArgumentException("Missing sheet: " + sheetConfig.name());
        }

        List<TransformedRow> transformedRows = new ArrayList<>();
        int lastRowNum = sheet.getLastRowNum();

        // Process data rows (skip header at row 0)
        for (int i = 1; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            TransformedRow transformedRow = transformRow(row, i + 1, sheetConfig.columns());
            transformedRows.add(transformedRow);
        }

        return new SheetTransformationResult(sheetConfig.name(), transformedRows.size(), transformedRows);
    }

    private TransformedRow transformRow(Row row, int rowNumber, List<ColumnConfig> columns) {
        Map<String, String> values = new LinkedHashMap<>();

        for (int i = 0; i < columns.size(); i++) {
            ColumnConfig colConfig = columns.get(i);
            Cell cell = row.getCell(i);

            String transformedValue = cellTransformer.transform(cell, colConfig.transformations());
            values.put(colConfig.name(), transformedValue);
        }

        return new TransformedRow(rowNumber, values);
    }
}
