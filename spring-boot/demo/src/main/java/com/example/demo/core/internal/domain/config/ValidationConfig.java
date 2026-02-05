package com.example.demo.core.internal.domain.config;

import java.util.List;

/**
 * Configuration for Excel validation, mapped from YAML.
 *
 * @param excelFilename Expected filename of the Excel file
 * @param sheets List of sheet configurations
 */
public record ValidationConfig(String excelFilename, List<SheetConfig> sheets) {}
