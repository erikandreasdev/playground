package com.example.demo.core.ports.outbound;

import com.example.demo.core.internal.domain.config.PersistenceConfig;
import com.example.demo.core.internal.domain.result.PersistenceResult;
import java.util.List;
import java.util.Map;

/**
 * Outbound port for database operations.
 *
 * <p>This port abstracts database interactions allowing the domain to remain independent of
 * specific database implementations (Oracle, PostgreSQL, etc.).
 */
public interface DatabasePort {

  /**
   * Persists rows to the database.
   *
   * @param rows the data to persist
   * @param config persistence settings
   * @param dryRun if true, only generates SQL without executing
   * @return result of the operation
   */
  PersistenceResult persist(List<Map<String, Object>> rows, PersistenceConfig config,
      boolean dryRun);

  /**
   * Checks if a value exists in a database table column.
   *
   * @param table the table name to query
   * @param column the column name to check
   * @param value the value to look up
   * @return true if the value exists, false otherwise
   */
  boolean lookup(String table, String column, Object value);

  /**
   * Checks if a combination of values exists in a database table (composite key lookup).
   *
   * @param table the table name to query
   * @param columnValues map of column names to values
   * @return true if the combination exists, false otherwise
   */
  boolean lookupComposite(String table, Map<String, Object> columnValues);
}
