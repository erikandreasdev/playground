package com.example.demo.core.internal.domain.result;

import java.util.List;
import java.util.Map;

/**
 * Result of validating a single Excel row.
 *
 * <p>Contains validation outcome, collected errors, and row data for valid rows.
 *
 * @param isValid whether the row passed all validations
 * @param rowData map of column name to typed value (only populated if valid)
 * @param errors list of validation error messages for this row
 */
public record RowValidationResult(
    boolean isValid, Map<String, Object> rowData, List<String> errors) {

  /**
   * Compact constructor with validation.
   *
   * @throws IllegalArgumentException if rowData or errors is null
   */
  public RowValidationResult {
    if (rowData == null) {
      throw new IllegalArgumentException("rowData cannot be null");
    }
    if (errors == null) {
      throw new IllegalArgumentException("errors cannot be null");
    }
  }
}
