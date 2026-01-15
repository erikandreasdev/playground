package com.example.working_with_excels.config.validation;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelValidationService {

    private final ExcelConfigLoader configLoader;
    private final CellValidator cellValidator;

    public ExcelValidationService(ExcelConfigLoader configLoader, CellValidator cellValidator) {
        this.configLoader = configLoader;
        this.cellValidator = cellValidator;
    }

    public ExcelValidationReport validateExcelStructure(String excelFileName, String yamlConfigPath)
            throws IOException {
        // 1. Load Config
        FilesConfig filesConfig = configLoader.loadConfig(yamlConfigPath);
        FileConfig fileConfig = configLoader.findFileConfig(filesConfig, excelFileName);

        // 2. Load Excel File and Calculate Size
        ClassPathResource excelResource = new ClassPathResource(excelFileName);
        long fileSizeBytes = excelResource.contentLength();
        String fileSizeFormatted = formatFileSize(fileSizeBytes);

        List<SheetValidationReport> sheetReports = new ArrayList<>();

        try (InputStream excelStream = excelResource.getInputStream();
                Workbook workbook = new XSSFWorkbook(excelStream)) {

            // 3. Validate Sheets
            for (SheetConfig sheetConfig : fileConfig.sheets()) {
                sheetReports.add(validateSheet(workbook, sheetConfig));
            }
        }

        return new ExcelValidationReport(excelFileName, yamlConfigPath, fileSizeFormatted, sheetReports);
    }

    private SheetValidationReport validateSheet(Workbook workbook, SheetConfig sheetConfig) {
        Sheet sheet = workbook.getSheet(sheetConfig.name());
        if (sheet == null) {
            throw new IllegalArgumentException("Missing sheet: " + sheetConfig.name());
        }

        List<RowValidationError> rowErrors = new ArrayList<>();
        int totalRows = 0;
        int validRows = 0;
        int lastRowNum = sheet.getLastRowNum();

        // Validate Header (Row 0)
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new IllegalArgumentException("Sheet " + sheetConfig.name() + " is empty or missing header row");
        }
        for (int i = 0; i < sheetConfig.columns().size(); i++) {
            ColumnConfig colConfig = sheetConfig.columns().get(i);
            Cell cell = headerRow.getCell(i);
            String headerValue = cell == null ? "" : cell.getStringCellValue();
            if (!headerValue.equalsIgnoreCase(colConfig.name())) {
                throw new IllegalArgumentException(
                        String.format("Column mismatch in sheet '%s' at index %d. Expected '%s', but found '%s'",
                                sheetConfig.name(), i, colConfig.name(), headerValue));
            }
        }

        // Validate Data Rows (Row 1 to End)
        for (int i = 1; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            totalRows++;

            String rowError = validateRow(row, sheetConfig.columns());
            if (rowError != null) {
                rowErrors.add(new RowValidationError(i + 1, rowError));
            } else {
                validRows++;
            }
        }

        int invalidRows = totalRows - validRows;
        return new SheetValidationReport(sheetConfig.name(), totalRows, validRows, invalidRows, rowErrors);
    }

    private String validateRow(Row row, List<ColumnConfig> columns) {
        for (int i = 0; i < columns.size(); i++) {
            ColumnConfig colConfig = columns.get(i);
            Cell cell = row.getCell(i);

            String error = cellValidator.validate(cell, colConfig);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
