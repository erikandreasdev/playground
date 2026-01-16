package com.example.working_with_excels.excel.domain.model;

import java.util.List;

/**
 * Value object representing configuration for a single Excel column.
 *
 * <p>
 * This record defines the expected name, data type, validation rules,
 * and transformations for a column within an Excel sheet.
 *
 * @param name            the expected header name of the column
 * @param type            the expected data type of values in this column
 * @param validation      optional validation rules for column values
 * @param transformations optional list of transformations to apply (in order)
 */
public record ColumnConfig(
        String name,
        ExcelColumnType type,
        ColumnValidation validation,
        List<ColumnTransformation> transformations) {

    /**
     * Creates a ColumnConfig without transformations (for backward compatibility).
     *
     * @param name       the expected header name
     * @param type       the expected data type
     * @param validation optional validation rules
     * @return a new ColumnConfig with no transformations
     */
    public static ColumnConfig of(String name, ExcelColumnType type, ColumnValidation validation) {
        return new ColumnConfig(name, type, validation, null);
    }
}
