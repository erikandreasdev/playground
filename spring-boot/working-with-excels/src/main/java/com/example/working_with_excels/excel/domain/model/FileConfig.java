package com.example.working_with_excels.excel.domain.model;

import java.util.List;

/**
 * Value object representing configuration for a single Excel file.
 *
 * <p>
 * This record defines the expected filename and the list of sheet
 * configurations that describe the expected structure of the file.
 *
 * @param filename the name of the Excel file
 * @param sheets   the list of sheet configurations for this file
 */
public record FileConfig(String filename, List<SheetConfig> sheets) {
}
