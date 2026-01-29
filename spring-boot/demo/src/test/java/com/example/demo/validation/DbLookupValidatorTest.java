package com.example.demo.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.domain.DbLookup;
import com.example.demo.service.JdbcDatabaseAdapter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for DbLookupValidator.
 *
 * <p>Tests the database lookup validation logic for both single-column and composite-key lookups.
 */
@ExtendWith(MockitoExtension.class)
class DbLookupValidatorTest {

  @Mock private JdbcDatabaseAdapter databaseAdapter;

  private DbLookupValidator validator;

  @BeforeEach
  void setUp() {
    validator = new DbLookupValidator(databaseAdapter);
  }

  @Test
  void validate_shouldReturnNull_whenDbLookupIsNull() {
    String result = validator.validate("someValue", null);

    assertThat(result).isNull();
    verify(databaseAdapter, never()).lookup(anyString(), anyString(), any());
  }

  @Test
  void validate_shouldReturnNull_whenValueIsNull() {
    DbLookup dbLookup = new DbLookup("users", "id", null, null);

    String result = validator.validate(null, dbLookup);

    assertThat(result).isNull();
    verify(databaseAdapter, never()).lookup(anyString(), anyString(), any());
  }

  @Test
  void validate_shouldReturnNull_whenValueIsBlank() {
    DbLookup dbLookup = new DbLookup("users", "id", null, null);

    String result = validator.validate("  ", dbLookup);

    assertThat(result).isNull();
    verify(databaseAdapter, never()).lookup(anyString(), anyString(), any());
  }

  @Test
  void validate_shouldReturnNull_whenSingleColumnLookupSucceeds() {
    DbLookup dbLookup = new DbLookup("users", "id", null, null);
    when(databaseAdapter.lookup("users", "id", "123")).thenReturn(true);

    String result = validator.validate("123", dbLookup);

    assertThat(result).isNull();
    verify(databaseAdapter).lookup("users", "id", "123");
  }

  @Test
  void validate_shouldReturnDefaultErrorMessage_whenSingleColumnLookupFails() {
    DbLookup dbLookup = new DbLookup("users", "id", null, null);
    when(databaseAdapter.lookup("users", "id", "999")).thenReturn(false);

    String result = validator.validate("999", dbLookup);

    assertThat(result).isEqualTo("Value does not exist in users.id");
    verify(databaseAdapter).lookup("users", "id", "999");
  }

  @Test
  void validate_shouldReturnCustomErrorMessage_whenSingleColumnLookupFailsWithCustomMessage() {
    DbLookup dbLookup = new DbLookup("users", "id", null, "User ID not found");
    when(databaseAdapter.lookup("users", "id", "999")).thenReturn(false);

    String result = validator.validate("999", dbLookup);

    assertThat(result).isEqualTo("User ID not found");
    verify(databaseAdapter).lookup("users", "id", "999");
  }

  @Test
  void validate_shouldReturnErrorMessage_whenCompositeKeyLookupRequested() {
    DbLookup dbLookup = new DbLookup("orders", null, List.of("customer_id", "order_id"), null);

    String result = validator.validate("123", dbLookup);

    assertThat(result).isEqualTo("Composite key lookups require row-level validation context");
    verify(databaseAdapter, never()).lookup(anyString(), anyString(), any());
    verify(databaseAdapter, never()).lookupComposite(anyString(), anyMap());
  }

  @Test
  void validateComposite_shouldReturnNull_whenDbLookupIsNull() {
    Map<String, String> rowValues = Map.of("id", "123");

    String result = validator.validateComposite(rowValues, null);

    assertThat(result).isNull();
    verify(databaseAdapter, never()).lookupComposite(anyString(), anyMap());
  }

  @Test
  void validateComposite_shouldDelegatesToValidate_whenSingleColumnLookup() {
    DbLookup dbLookup = new DbLookup("users", "id", null, null);
    Map<String, String> rowValues = Map.of("id", "123", "name", "John");
    when(databaseAdapter.lookup("users", "id", "123")).thenReturn(true);

    String result = validator.validateComposite(rowValues, dbLookup);

    assertThat(result).isNull();
    verify(databaseAdapter).lookup("users", "id", "123");
  }

