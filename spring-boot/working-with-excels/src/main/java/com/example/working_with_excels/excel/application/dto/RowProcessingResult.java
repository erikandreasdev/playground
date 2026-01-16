package com.example.working_with_excels.excel.application.dto;

import java.util.List;
import java.util.Map;

/**
 * Result of processing a single Excel row.
 *
 * <p>
 * Encapsulates the outcome of row validation and transformation,
 * including the extracted named parameters for database insertion
 * or validation errors if processing failed.
 */
public record RowProcessingResult(
        boolean isValid,
        Map<String, Object> namedParams,
        List<ImportError> errors,
        boolean skipped) {

    /**
     * Creates a successful processing result with the extracted parameters.
     *
     * @param params the named parameters map for database insertion
     * @return a valid RowProcessingResult
     */
    public static RowProcessingResult valid(Map<String, Object> params) {
        return new RowProcessingResult(true, params, List.of(), false);
    }

    /**
     * Creates a successful but skipped processing result.
     *
     * @return a skipped RowProcessingResult
     */
    public static RowProcessingResult ofSkipped() {
        return new RowProcessingResult(true, Map.of(), List.of(), true);
    }

    /**
     * Creates a failed processing result with validation errors.
     *
     * @param errors the list of validation errors encountered
     * @return an invalid RowProcessingResult
     */
    public static RowProcessingResult invalid(List<ImportError> errors) {
        return new RowProcessingResult(false, null, errors, false);
    }
}
