package com.example.demo.validation;

import com.example.demo.domain.DbLookup;
import com.example.demo.service.JdbcDatabaseAdapter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Validator for database lookup validations.
 *
 * <p>This validator checks if a cell value (or combination of values) exists in a database table.
 * It supports both single-column and composite-key lookups.
 */
@Component
public class DbLookupValidator {

  private final JdbcDatabaseAdapter databaseAdapter;

  /**
   * Constructs the validator.
   *
   * @param databaseAdapter adapter for database operations
   */
  public DbLookupValidator(JdbcDatabaseAdapter databaseAdapter) {
    this.databaseAdapter = databaseAdapter;
  }

  /**
   * Validates that a value exists in the database table specified by the DbLookup configuration.
   *
   * @param value the value to validate
   * @param dbLookup the database lookup configuration
   * @return error message if validation fails, null if valid
   */
  public String validate(String value, DbLookup dbLookup) {
    if (dbLookup == null) {
      return null; // No lookup configured
    }

    if (value == null || value.isBlank()) {
      return null; // Skip lookup for null/blank values (handled by other validators)
    }

    boolean exists;
    if (dbLookup.isSingleColumn()) {
      exists = databaseAdapter.lookup(dbLookup.table(), dbLookup.column(), value);
    } else {
      // For composite lookups, this is a simplified implementation
      // In practice, you'd need to pass multiple values from the row
      // For now, we'll return an error indicating this needs row-level context
      return "Composite key lookups require row-level validation context";
    }

    if (!exists) {
      return dbLookup.getEffectiveErrorMessage();
    }

    return null; // Valid
  }

  /**
   * Validates a row of values against a database lookup configuration for composite keys.
   *
   * @param rowValues map of column names to values for the entire row
   * @param dbLookup the database lookup configuration
   * @return error message if validation fails, null if valid
   */
  public String validateComposite(Map<String, String> rowValues, DbLookup dbLookup) {
    if (dbLookup == null) {
      return null; // No lookup configured
    }

    if (dbLookup.isSingleColumn()) {
      // Use single-value validation for single columns
      String columnName = dbLookup.column();
      String value = rowValues.get(columnName);
      return validate(value, dbLookup);
    }

    // Build a map of column values for the composite key
    List<String> lookupColumns = dbLookup.columns();
    Map<String, Object> lookupValues = new java.util.HashMap<>();

    for (String column : lookupColumns) {
      String value = rowValues.get(column);
      if (value == null || value.isBlank()) {
        return null; // Skip if any composite key part is missing
      }
      lookupValues.put(column, value);
    }

    // Perform composite lookup
    boolean exists = databaseAdapter.lookupComposite(dbLookup.table(), lookupValues);

    if (!exists) {
      return dbLookup.getEffectiveErrorMessage();
    }

    return null; // Valid
  }

  /**
   * Validates a single column value with row context.
   *
   * <p>This method is used during row validation where we have access to all column values. It
   * automatically handles both single and composite key lookups.
   *
   * @param columnName the name of the column being validated
   * @param rowValues map of all column names to values in the row
   * @param dbLookup the database lookup configuration
   * @return error message if validation fails, null if valid
   */
  public String validateWithRowContext(
      String columnName, Map<String, String> rowValues, DbLookup dbLookup) {
    if (dbLookup == null) {
      return null;
    }

    // Get the value for this specific column
    String value = rowValues.get(columnName);
    if (value == null || value.isBlank()) {
      return null; // Skip lookup for null/blank values
    }

    // Use composite validation which handles both single and composite cases
    return validateComposite(rowValues, dbLookup);
  }
}
