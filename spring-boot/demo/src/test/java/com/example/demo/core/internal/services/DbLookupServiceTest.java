package com.example.demo.core.internal.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.core.internal.domain.config.DbLookup;
import com.example.demo.core.ports.outbound.DatabasePort;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for DbLookupService.
 *
 * <p>Tests the database lookup validation logic for both single-column and composite-key lookups.
 */
@ExtendWith(MockitoExtension.class)
class DbLookupServiceTest {

  @Mock private DatabasePort databasePort;

  private DbLookupService service;

  @BeforeEach
  void setUp() {
    service = new DbLookupService(databasePort);
  }

  @Test
  void validate_shouldReturnNull_whenDbLookupIsNull() {
    String result = service.validate("someValue", null);

    assertThat(result).isNull();
    verify(databasePort, never()).lookup(anyString(), anyString(), any());
  }

  @Test
  void validate_shouldReturnNull_whenValueIsNull() {
    DbLookup dbLookup = new DbLookup("users", "id", null, null);

    String result = service.validate(null, dbLookup);

    assertThat(result).isNull();
    verify(databasePort, never()).lookup(anyString(), anyString(), any());
  }

  @Test
  void validate_shouldReturnNull_whenValueIsBlank() {
    DbLookup dbLookup = new DbLookup("users", "id", null, null);

    String result = service.validate("  ", dbLookup);

    assertThat(result).isNull();
    verify(databasePort, never()).lookup(anyString(), anyString(), any());
  }

  @Test
  void validate_shouldReturnNull_whenSingleColumnLookupSucceeds() {
    DbLookup dbLookup = new DbLookup("users", "id", null, null);
    when(databasePort.lookup("users", "id", "123")).thenReturn(true);

    String result = service.validate("123", dbLookup);

    assertThat(result).isNull();
    verify(databasePort).lookup("users", "id", "123");
  }

  @Test
  void validate_shouldReturnDefaultErrorMessage_whenSingleColumnLookupFails() {
    DbLookup dbLookup = new DbLookup("users", "id", null, null);
    when(databasePort.lookup("users", "id", "999")).thenReturn(false);

    String result = service.validate("999", dbLookup);

    assertThat(result).isEqualTo("Value does not exist in users.id");
    verify(databasePort).lookup("users", "id", "999");
  }

  @Test
  void validate_shouldReturnCustomErrorMessage_whenSingleColumnLookupFailsWithCustomMessage() {
    DbLookup dbLookup = new DbLookup("users", "id", null, "User ID not found");
    when(databasePort.lookup("users", "id", "999")).thenReturn(false);

    String result = service.validate("999", dbLookup);

    assertThat(result).isEqualTo("User ID not found");
    verify(databasePort).lookup("users", "id", "999");
  }

  @Test
  void validate_shouldReturnErrorMessage_whenCompositeKeyLookupRequested() {
    DbLookup dbLookup = new DbLookup("orders", null, List.of("customer_id", "order_id"), null);

    String result = service.validate("123", dbLookup);

    assertThat(result).isEqualTo("Composite key lookups require row-level validation context");
    verify(databasePort, never()).lookup(anyString(), anyString(), any());
    verify(databasePort, never()).lookupComposite(anyString(), anyMap());
  }

  @Test
  void validateComposite_shouldReturnNull_whenDbLookupIsNull() {
    Map<String, String> rowValues = Map.of("id", "123");

    String result = service.validateComposite(rowValues, null);

    assertThat(result).isNull();
    verify(databasePort, never()).lookupComposite(anyString(), anyMap());
  }

  @Test
  void validateComposite_shouldDelegatesToValidate_whenSingleColumnLookup() {
    DbLookup dbLookup = new DbLookup("users", "id", null, null);
    Map<String, String> rowValues = Map.of("id", "123", "name", "John");
    when(databasePort.lookup("users", "id", "123")).thenReturn(true);

    String result = service.validateComposite(rowValues, dbLookup);

    assertThat(result).isNull();
    verify(databasePort).lookup("users", "id", "123");
  }

  @Test
  void validateComposite_shouldReturnNull_whenCompositeKeyLookupSucceeds() {
    DbLookup dbLookup = new DbLookup("orders", null, List.of("customer_id", "order_id"), null);
    Map<String, String> rowValues = Map.of("customer_id", "C123", "order_id", "O456");
    when(databasePort.lookupComposite(
            "orders", Map.of("customer_id", "C123", "order_id", "O456")))
        .thenReturn(true);

    String result = service.validateComposite(rowValues, dbLookup);

    assertThat(result).isNull();
    verify(databasePort)
        .lookupComposite("orders", Map.of("customer_id", "C123", "order_id", "O456"));
  }

  @Test
  void validateComposite_shouldReturnErrorMessage_whenCompositeKeyLookupFails() {
    DbLookup dbLookup =
        new DbLookup("orders", null, List.of("customer_id", "order_id"), "Order not found");
    Map<String, String> rowValues = Map.of("customer_id", "C999", "order_id", "O999");
    when(databasePort.lookupComposite(
            "orders", Map.of("customer_id", "C999", "order_id", "O999")))
        .thenReturn(false);

    String result = service.validateComposite(rowValues, dbLookup);

    assertThat(result).isEqualTo("Order not found");
    verify(databasePort)
        .lookupComposite("orders", Map.of("customer_id", "C999", "order_id", "O999"));
  }

  @Test
  void validateWithRowContext_shouldReturnNull_whenDbLookupIsNull() {
    Map<String, String> rowValues = Map.of("id", "123");

    String result = service.validateWithRowContext("id", rowValues, null);

    assertThat(result).isNull();
  }

  @Test
  void validateWithRowContext_shouldReturnNull_whenValueIsNull() {
    DbLookup dbLookup = new DbLookup("users", "id", null, null);
    Map<String, String> rowValues = Map.of("name", "John"); // id is missing

    String result = service.validateWithRowContext("id", rowValues, dbLookup);

    assertThat(result).isNull();
    verify(databasePort, never()).lookup(anyString(), anyString(), any());
  }
}
