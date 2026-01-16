package com.example.working_with_excels.excel.application.dto;

import java.util.List;

/**
 * Import result for a single sheet within an Excel file.
 *
 * @param sheetName    the name of the processed sheet
 * @param tableName    the target database table
 * @param totalRows    total number of data rows in the sheet
 * @param insertedRows number of rows successfully inserted
 * @param skippedRows  number of rows skipped due to errors
 * @param errors       list of errors encountered during processing
 */
public record SheetImportResult(
        String sheetName,
        String tableName,
        int totalRows,
        int insertedRows,
        int skippedRows,
        List<ImportError> errors) {

    /**
     * Checks if the sheet was imported without any errors.
     *
     * @return true if no errors occurred
     */
    public boolean isSuccess() {
        return errors == null || errors.isEmpty();
    }
}
