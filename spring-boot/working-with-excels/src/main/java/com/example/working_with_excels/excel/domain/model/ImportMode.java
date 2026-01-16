package com.example.working_with_excels.excel.domain.model;

/**
 * Execution mode for Excel import operations.
 *
 * <p>
 * Determines whether the import actually modifies the database
 * or just logs the SQL statements for testing purposes.
 */
public enum ImportMode {

    /**
     * Execute actual database inserts.
     */
    EXECUTE,

    /**
     * Log SQL statements without executing them (dry run).
     */
    DRY_RUN
}
