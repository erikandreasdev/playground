package com.example.working_with_excels.excel.application.dto;

import java.util.List;

/**
 * Data Transfer Object representing a complete transformation result for an
 * Excel file.
 *
 * <p>
 * This record contains file metadata and transformation results for all sheets.
 *
 * @param filename    the name of the processed Excel file
 * @param mappingFile the path to the YAML configuration file used
 * @param sheets      list of transformation results for each sheet
 */
public record ExcelTransformationResult(
        String filename,
        String mappingFile,
        List<SheetTransformationResult> sheets) {
}
