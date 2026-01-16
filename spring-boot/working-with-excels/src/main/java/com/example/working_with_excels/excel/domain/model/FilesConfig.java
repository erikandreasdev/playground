package com.example.working_with_excels.excel.domain.model;

import java.util.List;

/**
 * Root configuration object containing multiple Excel file configurations.
 *
 * <p>
 * This record serves as the top-level container for all Excel file
 * configurations loaded from a YAML mapping file.
 *
 * @param files the list of file configurations
 */
public record FilesConfig(List<FileConfig> files) {
}
