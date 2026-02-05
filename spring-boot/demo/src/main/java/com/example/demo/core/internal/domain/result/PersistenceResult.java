package com.example.demo.core.internal.domain.result;

import java.util.List;

/**
 * Outcome of a persistence attempt.
 *
 * @param success Whether the persistence operation succeeded
 * @param rowsAffected Number of rows successfully persisted
 * @param errors List of database error messages
 * @param generatedSql The SQL query that was executed (or would have been)
 */
public record PersistenceResult(
    boolean success, int rowsAffected, List<String> errors, String generatedSql) {

  /** Constructor with defaults. */
  public PersistenceResult {
    if (errors == null) {
      errors = List.of();
    }
  }

  /** Factory for preview results. */
  public static PersistenceResult preview(String sql) {
    return new PersistenceResult(true, 0, List.of(), sql);
  }

  /** Factory for success results. */
  public static PersistenceResult success(int rowsAffected, String sql) {
    return new PersistenceResult(true, rowsAffected, List.of(), sql);
  }

  /** Factory for failure results. */
  public static PersistenceResult failure(List<String> errors, String sql) {
    return new PersistenceResult(false, 0, errors, sql);
  }
}
