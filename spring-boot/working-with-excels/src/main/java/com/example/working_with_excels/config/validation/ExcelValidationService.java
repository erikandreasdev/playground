package com.example.working_with_excels.config.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
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

    public ExcelValidationReport validateExcelStructure(String excelFileName, String yamlConfigPath)
            throws IOException {
        // 1. Load YAML Config
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        FilesConfig filesConfig;
        try (InputStream yamlStream = new ClassPathResource(yamlConfigPath).getInputStream()) {
            filesConfig = mapper.readValue(yamlStream, FilesConfig.class);
        }

        // 2. Find Config for this specific file
        FileConfig fileConfig = filesConfig.files().stream()
                .filter(f -> f.filename().equals(excelFileName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No configuration found for file: " + excelFileName));

        // 3. Load Excel File and Calculate Size
        ClassPathResource excelResource = new ClassPathResource(excelFileName);
        long fileSizeBytes = excelResource.contentLength();
        String fileSizeFormatted = formatFileSize(fileSizeBytes);

        List<SheetValidationReport> sheetReports = new ArrayList<>();

        try (InputStream excelStream = excelResource.getInputStream();
                Workbook workbook = new XSSFWorkbook(excelStream)) {

            // 4. Validate Sheets
            for (SheetConfig sheetConfig : fileConfig.sheets()) {
                sheetReports.add(validateSheet(workbook, sheetConfig));
            }
        }

        return new ExcelValidationReport(excelFileName, yamlConfigPath, fileSizeFormatted, sheetReports);
    }

    private SheetValidationReport validateSheet(Workbook workbook, SheetConfig sheetConfig) {
        Sheet sheet = workbook.getSheet(sheetConfig.name());
        if (sheet == null) {
            // Treat missing sheet as a major error, or just return an empty report with
            // error?
            // For now let's replicate previous behavior: throw exception or handle
            // gracefully.
            // The prompt asks for a report, so let's log a "sheet missing" error as a
            // generic row error or similar?
            // Actually, if the sheet is missing, we can't really validate rows.
            // Let's throw for now as it makes the structure invalid.
            throw new IllegalArgumentException("Missing sheet: " + sheetConfig.name());
        }

        List<RowValidationError> rowErrors = new ArrayList<>();
        int totalRows = 0;
        int validRows = 0;
        int lastRowNum = sheet.getLastRowNum(); // 0-based index

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
        // Note: getLastRowNum() returns the index of the last row (0-based)
        for (int i = 1; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue; // Skip empty rows? or count as invalid?
            }
            totalRows++;

            String rowError = validateRow(row, sheetConfig.columns());
            if (rowError != null) {
                rowErrors.add(new RowValidationError(i + 1, rowError)); // Use 1-based index for reporting
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

            if (cell == null || cell.getCellType() == CellType.BLANK) {
                // For now, assuming all columns are required.
                return "Missing value at column: " + colConfig.name();
            }

            if (!isValidType(cell, colConfig.type())) {
                return String.format("Invalid type for column '%s'. Expected %s but found %s",
                        colConfig.name(), colConfig.type(), getCellTypeDescription(cell));
            }

            if (colConfig.validation() != null) {
                String ruleError = validateRules(cell, colConfig.validation());
                if (ruleError != null) {
                    return String.format("Validation failed for column '%s': %s", colConfig.name(), ruleError);
                }
            }
        }
        return null;
    }

    private boolean isValidType(Cell cell, ExcelColumnType expectedType) {
        return switch (expectedType) {
            case STRING -> cell.getCellType() == CellType.STRING;
            case EMAIL -> cell.getCellType() == CellType.STRING && isValidEmail(cell.getStringCellValue());
            case INTEGER -> cell.getCellType() == CellType.NUMERIC && isInteger(cell.getNumericCellValue());
            case DECIMAL -> cell.getCellType() == CellType.NUMERIC;
            case BOOLEAN -> cell.getCellType() == CellType.BOOLEAN;
            case DATE -> DateUtil.isCellDateFormatted(cell);
        };
    }

    private String validateRules(Cell cell, ColumnValidation validation) {
        // Resolve value for String-based checks (Regex, Length)
        String stringValue = null;
        if (cell.getCellType() == CellType.STRING) {
            stringValue = cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                stringValue = cell.getLocalDateTimeCellValue().toString();
            } else {
                stringValue = String.valueOf(cell.getNumericCellValue());
            }
        }

        // Regex Validation
        if (validation.regex() != null && !validation.regex().isEmpty() && stringValue != null) {
            if (!stringValue.matches(validation.regex())) {
                return "Value '" + stringValue + "' does not match regex: " + validation.regex();
            }
        }

        // Length Validation (Strings)
        if ((validation.minLength() != null || validation.maxLength() != null) && stringValue != null) {
            int length = stringValue.length();
            if (validation.minLength() != null && length < validation.minLength()) {
                return "Value length " + length + " is less than min length " + validation.minLength();
            }
            if (validation.maxLength() != null && length > validation.maxLength()) {
                return "Value length " + length + " exceeds max length " + validation.maxLength();
            }
        }

        // Numeric Range Validation
        if ((validation.min() != null || validation.max() != null) && cell.getCellType() == CellType.NUMERIC
                && !DateUtil.isCellDateFormatted(cell)) {
            double value = cell.getNumericCellValue();
            if (validation.min() != null && value < validation.min()) {
                return "Value " + value + " is less than min " + validation.min();
            }
            if (validation.max() != null && value > validation.max()) {
                return "Value " + value + " exceeds max " + validation.max();
            }
        }

        return null;
    }

    private boolean isInteger(double value) {
        return value == Math.floor(value) && !Double.isInfinite(value);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private String getCellTypeDescription(Cell cell) {
        if (cell == null)
            return "NULL";
        return switch (cell.getCellType()) {
            case STRING -> "STRING (" + cell.getStringCellValue() + ")";
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? "DATE" : "NUMBER (" + cell.getNumericCellValue() + ")";
            case BOOLEAN -> "BOOLEAN (" + cell.getBooleanCellValue() + ")";
            default -> cell.getCellType().toString();
        };
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
