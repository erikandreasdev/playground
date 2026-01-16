package com.example.working_with_excels.excel.application.dto;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object representing a transformed Excel row.
 *
 * <p>
 * This record contains the row number and a map of column names
 * to their transformed values.
 *
 * @param rowNumber the 1-indexed row number
 * @param values    map of column names to transformed cell values
 */
public record TransformedRow(
        int rowNumber,
        Map<String, String> values) {
}
