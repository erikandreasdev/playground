package com.example.working_with_excels.excel.application.dto;

import java.util.List;

import com.example.working_with_excels.excel.domain.model.RowValidationError;

/**
 * Data Transfer Object representing validation results for a single sheet.
 *
 * <p>
 * This record contains statistics about row validation outcomes and
 * a list of any validation errors encountered.
 *
 * @param sheetName   the name of the validated sheet
 * @param totalRows   total number of data rows processed
 * @param validRows   count of rows that passed validation
 * @param invalidRows count of rows that failed validation
 * @param rowErrors   list of detailed error records for failed rows
 */
public record SheetValidationReport(
        String sheetName,
        int totalRows,
        int validRows,
        int invalidRows,
        List<RowValidationError> rowErrors) {
}
