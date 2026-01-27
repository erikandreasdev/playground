package com.example.working_with_excels.excel.domain.model;

/**
 * Configuration for mapping an Excel column to a database column.
 *
 * <p>
 * Defines how cell values should be stored in the target database table,
 * including optional type overrides, lookup configurations, and pre-existence
 * checks.
 *
 * @param dbColumn the target column name in the database table
 * @param dbType   optional database-specific type override (e.g., VARCHAR2,
 *                 NUMBER)
 * @param lookup   optional lookup configuration for resolving values to IDs
 * @param existsIn optional configuration to validate value existence in a
 *                 reference table
 */
public record DbColumnMapping(
        String dbColumn,
        String dbType,
        LookupConfig lookup,
        ExistsInConfig existsIn) {

    /**
     * Creates a simple mapping with just the column name.
     *
     * @param dbColumn the target database column name
     * @return a new DbColumnMapping with no type override or lookup
     */
    public static DbColumnMapping of(String dbColumn) {
        return new DbColumnMapping(dbColumn, null, null, null);
    }
}
