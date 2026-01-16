package com.example.working_with_excels.excel.domain.model;

/**
 * Value object representing a validation error for a specific row.
 *
 * <p>
 * This record captures the row number (1-indexed) and the error
 * message describing the validation failure.
 *
 * @param rowNumber    the 1-indexed row number where the error occurred
 * @param errorMessage the description of the validation failure
 */
public record RowValidationError(int rowNumber, String errorMessage) {
}
