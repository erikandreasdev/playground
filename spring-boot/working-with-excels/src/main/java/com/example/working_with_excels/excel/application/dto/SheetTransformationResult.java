package com.example.working_with_excels.excel.application.dto;

import java.util.List;

/**
 * Data Transfer Object representing a complete transformation result for a
 * sheet.
 *
 * <p>
 * This record contains metadata about the sheet and all transformed rows.
 *
 * @param sheetName       the name of the processed sheet
 * @param totalRows       total number of data rows processed
 * @param transformedRows list of transformed row data
 */
public record SheetTransformationResult(
        String sheetName,
        int totalRows,
        List<TransformedRow> transformedRows) {
}
