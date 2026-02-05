package com.example.demo.core.internal.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.core.internal.domain.enums.TransformationType;
import java.util.List;
import org.junit.jupiter.api.Test;

class CellTransformerServiceTest {

  private final CellTransformerService transformer = new CellTransformerService();

  @Test
  void apply_shouldReturnNull_whenValueIsNull() {
    assertThat(transformer.apply(null, List.of(TransformationType.TRIM))).isNull();
  }

  @Test
  void apply_shouldReturnOriginal_whenTransformationsEmpty() {
    assertThat(transformer.apply(" value ", List.of())).isEqualTo(" value ");
  }

  @Test
  void apply_shouldTrim() {
    assertThat(transformer.apply(" value ", List.of(TransformationType.TRIM))).isEqualTo("value");
  }

  @Test
  void apply_shouldUppercase() {
    assertThat(transformer.apply("value", List.of(TransformationType.UPPERCASE)))
        .isEqualTo("VALUE");
  }

  @Test
  void apply_shouldLowercase() {
    assertThat(transformer.apply("VALUE", List.of(TransformationType.LOWERCASE)))
        .isEqualTo("value");
  }

  @Test
  void apply_shouldApplyMultipleTransformations() {
    assertThat(
            transformer.apply(
                " VaLuE ", List.of(TransformationType.TRIM, TransformationType.LOWERCASE)))
        .isEqualTo("value");
  }
}
