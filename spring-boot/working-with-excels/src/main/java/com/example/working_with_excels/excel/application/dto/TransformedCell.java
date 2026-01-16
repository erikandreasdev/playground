package com.example.working_with_excels.excel.application.dto;

/**
 * Data Transfer Object representing a transformed cell value.
 *
 * <p>
 * This record contains both the original and transformed values,
 * along with position information for the cell.
 *
 * @param rowNumber        the 1-indexed row number
 * @param columnName       the name of the column
 * @param originalValue    the original value before transformation
 * @param transformedValue the value after applying transformations
 */
public record TransformedCell(
        int rowNumber,
        String columnName,
        String originalValue,
        String transformedValue) {
}