  @Test
  void validateComposite_shouldReturnNull_whenCompositeKeyLookupSucceeds() {
    DbLookup dbLookup = new DbLookup("orders", null, List.of("customer_id", "order_id"), null);
    Map<String, String> rowValues = Map.of("customer_id", "C123", "order_id", "O456");
    when(databaseAdapter.lookupComposite(
            "orders", Map.of("customer_id", "C123", "order_id", "O456")))
        .thenReturn(true);

    String result = validator.validateComposite(rowValues, dbLookup);

    assertThat(result).isNull();
    verify(databaseAdapter)
        .lookupComposite("orders", Map.of("customer_id", "C123", "order_id", "O456"));
  }

  @Test
  void validateComposite_shouldReturnErrorMessage_whenCompositeKeyLookupFails() {
    DbLookup dbLookup =
        new DbLookup("orders", null, List.of("customer_id", "order_id"), "Order not found");
    Map<String, String> rowValues = Map.of("customer_id", "C999", "order_id", "O999");
    when(databaseAdapter.lookupComposite(
            "orders", Map.of("customer_id", "C999", "order_id", "O999")))
        .thenReturn(false);

    String result = validator.validateComposite(rowValues, dbLookup);

    assertThat(result).isEqualTo("Order not found");
    verify(databaseAdapter)
        .lookupComposite("orders", Map.of("customer_id", "C999", "order_id", "O999"));
  }

  @Test
  void validateComposite_shouldReturnNull_whenCompositeKeyValueIsMissing() {
    DbLookup dbLookup = new DbLookup("orders", null, List.of("customer_id", "order_id"), null);
    Map<String, String> rowValues = Map.of("customer_id", "C123"); // order_id missing

    String result = validator.validateComposite(rowValues, dbLookup);

    assertThat(result).isNull();
    verify(databaseAdapter, never()).lookupComposite(anyString(), anyMap());
  }

  @Test
  void validateComposite_shouldReturnNull_whenCompositeKeyValueIsBlank() {
    DbLookup dbLookup = new DbLookup("orders", null, List.of("customer_id", "order_id"), null);
    Map<String, String> rowValues = Map.of("customer_id", "C123", "order_id", "  ");

    String result = validator.validateComposite(rowValues, dbLookup);

    assertThat(result).isNull();
    verify(databaseAdapter, never()).lookupComposite(anyString(), anyMap());
  }

  @Test
  void validateWithRowContext_shouldReturnNull_whenDbLookupIsNull() {
    Map<String, String> rowValues = Map.of("id", "123");

    String result = validator.validateWithRowContext("id", rowValues, null);

    assertThat(result).isNull();
  }

  @Test
  void validateWithRowContext_shouldReturnNull_whenValueIsNull() {
    DbLookup dbLookup = new DbLookup("users", "id", null, null);
    Map<String, String> rowValues = Map.of("name", "John"); // id is missing

    String result = validator.validateWithRowContext("id", rowValues, dbLookup);

    assertThat(result).isNull();
    verify(databaseAdapter, never()).lookup(anyString(), anyString(), any());
  }

  @Test
  void validateWithRowContext_shouldReturnNull_whenValueIsBlank() {
    DbLookup dbLookup = new DbLookup("users", "id", null, null);
    Map<String, String> rowValues = Map.of("id", "  ", "name", "John");

    String result = validator.validateWithRowContext("id", rowValues, dbLookup);

    assertThat(result).isNull();
    verify(databaseAdapter, never()).lookup(anyString(), anyString(), any());
  }

  @Test
  void validateWithRowContext_shouldDelegateToValidateComposite() {
    DbLookup dbLookup = new DbLookup("users", "id", null, null);
    Map<String, String> rowValues = Map.of("id", "123", "name", "John");
    when(databaseAdapter.lookup("users", "id", "123")).thenReturn(true);

    String result = validator.validateWithRowContext("id", rowValues, dbLookup);

    assertThat(result).isNull();
    verify(databaseAdapter).lookup("users", "id", "123");
  }

  @Test
  void validateWithRowContext_shouldHandleCompositeKeys() {
    DbLookup dbLookup = new DbLookup("orders", null, List.of("customer_id", "order_id"), null);
    Map<String, String> rowValues = Map.of("customer_id", "C123", "order_id", "O456");
    when(databaseAdapter.lookupComposite(
            "orders", Map.of("customer_id", "C123", "order_id", "O456")))
        .thenReturn(true);

    String result = validator.validateWithRowContext("customer_id", rowValues, dbLookup);

    assertThat(result).isNull();
    verify(databaseAdapter)
        .lookupComposite("orders", Map.of("customer_id", "C123", "order_id", "O456"));
  }
}
