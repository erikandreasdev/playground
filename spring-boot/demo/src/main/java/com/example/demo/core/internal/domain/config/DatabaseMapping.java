package com.example.demo.core.internal.domain.config;

/**
 * Maps an Excel column to a database column.
 *
 * @param excelColumn Name of the column in the Excel sheet
 * @param dbColumn Name of the column in the database table
 */
public record DatabaseMapping(String excelColumn, String dbColumn) {}
