package com.example.demo.core.internal.domain.result;

import com.example.demo.core.internal.domain.enums.ValidationStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * Report containing the results of an Excel validation.
 *
 * @param status Overall validation status
 * @param excelMetadata Metadata about the validated Excel file
 * @param rulesMetadata Metadata about the validation rules file
 * @param globalMetrics Aggregated metrics across all sheets
 * @param sheetMetrics Granular metrics per sheet
 * @param errors List of structural error messages (e.g., missing sheets)
 */
public record ValidationReport(
    ValidationStatus status,
    FileMetadata excelMetadata,
    FileMetadata rulesMetadata,
    ValidationMetrics globalMetrics,
    List<SheetMetrics> sheetMetrics,
    List<String> errors) {

  /** Constructor with defaults. */
  public ValidationReport {
    if (sheetMetrics == null) {
      sheetMetrics = List.of();
    }
    if (errors == null) {
      errors = new ArrayList<>();
    }
  }
}
