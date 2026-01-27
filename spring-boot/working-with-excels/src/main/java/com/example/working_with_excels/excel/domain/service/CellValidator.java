package com.example.working_with_excels.excel.domain.service;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;

import com.example.working_with_excels.excel.domain.model.ColumnConfig;
import com.example.working_with_excels.excel.domain.model.ColumnValidation;
import com.example.working_with_excels.excel.domain.model.ExcelColumnType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

/**
 * Domain service responsible for validating individual Excel cell values.
 *
 * <p>
 * This class encapsulates the core validation logic for cell values,
 * including type checking, regex matching, length constraints, numeric
 * ranges, and value restrictions. It is a pure domain service with no
 * framework dependencies.
 */
public class CellValidator {

    /**
     * Validates a cell value against the provided column configuration.
     *
     * @param cell      the Excel cell to validate (may be null)
     * @param colConfig the column configuration containing type and validation
     *                  rules
     * @return an error message if validation fails, or null if the cell is valid
     */
    public String validate(Cell cell, ColumnConfig colConfig) {
        // Not Empty Validation
        boolean isBlank = cell == null || cell.getCellType() == CellType.BLANK;
        if (colConfig.validation() != null
                && Boolean.TRUE.equals(colConfig.validation().notEmpty())
                && isBlank) {
            return "Value is required at column: " + colConfig.name();
        }

        // If blank and not required, skip further validation
        if (isBlank) {
            return null;
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

    /**
     * Validates a transformed value against the column configuration rules.
     *
     * <p>
     * This method should be called AFTER transformation to validate
     * the final value (e.g., after UPPERCASE transform, check against
     * allowedValues).
     *
     * @param value     the transformed value (may be null)
     * @param colConfig the column configuration
     * @return an error message if validation fails, or null if valid
     */
    public String validateTransformedValue(Object value, ColumnConfig colConfig) {
        if (colConfig.validation() == null) {
            return null;
        }

        ColumnValidation validation = colConfig.validation();
        String stringValue = value != null ? value.toString() : null;

        // Allowed Values (Enumerated) - check against transformed value
        if (validation.allowedValues() != null && !validation.allowedValues().isEmpty() && stringValue != null) {
            if (!validation.allowedValues().contains(stringValue)) {
                return String.format("Validation failed for column '%s': Value '%s' is not in the allowed list: %s",
                        colConfig.name(), stringValue, validation.allowedValues());
            }
        }

        // Excluded Values - check against transformed value
        if (validation.excludedValues() != null && !validation.excludedValues().isEmpty() && stringValue != null) {
            if (validation.excludedValues().contains(stringValue)) {
                return String.format("Validation failed for column '%s': Value '%s' is in the excluded list",
                        colConfig.name(), stringValue);
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

        // Date Validation (Past/Future)
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            LocalDateTime dateVal = cell.getLocalDateTimeCellValue();
            LocalDateTime now = LocalDateTime.now();
            if (Boolean.TRUE.equals(validation.past()) && !dateVal.isBefore(now)) {
                return "Date must be in the past";
            }
            if (Boolean.TRUE.equals(validation.future()) && !dateVal.isAfter(now)) {
                return "Date must be in the future";
            }
        }

        // NOTE: allowedValues and excludedValues are checked in
        // validateTransformedValue()
        // AFTER transformation, so we skip them here.

        return null;
    }

    private boolean isInteger(double value) {
        return value == Math.floor(value) && !Double.isInfinite(value);
    }

    private boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        EmailValidator validator = new EmailValidator();
        return validator.isValid(email, null);
    }

    private String getCellTypeDescription(Cell cell) {
        if (cell == null) {
            return "NULL";
        }
        return switch (cell.getCellType()) {
            case STRING -> "STRING (" + cell.getStringCellValue() + ")";
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? "DATE" : "NUMBER (" + cell.getNumericCellValue() + ")";
            case BOOLEAN -> "BOOLEAN (" + cell.getBooleanCellValue() + ")";
            default -> cell.getCellType().toString();
        };
    }
}
