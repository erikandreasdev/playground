package com.example.demo.service;

import com.example.demo.domain.DatabaseMapping;
import com.example.demo.domain.PersistenceConfig;
import com.example.demo.domain.PersistenceResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

/** Adapter for database operations using JDBC. */
@Repository
public class JdbcDatabaseAdapter {

  private static final Logger log = LoggerFactory.getLogger(JdbcDatabaseAdapter.class);

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public JdbcDatabaseAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Persists rows to the database or generates a preview.
   *
   * @param rows The data to persist
   * @param config Persistence settings
   * @param dryRun If true, only generates SQL without executing
   * @return Result of the operation
   */
  public PersistenceResult persist(
      List<Map<String, Object>> rows, PersistenceConfig config, boolean dryRun) {
    if (rows.isEmpty()) {
      return PersistenceResult.success(0, "");
    }

    String sql = buildSql(config);

    if (dryRun) {
      return PersistenceResult.preview(sql);
    }

    try {
      SqlParameterSource[] batch =
          rows.stream().map(MapSqlParameterSource::new).toArray(SqlParameterSource[]::new);

      int[] updateCounts = jdbcTemplate.batchUpdate(sql, batch);
      int rowsAffected = 0;
      for (int count : updateCounts) {
        if (count > 0) {
          rowsAffected += count;
        }
      }

      return PersistenceResult.success(rowsAffected, sql);
    } catch (DataAccessException e) {
      log.error("Failed to execute batch update", e);
      return PersistenceResult.failure(List.of(e.getMessage()), sql);
    }
  }

  /**
   * Checks if a value exists in a database table column.
   *
   * @param table the table name to query
   * @param column the column name to check
   * @param value the value to look up
   * @return true if the value exists, false otherwise
   */
  public boolean lookup(String table, String column, Object value) {
    if (value == null) {
      return false;
    }

    String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s = :value", table, column);

    try {
      MapSqlParameterSource params = new MapSqlParameterSource("value", value);
      Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
      return count != null && count > 0;
    } catch (DataAccessException e) {
      log.error("Failed to perform database lookup for {}={} in {}", column, value, table, e);
      return false;
    }
  }

  /**
   * Checks if a combination of values exists in a database table (composite key lookup).
   *
   * @param table the table name to query
   * @param columnValues map of column names to values
   * @return true if the combination exists, false otherwise
   */
  public boolean lookupComposite(String table, Map<String, Object> columnValues) {
    if (columnValues == null || columnValues.isEmpty()) {
      return false;
    }

    // Build WHERE clause: col1 = :col1 AND col2 = :col2 ...
    String whereClause =
        columnValues.keySet().stream()
            .map(col -> col + " = :" + col)
            .collect(Collectors.joining(" AND "));

    String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s", table, whereClause);

    try {
      MapSqlParameterSource params = new MapSqlParameterSource(columnValues);
      Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
      return count != null && count > 0;
    } catch (DataAccessException e) {
      log.error("Failed to perform composite database lookup in {}", table, e);
      return false;
    }
  }

  private String buildSql(PersistenceConfig config) {
    List<String> columns = config.mappings().stream().map(DatabaseMapping::dbColumn).toList();

    String placeholders = columns.stream().map(c -> ":" + c).collect(Collectors.joining(", "));

    String columnList = String.join(", ", columns);

    if (config.upsert() && config.primaryKey() != null && !config.primaryKey().isBlank()) {
      // Resolve primary key to DB column name if it matches an Excel column
      String dbPrimaryKey =
          config.mappings().stream()
              .filter(m -> m.excelColumn().equals(config.primaryKey()))
              .map(DatabaseMapping::dbColumn)
              .findFirst()
              .orElse(config.primaryKey());

      return buildUpsertSql(config.tableName(), columns, columnList, placeholders, dbPrimaryKey);
    }

    return String.format(
        "INSERT INTO %s (%s) VALUES (%s)", config.tableName(), columnList, placeholders);
  }

  private String buildUpsertSql(
      String table,
      List<String> columns,
      String columnList,
      String placeholders,
      String primaryKey) {
    // Basic Oracle style MERGE
    String updateList =
        columns.stream()
            .filter(c -> !c.equalsIgnoreCase(primaryKey))
            .map(c -> "target." + c + " = source." + c)
            .collect(Collectors.joining(", "));

    String valuesList = columns.stream().map(c -> "source." + c).collect(Collectors.joining(", "));

    return String.format(
        "MERGE INTO %s target "
            + "USING (SELECT %s FROM DUAL) source "
            + "ON (target.%s = source.%s) "
            + "WHEN MATCHED THEN UPDATE SET %s "
            + "WHEN NOT MATCHED THEN INSERT (%s) VALUES (%s)",
        table,
        columns.stream().map(c -> ":" + c + " AS " + c).collect(Collectors.joining(", ")),
        primaryKey,
        primaryKey,
        updateList,
        columnList,
        valuesList);
  }
}
