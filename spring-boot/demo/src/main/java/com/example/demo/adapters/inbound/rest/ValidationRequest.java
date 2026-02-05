package com.example.demo.adapters.inbound.rest;

/**
 * Request DTO for Excel validation.
 *
 * @param excelFilename name/path of the excel file
 * @param validationsFilename name/path of the validation config file
 * @param persist whether to persist valid rows to database
 */
public record ValidationRequest(String excelFilename, String validationsFilename, Boolean persist) {
  /** Compact constructor to provide default values. */
  public ValidationRequest {
    if (persist == null) {
      persist = false;
    }
  }
}
