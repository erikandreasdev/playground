package com.example.working_with_excels.excel.domain.model;

/**
 * Configuration for checking if a value exists in a database table.
 * 
 * @param table        The table to check in
 * @param column       The column to check against
 * @param errorMessage Custom error message if existence check fails
 */
public record ExistsInConfig(
        String table,
        String column,
        String errorMessage) {
}
