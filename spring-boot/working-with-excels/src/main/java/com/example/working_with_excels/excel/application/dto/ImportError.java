package com.example.working_with_excels.excel.application.dto;

/**
 * Details of an error that occurred during row import.
 *
 * @param rowNumber    the 1-indexed row number where the error occurred
 * @param columnName   the column name associated with the error (if applicable)
 * @param errorMessage description of what went wrong
 * @param errorType    classification of the error (VALIDATION, LOOKUP,
 *                     DATABASE)
 */
public record ImportError(
        int rowNumber,
        String columnName,
        String errorMessage,
        ErrorType errorType) {

    /**
     * Classification of import errors.
     */
    public enum ErrorType {
        /** Validation rule failed. */
        VALIDATION,
        /** Database lookup failed to find a matching value. */
        LOOKUP,
        /** Database insert operation failed. */
        DATABASE
    }

    /**
     * Creates a validation error.
     *
     * @param rowNumber    the row number
     * @param columnName   the column name
     * @param errorMessage the error message
     * @return a new ImportError of type VALIDATION
     */
    public static ImportError validation(int rowNumber, String columnName, String errorMessage) {
        return new ImportError(rowNumber, columnName, errorMessage, ErrorType.VALIDATION);
    }

    /**
     * Creates a lookup error.
     *
     * @param rowNumber    the row number
     * @param columnName   the column name
     * @param errorMessage the error message
     * @return a new ImportError of type LOOKUP
     */
    public static ImportError lookup(int rowNumber, String columnName, String errorMessage) {
        return new ImportError(rowNumber, columnName, errorMessage, ErrorType.LOOKUP);
    }

    /**
     * Creates a database error.
     *
     * @param rowNumber    the row number
     * @param errorMessage the error message
     * @return a new ImportError of type DATABASE
     */
    public static ImportError database(int rowNumber, String errorMessage) {
        return new ImportError(rowNumber, null, errorMessage, ErrorType.DATABASE);
    }
}
