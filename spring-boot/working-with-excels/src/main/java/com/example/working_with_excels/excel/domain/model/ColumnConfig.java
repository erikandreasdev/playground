package com.example.working_with_excels.excel.domain.model;

/**
 * Value object representing configuration for a single Excel column.
 *
 * <p>
 * This record defines the expected name, data type, and validation
 * rules for a column within an Excel sheet.
 *
 * @param name       the expected header name of the column
 * @param type       the expected data type of values in this column
 * @param validation optional validation rules for column values
 */
public record ColumnConfig(String name, ExcelColumnType type, ColumnValidation validation) {
}
