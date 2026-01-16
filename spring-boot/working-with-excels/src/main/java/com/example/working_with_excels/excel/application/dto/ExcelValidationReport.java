package com.example.working_with_excels.excel.application.dto;

import java.util.List;

/**
 * Data Transfer Object representing the complete validation report for an Excel
 * file.
 *
 * <p>
 * This record contains metadata about the validated file along with
 * validation reports for each sheet within the file.
 *
 * @param filename          the name of the validated Excel file
 * @param mappingFile       the path to the YAML configuration file used
 * @param fileSizeFormatted the human-readable file size (e.g., "1.5 MB")
 * @param sheets            list of validation reports for each sheet
 */
public record ExcelValidationReport(
        String filename,
        String mappingFile,
        String fileSizeFormatted,
        List<SheetValidationReport> sheets) {
}
