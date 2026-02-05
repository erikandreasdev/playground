package com.example.demo.core.internal.services;

import com.example.demo.core.internal.domain.enums.TransformationType;
import java.util.List;

/** Applies transformations to cell values. Extensible design using Enum dispatch. */
public class CellTransformerService {

  /**
   * Applies a list of transformations to a value.
   *
   * @param value the original value (can be null)
   * @param transformations list of transformations to apply
   * @return the transformed value
   */
  public String apply(String value, List<TransformationType> transformations) {
    if (value == null) {
      return null;
    }
    if (transformations == null || transformations.isEmpty()) {
      return value;
    }

    String result = value;
    for (TransformationType type : transformations) {
      result = applySingle(result, type);
    }
    return result;
  }

  private String applySingle(String value, TransformationType type) {
    switch (type) {
      case TRIM:
        return value.trim();
      case UPPERCASE:
        return value.toUpperCase();
      case LOWERCASE:
        return value.toLowerCase();
      default:
        return value;
    }
  }
}
