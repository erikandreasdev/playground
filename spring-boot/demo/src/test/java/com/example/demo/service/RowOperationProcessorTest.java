package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.demo.domain.Operation;
import com.example.demo.domain.OperationType;
import com.example.demo.domain.RowOperation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RowOperationProcessor}.
 *
 * <p>Tests all operation types (CONCATENATE, REPLACE, SUBSTRING, UPPERCASE, LOWERCASE, TRIM) and
 * sequential operation chains.
 */
class RowOperationProcessorTest {

  private RowOperationProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new RowOperationProcessor();
  }

  @Test
  void applyRowOperations_shouldHandleEmptyOperationsList() {
    // Arrange
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Col1", "Value1");

    // Act
    processor.applyRowOperations(rowData, List.of());

    // Assert
    assertThat(rowData).hasSize(1).containsEntry("Col1", "Value1");
  }

  @Test
  void applyRowOperations_shouldHandleNullOperationsList() {
    // Arrange
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Col1", "Value1");

    // Act
    processor.applyRowOperations(rowData, null);

    // Assert
    assertThat(rowData).hasSize(1).containsEntry("Col1", "Value1");
  }

  @Test
  void concatenate_shouldJoinTwoColumnsWithSeparator() {
    // Arrange
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

    // Act
    processor.applyRowOperations(rowData, List.of(rowOp));

    // Assert
    assertThat(rowData).containsEntry("FullName", "John Doe");
  }

  @Test
  void concatenate_shouldJoinMultipleColumnsWithCustomSeparator() {
    // Arrange
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

    // Act
    processor.applyRowOperations(rowData, List.of(rowOp));

    // Assert
    assertThat(rowData).containsEntry("CustomerID", "CUST-001-A");
  }

  @Test
  void concatenate_shouldHandleNullValues() {
    // Arrange
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Col1", "A");
    rowData.put("Col2", null);
    rowData.put("Col3", "C");

    Operation op =
        new Operation(
            OperationType.CONCATENATE,
            List.of("Col1", "Col2", "Col3"),
            "-",
            null,
            null,
            null,
            null);
    RowOperation rowOp = new RowOperation("Result", List.of(op));

    // Act
    processor.applyRowOperations(rowData, List.of(rowOp));

    // Assert
    assertThat(rowData).containsEntry("Result", "A--C"); // null treated as empty string
  }

  @Test
  void concatenate_shouldHandleMissingColumns() {
    // Arrange
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Col1", "A");
    // Col2 missing entirely

    Operation op =
        new Operation(
            OperationType.CONCATENATE, List.of("Col1", "Col2"), "-", null, null, null, null);
    RowOperation rowOp = new RowOperation("Result", List.of(op));

    // Act
    processor.applyRowOperations(rowData, List.of(rowOp));

    // Assert
    assertThat(rowData).containsEntry("Result", "A-"); // missing column treated as empty string
  }

  @Test
  void replace_shouldReplacePatternInValue() {
    // Arrange
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Text", "Hello World Test");

    Operation concatOp =
        new Operation(OperationType.CONCATENATE, List.of("Text"), "", null, null, null, null);
    Operation replaceOp = new Operation(OperationType.REPLACE, null, null, " ", "_", null, null);
    RowOperation operation = new RowOperation("Result", List.of(concatOp, replaceOp));

    // Act
    processor.applyRowOperations(rowData, List.of(operation));

    // Assert
    assertThat(rowData).containsEntry("Result", "Hello_World_Test");
  }

  @Test
  void substring_shouldExtractSubstringWithBothIndices() {
    // Arrange
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("FullText", "ABCDEFGH");

    Operation concat =
        new Operation(OperationType.CONCATENATE, List.of("FullText"), "", null, null, null, null);
    Operation substring = new Operation(OperationType.SUBSTRING, null, null, null, null, 2, 5);
    RowOperation rowOp = new RowOperation("Result", List.of(concat, substring));

    // Act
    processor.applyRowOperations(rowData, List.of(rowOp));

    // Assert
    assertThat(rowData).containsEntry("Result", "CDE"); // indices 2-5 (exclusive)
  }

  @Test
  void substring_shouldExtractToEndWhenEndIndexIsNull() {
    // Arrange
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("FullText", "ABCDEFGH");

    Operation concat =
        new Operation(OperationType.CONCATENATE, List.of("FullText"), "", null, null, null, null);
    Operation substring = new Operation(OperationType.SUBSTRING, null, null, null, null, 3, null);
    RowOperation rowOp = new RowOperation("Result", List.of(concat, substring));

    // Act
    processor.applyRowOperations(rowData, List.of(rowOp));

    // Assert
    assertThat(rowData).containsEntry("Result", "DEFGH"); // from index 3 to end
  }

  @Test
  void substring_shouldReturnEmptyWhenStartIndexBeyondLength() {
    // Arrange
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Text", "ABC");

    Operation concat =
        new Operation(OperationType.CONCATENATE, List.of("Text"), "", null, null, null, null);
    Operation substring = new Operation(OperationType.SUBSTRING, null, null, null, null, 10, null);
    RowOperation rowOp = new RowOperation("Result", List.of(concat, substring));

    // Act
    processor.applyRowOperations(rowData, List.of(rowOp));

    // Assert
    assertThat(rowData).containsEntry("Result", "");
  }

  @Test
  void uppercase_shouldConvertToUpperCase() {
    // Arrange
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Text", "hello world");

    Operation concat =
        new Operation(OperationType.CONCATENATE, List.of("Text"), "", null, null, null, null);
    Operation uppercase =
        new Operation(OperationType.UPPERCASE, null, null, null, null, null, null);
    RowOperation rowOp = new RowOperation("Result", List.of(concat, uppercase));

    // Act
    processor.applyRowOperations(rowData, List.of(rowOp));

    // Assert
    assertThat(rowData).containsEntry("Result", "HELLO WORLD");
  }

  @Test
  void lowercase_shouldConvertToLowerCase() {
    // Arrange
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Text", "HELLO WORLD");

    Operation concat =
        new Operation(OperationType.CONCATENATE, List.of("Text"), "", null, null, null, null);
    Operation lowercase =
        new Operation(OperationType.LOWERCASE, null, null, null, null, null, null);
    RowOperation rowOp = new RowOperation("Result", List.of(concat, lowercase));

    // Act
    processor.applyRowOperations(rowData, List.of(rowOp));

    // Assert
    assertThat(rowData).containsEntry("Result", "hello world");
  }

  @Test
  void trim_shouldRemoveWhitespace() {
    // Arrange
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Text", "  spaced  ");

    Operation concat =
        new Operation(OperationType.CONCATENATE, List.of("Text"), "", null, null, null, null);
    Operation trim = new Operation(OperationType.TRIM, null, null, null, null, null, null);
    RowOperation rowOp = new RowOperation("Result", List.of(concat, trim));

    // Act
    processor.applyRowOperations(rowData, List.of(rowOp));

    // Assert
    assertThat(rowData).containsEntry("Result", "spaced");
  }

  @Test
  void sequentialOperations_shouldApplyInOrder() {
    // Arrange
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("Prefix", "CUST");
    rowData.put("Number", "001");

    // Step 1: CONCATENATE Prefix + "-" + Number → "CUST-001"
    // Step 2: UPPERCASE → "CUST-001" (already uppercase)
    // Step 3: REPLACE "-" with "_" → "CUST_001"
    Operation concat =
        new Operation(
            OperationType.CONCATENATE, List.of("Prefix", "Number"), "-", null, null, null, null);
    Operation uppercase =
        new Operation(OperationType.UPPERCASE, null, null, null, null, null, null);
    Operation replace = new Operation(OperationType.REPLACE, null, null, "-", "_", null, null);

    RowOperation rowOp = new RowOperation("CustomerID", List.of(concat, uppercase, replace));

    // Act
    processor.applyRowOperations(rowData, List.of(rowOp));

    // Assert
    assertThat(rowData).containsEntry("CustomerID", "CUST_001");
  }

  @Test
  void sequentialOperations_shouldHandleComplexChain() {
    // Arrange
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("First", "john");
    rowData.put("Last", "doe");

    // Step 1: CONCATENATE First + " " + Last → "john doe"
    // Step 2: UPPERCASE → "JOHN DOE"
    // Step 3: REPLACE " " with "-" → "JOHN-DOE"
    // Step 4: SUBSTRING 0-8 → "JOHN-DOE" (full length is 8, so takes all)
    Operation concat =
        new Operation(
            OperationType.CONCATENATE, List.of("First", "Last"), " ", null, null, null, null);
    Operation uppercase =
        new Operation(OperationType.UPPERCASE, null, null, null, null, null, null);
    Operation replace = new Operation(OperationType.REPLACE, null, null, " ", "-", null, null);
    Operation substring = new Operation(OperationType.SUBSTRING, null, null, null, null, 0, 8);

    RowOperation rowOp = new RowOperation("Name", List.of(concat, uppercase, replace, substring));

    // Act
    processor.applyRowOperations(rowData, List.of(rowOp));

    // Assert
    assertThat(rowData).containsEntry("Name", "JOHN-DOE");
  }

  @Test
  void multipleRowOperations_shouldCreateMultipleComputedColumns() {
    // Arrange
    Map<String, Object> rowData = new HashMap<>();
    rowData.put("First", "John");
    rowData.put("Last", "Doe");
    rowData.put("ID", "123");

    // RowOp 1: Create "FullName" = First + " " + Last
    Operation concat1 =
        new Operation(
            OperationType.CONCATENATE, List.of("First", "Last"), " ", null, null, null, null);
    RowOperation rowOp1 = new RowOperation("FullName", List.of(concat1));

    // RowOp 2: Create "UpperID" = UPPERCASE(ID)
    Operation concat2 =
        new Operation(OperationType.CONCATENATE, List.of("ID"), "", null, null, null, null);
    Operation upper = new Operation(OperationType.UPPERCASE, null, null, null, null, null, null);
    RowOperation rowOp2 = new RowOperation("UpperID", List.of(concat2, upper));

    // Act
    processor.applyRowOperations(rowData, List.of(rowOp1, rowOp2));

    // Assert
    assertThat(rowData)
        .containsEntry("FullName", "John Doe")
        .containsEntry("UpperID", "123") // Already uppercase
        .hasSize(5); // First, Last, ID, FullName, UpperID
  }

  @Test
  void operation_shouldThrowExceptionForNullType() {
    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> new Operation(null, null, null, null, null, null, null));
  }

  @Test
  void operation_shouldThrowExceptionForConcatenateWithoutSourceColumns() {
    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> new Operation(OperationType.CONCATENATE, null, "-", null, null, null, null));
  }

  @Test
  void operation_shouldThrowExceptionForReplaceWithoutPattern() {
    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> new Operation(OperationType.REPLACE, null, null, null, "replacement", null, null));
  }

  @Test
  void operation_shouldThrowExceptionForSubstringWithoutStartIndex() {
    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> new Operation(OperationType.SUBSTRING, null, null, null, null, null, null));
  }

  @Test
  void operation_shouldDefaultSeparatorToEmptyString() {
    // Arrange & Act
    Operation op =
        new Operation(
            OperationType.CONCATENATE, List.of("Col1", "Col2"), null, null, null, null, null);

    // Assert
    assertThat(op.separator()).isEqualTo("");
  }

  @Test
  void rowOperation_shouldThrowExceptionForNullTargetColumn() {
    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RowOperation(
                null,
                List.of(new Operation(OperationType.TRIM, null, null, null, null, null, null))));
  }

  @Test
  void rowOperation_shouldThrowExceptionForEmptyOperations() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> new RowOperation("Target", List.of()));
  }
}
