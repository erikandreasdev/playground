package com.example.demo.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.domain.ColumnConfig;
import com.example.demo.domain.DataType;
import com.example.demo.domain.ValidationRule;
import java.util.List;
import org.junit.jupiter.api.Test;

class CellValueValidatorTest {

  private final DbLookupValidator dbLookupValidator = mock(DbLookupValidator.class);
  private final CellValueValidator validator = new CellValueValidator(dbLookupValidator);

  @Test
  void validate_shouldReturnTrue_whenRulesEmpty() {
    ColumnConfig config =
        new ColumnConfig("test", List.of(), List.of(), DataType.STRING, List.of(), false, null);
    assertThat(validator.validate("any", config)).isEmpty();
  }

  @Test
  void validate_shouldValidateNotNull() {
    ColumnConfig config =
        new ColumnConfig(
            "test",
            List.of(),
            List.of(ValidationRule.NOT_NULL),
            DataType.STRING,
            List.of(),
            false,
            null);
    assertThat(validator.validate("val", config)).isEmpty();
    assertThat(validator.validate(null, config)).isNotEmpty();
  }

  @Test
  void validate_shouldValidateNotEmpty() {
    ColumnConfig config =
        new ColumnConfig(
            "test",
            List.of(),
            List.of(ValidationRule.NOT_EMPTY),
            DataType.STRING,
            List.of(),
            false,
            null);
    assertThat(validator.validate("val", config)).isEmpty();
    assertThat(validator.validate("", config)).isNotEmpty();
    assertThat(validator.validate(null, config)).isNotEmpty();
    assertThat(validator.validate("   ", config)).isNotEmpty();
  }

  @Test
  void validate_shouldValidateEmail() {
    ColumnConfig config =
        new ColumnConfig(
            "test",
            List.of(),
            List.of(ValidationRule.EMAIL_FORMAT),
            DataType.STRING,
            List.of(),
            false,
            null);
    assertThat(validator.validate("test@example.com", config)).isEmpty();
    assertThat(validator.validate("invalid", config)).isNotEmpty();
    assertThat(validator.validate(null, config)).isNotEmpty();
  }

  @Test
  void validate_shouldValidateAllowedValues() {
    ColumnConfig config =
        new ColumnConfig(
            "test", List.of(), List.of(), DataType.STRING, List.of("A", "B"), false, null);
    assertThat(validator.validate("A", config)).isEmpty();
    assertThat(validator.validate("C", config)).isNotEmpty();
  }

  @Test
  void validate_shouldValidateDataTypeNumber() {
    ColumnConfig config =
        new ColumnConfig("test", List.of(), List.of(), DataType.NUMBER, List.of(), false, null);
    assertThat(validator.validate("123", config)).isEmpty();
    assertThat(validator.validate("123.45", config)).isEmpty();
    assertThat(validator.validate("abc", config)).isNotEmpty();
  }

  @Test
  void validate_shouldValidateDataTypeBoolean() {
    ColumnConfig config =
        new ColumnConfig("test", List.of(), List.of(), DataType.BOOLEAN, List.of(), false, null);
    assertThat(validator.validate("true", config)).isEmpty();
    assertThat(validator.validate("0", config)).isEmpty();
    assertThat(validator.validate("yes", config)).isEmpty();
    assertThat(validator.validate("abc", config)).isNotEmpty();
  }

  @Test
  void validate_shouldValidateDataTypeUuid() {
    ColumnConfig config =
        new ColumnConfig("test", List.of(), List.of(), DataType.UUID, List.of(), false, null);
    assertThat(validator.validate("123e4567-e89b-12d3-a456-426614174000", config)).isEmpty();
    assertThat(validator.validate("short-id", config)).isNotEmpty();
  }

  @Test
  void validateWithRowContext_shouldDelegateToDbLookup() {
    com.example.demo.domain.DbLookup dbLookup =
        new com.example.demo.domain.DbLookup("users", "id", null, null);
    ColumnConfig config =
        new ColumnConfig("id", List.of(), List.of(), DataType.STRING, List.of(), false, dbLookup);
    java.util.Map<String, String> rowValues = java.util.Map.of("id", "123");

    when(dbLookupValidator.validateWithRowContext("id", rowValues, dbLookup))
        .thenReturn("DB Error");

    assertThat(validator.validateWithRowContext("id", "123", rowValues, config))
        .contains("DB Error");
  }

  @Test
  void validateWithRowContext_shouldValidateWithoutDbLookup() {
    ColumnConfig config =
        new ColumnConfig("test", List.of(), List.of(), DataType.STRING, List.of(), false, null);
    java.util.Map<String, String> rowValues = java.util.Map.of("test", "value");

    assertThat(validator.validateWithRowContext("test", "value", rowValues, config)).isEmpty();
  }
}
