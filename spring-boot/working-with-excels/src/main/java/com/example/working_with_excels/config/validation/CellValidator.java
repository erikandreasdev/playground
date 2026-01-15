package com.example.working_with_excels.config.validation;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.stereotype.Component;

@Component
public class CellValidator {

    public String validate(Cell cell, ColumnConfig colConfig) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
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
}
