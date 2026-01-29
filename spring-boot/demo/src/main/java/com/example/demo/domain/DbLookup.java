package com.example.demo.domain;

/**
 * Represents a database lookup validation configuration.
 *
 * <p>This record defines how to validate a cell value by checking if it exists in a database table.
 * It supports checking a single column or multiple columns for composite keys.
 *
 * @param table the database table name to query
 * @param column the column name to check against (for single column lookup)
 * @param columns the column names to check against (for composite key lookup)
 * @param errorMessage custom error message when lookup fails
 */
public record DbLookup(
    String table, String column, java.util.List<String> columns, String errorMessage) {

  /**
   * Validates the DbLookup configuration.
   *
   * @throws IllegalArgumentException if configuration is invalid
   */
  public DbLookup {
    if (table == null || table.isBlank()) {
      throw new IllegalArgumentException("DbLookup table must not be blank");
    }

    // Must have either single column or multiple columns, but not both
    boolean hasSingleColumn = column != null && !column.isBlank();
    boolean hasMultipleColumns = columns != null && !columns.isEmpty();

    if (!hasSingleColumn && !hasMultipleColumns) {
      throw new IllegalArgumentException("DbLookup must specify either 'column' or 'columns'");
    }

    if (hasSingleColumn && hasMultipleColumns) {
      throw new IllegalArgumentException(
          "DbLookup cannot specify both 'column' and 'columns', use one");
    }
  }

  /**
   * Returns whether this is a single column lookup.
   *
   * @return true if single column, false if composite
   */
  public boolean isSingleColumn() {
    return column != null && !column.isBlank();
  }

  /**
   * Returns the default error message if none specified.
   *
   * @return the error message to use
   */
  public String getEffectiveErrorMessage() {
    if (errorMessage != null && !errorMessage.isBlank()) {
      return errorMessage;
    }
    if (isSingleColumn()) {
      return String.format("Value does not exist in %s.%s", table, column);
    }
    return String.format("Value does not exist in %s", table);
  }
}
