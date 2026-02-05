package com.example.demo.core.internal.services;

import com.example.demo.core.internal.domain.config.DatabaseMapping;
import com.example.demo.core.internal.domain.config.PersistenceConfig;
import com.example.demo.core.internal.domain.result.PersistenceResult;
import com.example.demo.core.ports.outbound.DatabasePort;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Service to orchestrate the persistence of validated Excel data. */
public class ExcelPersistenceService {

  private final DatabasePort databasePort;

  public ExcelPersistenceService(DatabasePort databasePort) {
    this.databasePort = databasePort;
  }

  /**
   * Persists valid rows to the database.
   *
   * @param validRows The data to persist
   * @param config Persistence settings
   * @param dryRun If true, only generates SQL preview
   * @return Result of the persistence operation
   */
  public PersistenceResult persistValidRows(
      List<Map<String, Object>> validRows, PersistenceConfig config, boolean dryRun) {

    if (config == null || validRows.isEmpty()) {
      return PersistenceResult.success(0, "");
    }

    List<Map<String, Object>> dbRows =
        validRows.stream().map(row -> mapToDb(row, config.mappings())).collect(Collectors.toList());

    return databasePort.persist(dbRows, config, dryRun);
  }

  private Map<String, Object> mapToDb(Map<String, Object> row, List<DatabaseMapping> mappings) {
    Map<String, Object> dbRow = new HashMap<>();
    for (DatabaseMapping mapping : mappings) {
      if (row.containsKey(mapping.excelColumn())) {
        dbRow.put(mapping.dbColumn(), row.get(mapping.excelColumn()));
      }
    }
    return dbRow;
  }
}
