package com.example.demo.core.internal.domain.result;

/**
 * Metrics tracking validation results.
 *
 * @param totalRows Total number of rows processed
 * @param validRows Number of valid rows
 * @param invalidRows Number of invalid rows
 * @param persistedRows Number of rows successfully persisted
 */
public record ValidationMetrics(int totalRows, int validRows, int invalidRows, int persistedRows) {}
