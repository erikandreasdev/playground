package com.example.working_with_excels.excel.domain.model;

/**
 * Strategy for handling errors during Excel import.
 *
 * <p>
 * Configurable per sheet in the YAML mapping file to control
 * how the import process responds to validation or database errors.
 */
public enum ErrorStrategy {

    /**
     * Log the error and continue processing remaining rows.
     */
    SKIP_ROW,

    /**
     * Stop processing current sheet but continue with other sheets.
     */
    FAIL_SHEET,

    /**
     * Stop the entire import process immediately.
     */
    FAIL_ALL
}
