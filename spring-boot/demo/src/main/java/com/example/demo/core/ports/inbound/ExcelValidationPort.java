package com.example.demo.core.ports.inbound;

import com.example.demo.core.internal.domain.result.ValidationReport;
import com.example.demo.core.internal.valueobjects.LoadedResource;

/**
 * Inbound port for Excel validation use case.
 *
 * <p>This port defines the contract for validating Excel files against configuration rules. It is
 * the primary entry point for the validation domain logic.
 */
public interface ExcelValidationPort {

  /**
   * Validates an Excel file against a configuration resource.
   *
   * @param excelResource the loaded Excel file resource
   * @param configResource the loaded validation configuration resource
   * @param persist whether to persist valid rows to the database
   * @return the validation report containing metrics and errors
   */
  ValidationReport validate(LoadedResource excelResource, LoadedResource configResource,
      boolean persist);
}
