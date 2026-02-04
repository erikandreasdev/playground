package com.example.demo.domain;

import java.util.List;

/**
 * Configuration for a specific sheet.
 *
 * @param name Name of the sheet
 * @param columns List of column configurations
 * @param persistence Optional configuration for database persistence
 * @param rowOperations Optional list of row-level operations to compute additional columns
 */
public record SheetConfig(
    String name,
    List<ColumnConfig> columns,
    PersistenceConfig persistence,
    List<RowOperation> rowOperations) {

  /**
   * Compact constructor with defaults for optional fields.
   *
   * <p>rowOperations defaults to an empty list if not specified in YAML configuration.
   */
  public SheetConfig {
    if (rowOperations == null) {
      rowOperations = List.of();
    }
  }
}
