package com.example.demo.validation;

import com.example.demo.domain.ColumnConfig;
import com.example.demo.domain.DataType;
import com.example.demo.domain.ValidationRule;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Validates cell values against rules. Extensible design using Enum dispatch. */
@Component
public class CellValueValidator {

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

  private final DbLookupValidator dbLookupValidator;

  /**
   * Constructs the validator.
   *
   * @param dbLookupValidator validator for database lookups
   */
  public CellValueValidator(DbLookupValidator dbLookupValidator) {
    this.dbLookupValidator = dbLookupValidator;
  }

  /**
   * Validates a value against a column configuration and returns a list of error messages.
   *
   * @param value the transformed value to validate
   * @param config the column configuration
   * @return list of error messages, empty if valid
   */
  public List<String> validate(String value, ColumnConfig config) {
    List<String> errors = new java.util.ArrayList<>();

    // 1. Check rules
    validateRules(value, config.rules(), errors);

    // 2. Check type
    validateType(value, config.type(), errors);

    // 3. Check allowed values
    validateAllowedValues(value, config.allowedValues(), errors);

    return errors;
  }

  /**
   * Validates a value with row context for database lookups.
   *
   * @param columnName the name of the column being validated
   * @param value the transformed value to validate
   * @param rowValues all values in the row (for composite lookups)
   * @param config the column configuration
   * @return list of error messages, empty if valid
   */
  public List<String> validateWithRowContext(
      String columnName, String value, Map<String, String> rowValues, ColumnConfig config) {
    List<String> errors = validate(value, config);

    // 4. Check database lookup if configured
    if (config.dbLookup() != null) {
      String dbError =
          dbLookupValidator.validateWithRowContext(columnName, rowValues, config.dbLookup());
      if (dbError != null) {
        errors.add(dbError);
      }
    }

    return errors;
  }

  private void validateRules(String value, List<ValidationRule> rules, List<String> errors) {
    if (rules == null || rules.isEmpty()) {
      return;
    }

    for (ValidationRule rule : rules) {
      if (!validateSingle(value, rule)) {
        errors.add("Rule violated: " + rule.name());
      }
    }
  }

  private void validateType(String value, DataType type, List<String> errors) {
    if (value == null || value.isEmpty()) {
      return; // Use NOT_EMPTY rule to catch empty values
    }

    try {
      switch (type) {
        case NUMBER:
          Double.parseDouble(value);
          break;
        case DECIMAL:
          new java.math.BigDecimal(value);
          break;
        case BOOLEAN:
          String val = value.trim().toLowerCase();
          boolean isValidBool =
              val.equals("true")
                  || val.equals("false")
                  || val.equals("1")
                  || val.equals("0")
                  || val.equals("yes")
                  || val.equals("no");
          if (!isValidBool) {
            errors.add("Invalid boolean format: " + value);
          }
          break;
        case UUID:
          UUID.fromString(value);
          break;
        case DATE:
          if (!value.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            errors.add("Invalid date format: " + value);
          }
          break;
        case EMAIL:
          if (!EMAIL_PATTERN.matcher(value).matches()) {
            errors.add("Invalid email format: " + value);
          }
          break;
        case STRING:
        default:
          break;
      }
    } catch (Exception e) {
      errors.add("Data type mismatch: expected " + type.name() + " but got '" + value + "'");
    }
  }

  private void validateAllowedValues(
      String value, List<String> allowedValues, List<String> errors) {
    if (allowedValues == null || allowedValues.isEmpty()) {
      return;
    }
    if (!allowedValues.contains(value)) {
      errors.add("Value '" + value + "' is not in allowed list: " + allowedValues);
    }
  }

  private boolean validateSingle(String value, ValidationRule rule) {
    switch (rule) {
      case NOT_NULL:
        return value != null;
      case NOT_EMPTY:
        return value != null && !value.trim().isEmpty();
      case EMAIL_FORMAT:
        return value != null && EMAIL_PATTERN.matcher(value).matches();
      default:
        return true;
    }
  }
}
