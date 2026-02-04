package com.example.demo.service;

import com.example.demo.domain.Operation;
import com.example.demo.domain.RowOperation;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Service to execute row-level operations that compute new column values from existing columns.
 *
 * <p>This service allows creating "computed columns" by applying sequential operations on values
 * from multiple existing columns. The computed columns are added to the row data and can then be
 * validated and persisted like regular columns.
 *
 * <p>Example: Concatenate "ID Prefix" + "-" + "ID Number", convert to uppercase, and store as
 * "Customer ID".
 */
@Service
public class RowOperationProcessor {

  /**
   * Applies all row operations to compute new column values and adds them to the row data.
   *
   * <p>Row operations are applied in the order they are defined. Each operation creates a new
   * column in the row data map.
   *
   * @param rowData Current row data (map of column name to value), modified in-place
   * @param rowOperations List of row operations to apply
   */
  public void applyRowOperations(Map<String, Object> rowData, List<RowOperation> rowOperations) {
    if (rowOperations == null || rowOperations.isEmpty()) {
      return;
    }

    for (RowOperation rowOp : rowOperations) {
      String result = executeOperationChain(rowData, rowOp.operations());
      rowData.put(rowOp.targetColumn(), result);
    }
  }

  /**
   * Executes a chain of operations sequentially.
   *
   * <p>The result of each operation becomes the input to the next operation. The first operation
   * typically uses CONCATENATE to combine multiple columns, and subsequent operations transform the
   * concatenated value.
   *
   * @param rowData Current row data for accessing source column values
   * @param operations List of operations to execute sequentially
   * @return Final computed value after all operations
   */
  private String executeOperationChain(Map<String, Object> rowData, List<Operation> operations) {
    String currentValue = "";

    for (Operation op : operations) {
      currentValue = executeOperation(currentValue, op, rowData);
    }

    return currentValue;
  }

  /**
   * Executes a single operation.
   *
   * @param currentValue Current value (result from previous operation in chain)
   * @param op Operation to execute
   * @param rowData Row data for accessing source column values
   * @return Result of the operation
   */
  private String executeOperation(String currentValue, Operation op, Map<String, Object> rowData) {
    switch (op.type()) {
      case CONCATENATE:
        return concatenate(op.sourceColumns(), op.separator(), rowData);

      case REPLACE:
        return replace(currentValue, op.pattern(), op.replacement());

      case SUBSTRING:
        return substring(currentValue, op.startIndex(), op.endIndex());

      case UPPERCASE:
        return currentValue.toUpperCase();

      case LOWERCASE:
        return currentValue.toLowerCase();

      case TRIM:
        return currentValue.trim();

      default:
        return currentValue;
    }
  }

  /**
   * Concatenates values from specified columns with a separator.
   *
   * <p>Null or missing column values are treated as empty strings.
   *
   * @param columnNames List of column names to concatenate
   * @param separator Separator string to place between values
   * @param rowData Row data containing column values
   * @return Concatenated string
   */
  private String concatenate(
      List<String> columnNames, String separator, Map<String, Object> rowData) {
    return columnNames.stream()
        .map(
            colName -> {
              Object value = rowData.get(colName);
              return value != null ? value.toString() : "";
            })
        .collect(Collectors.joining(separator));
  }

  /**
   * Replaces all occurrences of a pattern with a replacement string.
   *
   * @param value Current value
   * @param pattern Pattern to find
   * @param replacement Replacement string
   * @return Value with all occurrences replaced
   */
  private String replace(String value, String pattern, String replacement) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    return value.replace(pattern, replacement);
  }

  /**
   * Extracts a substring from the current value.
   *
   * <p>Handles edge cases: - If value is null/empty, returns empty string - If startIndex is beyond
   * string length, returns empty string - If endIndex is null, extracts to end of string - If
   * endIndex is beyond string length, extracts to end of string
   *
   * @param value Current value
   * @param startIndex Start index (0-based, inclusive)
   * @param endIndex End index (0-based, exclusive), null means end of string
   * @return Extracted substring
   */
  private String substring(String value, Integer startIndex, Integer endIndex) {
    if (value == null || value.isEmpty()) {
      return "";
    }

    int length = value.length();

    // Validate and adjust start index
    if (startIndex >= length) {
      return "";
    }

    // Determine actual end index
    int actualEndIndex = (endIndex == null) ? length : Math.min(endIndex, length);

    return value.substring(startIndex, actualEndIndex);
  }
}
