package com.example.working_with_excels.excel.domain.model;

/**
 * Configuration for database lookups during Excel row processing.
 *
 * <p>
 * Enables resolving cell values to database IDs by querying
 * a reference table. For example, converting a country name to
 * its corresponding country ID.
 *
 * @param table        the database table to query
 * @param matchColumn  the column to match against the cell value
 * @param returnColumn the column value to return (typically an ID)
 */
public record LookupConfig(
        String table,
        String matchColumn,
        String returnColumn) {
}
