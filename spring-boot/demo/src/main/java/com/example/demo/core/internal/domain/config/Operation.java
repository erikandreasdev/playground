package com.example.demo.core.internal.domain.config;

import com.example.demo.core.internal.domain.enums.OperationType;
import java.util.List;

/**
 * Represents a single operation in a row-level transformation chain.
 *
 * <p>Each operation type requires specific fields to be non-null:
 *
 * <ul>
 *   <li>CONCATENATE: sourceColumns (required), separator (optional, defaults to empty string)
 *   <li>REPLACE: pattern (required), replacement (required)
 *   <li>SUBSTRING: startIndex (required), endIndex (optional, defaults to end of string)
 *   <li>UPPERCASE, LOWERCASE, TRIM: no additional fields required
 * </ul>
 *
 * @param type Type of operation to perform
 * @param sourceColumns List of column names to read values from (for CONCATENATE)
 * @param separator Separator string for CONCATENATE operation
 * @param pattern Pattern to find for REPLACE operation
 * @param replacement Replacement string for REPLACE operation
 * @param startIndex Start index for SUBSTRING operation (0-based, inclusive)
 * @param endIndex End index for SUBSTRING operation (0-based, exclusive)
 */
public record Operation(
    OperationType type,
    List<String> sourceColumns,
    String separator,
    String pattern,
    String replacement,
    Integer startIndex,
    Integer endIndex) {

  /**
   * Compact constructor with validation for required fields based on operation type.
   *
   * @throws IllegalArgumentException if required fields are missing for the operation type
   */
  public Operation {
    if (type == null) {
      throw new IllegalArgumentException("Operation type cannot be null");
    }

    // Validate required fields based on operation type
    switch (type) {
      case CONCATENATE:
        if (sourceColumns == null || sourceColumns.isEmpty()) {
          throw new IllegalArgumentException(
              "CONCATENATE operation requires non-empty sourceColumns");
        }
        // Default separator to empty string if null
        if (separator == null) {
          separator = "";
        }
        break;

      case REPLACE:
        if (pattern == null) {
          throw new IllegalArgumentException("REPLACE operation requires pattern");
        }
        if (replacement == null) {
          throw new IllegalArgumentException("REPLACE operation requires replacement");
        }
        break;

      case SUBSTRING:
        if (startIndex == null) {
          throw new IllegalArgumentException("SUBSTRING operation requires startIndex");
        }
        if (startIndex < 0) {
          throw new IllegalArgumentException("startIndex must be non-negative");
        }
        if (endIndex != null && endIndex < startIndex) {
          throw new IllegalArgumentException(
              "endIndex must be greater than or equal to startIndex");
        }
        break;

      case UPPERCASE:
      case LOWERCASE:
      case TRIM:
        // No additional fields required
        break;

      default:
        throw new IllegalArgumentException("Unknown operation type: " + type);
    }
  }
}
