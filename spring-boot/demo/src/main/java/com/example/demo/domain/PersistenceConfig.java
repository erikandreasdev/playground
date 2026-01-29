package com.example.demo.domain;

import java.util.List;

/**
 * Configuration for database persistence of Excel data.
 *
 * @param tableName Name of the target database table
 * @param upsert Whether to use upsert (merge) instead of simple insert
 * @param primaryKey The column name to use as primary key for upserts
 * @param mappings List of column mappings
 */
public record PersistenceConfig(
    String tableName, boolean upsert, String primaryKey, List<DatabaseMapping> mappings) {

  /** Constructor with defaults. */
  public PersistenceConfig {
    if (mappings == null) {
      mappings = List.of();
    }
  }
}
