package com.example.working_with_excels.excel.domain.model;

/**
 * Enumeration representing the valid data types for Excel column values.
 *
 * <p>
 * This enum defines the set of supported column types that can be used
 * for validation purposes when processing Excel files.
 */
public enum ExcelColumnType {
    INTEGER,
    STRING,
    DECIMAL,
    BOOLEAN,
    DATE,
    EMAIL
}
