package com.example.demo.domain;

import java.util.List;

/**
 * Configuration for a specific column.
 *
 * @param name Name of the column header
 * @param transformations List of transformations to apply
 * @param rules List of validation rules to apply
 * @param type Expected data type
 * @param allowedValues List of allowed values
 * @param required Whether the column is required to exist and be valid
 * @param dbLookup Optional database lookup validation
 */
public record ColumnConfig(
    String name,
    List<TransformationType> transformations,
    List<ValidationRule> rules,
    DataType type,
    List<String> allowedValues,
    boolean required,
    DbLookup dbLookup) {

  /** Constructor with defaults for optional fields. */
  public ColumnConfig {
    if (transformations == null) {
      transformations = List.of();
    }
    if (rules == null) {
      rules = List.of();
    }
    if (allowedValues == null) {
      allowedValues = List.of();
    }
    if (type == null) {
      type = DataType.STRING;
    }
  }
}
