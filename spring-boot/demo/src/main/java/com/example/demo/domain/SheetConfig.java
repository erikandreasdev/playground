package com.example.demo.domain;

import java.util.List;

/**
 * Configuration for a specific sheet.
 *
 * @param name Name of the sheet
 * @param columns List of column configurations
 * @param persistence Optional configuration for database persistence
 */
public record SheetConfig(String name, List<ColumnConfig> columns, PersistenceConfig persistence) {}
