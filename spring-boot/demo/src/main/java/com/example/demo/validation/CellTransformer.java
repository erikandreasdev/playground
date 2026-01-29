package com.example.demo.validation;

import com.example.demo.domain.TransformationType;
import java.util.List;
import org.springframework.stereotype.Component;

/** Applies transformations to cell values. Extensible design using Enum dispatch. */
@Component
public class CellTransformer {

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
