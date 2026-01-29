package com.example.demo.domain;

/**
 * Metrics specific to a single Excel sheet.
 *
 * @param sheetName Name of the sheet
 * @param totalRows Total rows in the sheet
 * @param validRows Valid rows in the sheet
 * @param invalidRows Invalid rows in the sheet
 * @param errors List of structural error messages specific to this sheet
 * @param persistence Result of the persistence operation (optional)
 */
public record SheetMetrics(
    String sheetName,
    int totalRows,
    int validRows,
    int invalidRows,
    java.util.List<String> errors,
    PersistenceResult persistence) {}
