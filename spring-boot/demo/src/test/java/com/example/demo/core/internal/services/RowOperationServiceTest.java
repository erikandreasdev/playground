package com.example.demo.core.internal.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.demo.core.internal.domain.config.Operation;
import com.example.demo.core.internal.domain.config.RowOperation;
import com.example.demo.core.internal.domain.enums.OperationType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RowOperationService}.
 *
 * <p>Tests all operation types (CONCATENATE, REPLACE, SUBSTRING, UPPERCASE, LOWERCASE, TRIM) and
 * sequential operation chains.
 */
class RowOperationServiceTest {

  private RowOperationService service;

  @BeforeEach
  void setUp() {
    service = new RowOperationService();
  }

  @Test
  void applyRowOperations_shouldHandleEmptyOperationsList() {
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Col1", "Value1");

    service.applyRowOperations(rowData, List.of());

    assertThat(rowData).hasSize(1).containsEntry("Col1", "Value1");
  }

  @Test
  void applyRowOperations_shouldHandleNullOperationsList() {
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Col1", "Value1");

    service.applyRowOperations(rowData, null);

    assertThat(rowData).hasSize(1).containsEntry("Col1", "Value1");
  }

  @Test
  void concatenate_shouldJoinTwoColumnsWithSeparator() {
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("FirstName", "John");
    rowData.put("LastName", "Doe");

    Operation op =
        new Operation(
            OperationType.CONCATENATE,
            List.of("FirstName", "LastName"),
            " ",
            null,
            null,
            null,
            null);
    RowOperation rowOp = new RowOperation("FullName", List.of(op));

    service.applyRowOperations(rowData, List.of(rowOp));

    assertThat(rowData).containsEntry("FullName", "John Doe");
  }

  @Test
  void concatenate_shouldJoinMultipleColumnsWithCustomSeparator() {
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Prefix", "CUST");
    rowData.put("Number", "001");
    rowData.put("Suffix", "A");

    Operation op =
        new Operation(
            OperationType.CONCATENATE,
            List.of("Prefix", "Number", "Suffix"),
            "-",
            null,
            null,
            null,
            null);
    RowOperation rowOp = new RowOperation("CustomerID", List.of(op));

    service.applyRowOperations(rowData, List.of(rowOp));

    assertThat(rowData).containsEntry("CustomerID", "CUST-001-A");
  }

  @Test
  void replace_shouldReplacePatternInValue() {
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Text", "Hello World Test");

    Operation concatOp =
        new Operation(OperationType.CONCATENATE, List.of("Text"), "", null, null, null, null);
    Operation replaceOp = new Operation(OperationType.REPLACE, null, null, " ", "_", null, null);
    RowOperation operation = new RowOperation("Result", List.of(concatOp, replaceOp));

    service.applyRowOperations(rowData, List.of(operation));

    assertThat(rowData).containsEntry("Result", "Hello_World_Test");
  }

  @Test
  void substring_shouldExtractSubstringWithBothIndices() {
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("FullText", "ABCDEFGH");

    Operation concat =
        new Operation(OperationType.CONCATENATE, List.of("FullText"), "", null, null, null, null);
    Operation substring = new Operation(OperationType.SUBSTRING, null, null, null, null, 2, 5);
    RowOperation rowOp = new RowOperation("Result", List.of(concat, substring));

    service.applyRowOperations(rowData, List.of(rowOp));

    assertThat(rowData).containsEntry("Result", "CDE");
  }

  @Test
  void uppercase_shouldConvertToUpperCase() {
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Text", "hello world");

    Operation concat =
        new Operation(OperationType.CONCATENATE, List.of("Text"), "", null, null, null, null);
    Operation uppercase =
        new Operation(OperationType.UPPERCASE, null, null, null, null, null, null);
    RowOperation rowOp = new RowOperation("Result", List.of(concat, uppercase));

    service.applyRowOperations(rowData, List.of(rowOp));

    assertThat(rowData).containsEntry("Result", "HELLO WORLD");
  }

  @Test
  void lowercase_shouldConvertToLowerCase() {
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Text", "HELLO WORLD");

    Operation concat =
        new Operation(OperationType.CONCATENATE, List.of("Text"), "", null, null, null, null);
    Operation lowercase =
        new Operation(OperationType.LOWERCASE, null, null, null, null, null, null);
    RowOperation rowOp = new RowOperation("Result", List.of(concat, lowercase));

    service.applyRowOperations(rowData, List.of(rowOp));

    assertThat(rowData).containsEntry("Result", "hello world");
  }

  @Test
  void trim_shouldRemoveWhitespace() {
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Text", "  spaced  ");

    Operation concat =
        new Operation(OperationType.CONCATENATE, List.of("Text"), "", null, null, null, null);
    Operation trim = new Operation(OperationType.TRIM, null, null, null, null, null, null);
    RowOperation rowOp = new RowOperation("Result", List.of(concat, trim));

    service.applyRowOperations(rowData, List.of(rowOp));

    assertThat(rowData).containsEntry("Result", "spaced");
  }

  @Test
  void sequentialOperations_shouldApplyInOrder() {
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Prefix", "CUST");
    rowData.put("Number", "001");

    Operation concat =
        new Operation(
            OperationType.CONCATENATE, List.of("Prefix", "Number"), "-", null, null, null, null);
    Operation uppercase =
        new Operation(OperationType.UPPERCASE, null, null, null, null, null, null);
    Operation replace = new Operation(OperationType.REPLACE, null, null, "-", "_", null, null);

    RowOperation rowOp = new RowOperation("CustomerID", List.of(concat, uppercase, replace));

    service.applyRowOperations(rowData, List.of(rowOp));

    assertThat(rowData).containsEntry("CustomerID", "CUST_001");
  }

  @Test
  void operation_shouldThrowExceptionForNullType() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Operation(null, null, null, null, null, null, null));
  }

  @Test
  void operation_shouldThrowExceptionForConcatenateWithoutSourceColumns() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Operation(OperationType.CONCATENATE, null, "-", null, null, null, null));
  }

  @Test
  void rowOperation_shouldThrowExceptionForNullTargetColumn() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RowOperation(
                null,
                List.of(new Operation(OperationType.TRIM, null, null, null, null, null, null))));
  }
}
