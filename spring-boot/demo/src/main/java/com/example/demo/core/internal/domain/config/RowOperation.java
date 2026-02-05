package com.example.demo.core.internal.domain.config;

import java.util.List;

/**
 * Represents a row-level operation that computes a new column value from existing columns.
 *
 * <p>Row operations allow creating computed columns that don't exist in the original Excel file.
 * These computed columns are created by applying a sequence of operations on values from existing
 * columns.
 *
 * <p>Example: Create a "Customer ID" column by concatenating "ID Prefix" + "-" + "ID Number", then
 * converting to uppercase.
 *
 * @param targetColumn Name of the computed column to create (will be added to row data)
 * @param operations List of operations to apply sequentially (order matters)
 */
public record RowOperation(String targetColumn, List<Operation> operations) {

  /**
   * Compact constructor with validation.
   *
   * @throws IllegalArgumentException if targetColumn is null/empty or operations list is null/empty
   */
  public RowOperation {
    if (targetColumn == null || targetColumn.isBlank()) {
      throw new IllegalArgumentException("targetColumn cannot be null or blank");
    }
    if (operations == null || operations.isEmpty()) {
      throw new IllegalArgumentException("operations list cannot be null or empty");
    }
  }
}
