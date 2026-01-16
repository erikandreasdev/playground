package com.example.working_with_excels.excel.application.usecase;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.working_with_excels.excel.domain.model.SheetConfig;

/**
 * Service responsible for building SQL statements for Excel import operations.
 *
 * <p>
 * This class encapsulates SQL generation logic, creating parameterized INSERT
 * statements based on sheet configuration. Uses named parameters (e.g.,
 * :column_name)
 * for safe batch execution.
 */
@Service
public class SqlBuilder {

        /**
         * Builds an INSERT SQL statement from sheet configuration.
         *
         * <p>
         * Generates a parameterized INSERT statement with named placeholders
         * for all columns that have a database mapping configured.
         *
         * @param sheetConfig the sheet configuration containing column mappings
         * @return the parameterized INSERT SQL statement
         */
        public String buildInsertSql(SheetConfig sheetConfig) {
                List<String> dbColumns = sheetConfig.columns().stream()
                                .filter(c -> c.dbMapping() != null)
                                .map(c -> c.dbMapping().dbColumn())
                                .toList();

                if (sheetConfig.hasPrimaryKey()) {
                        return buildMergeSql(sheetConfig, dbColumns);
                }

                String columns = String.join(", ", dbColumns);
                String placeholders = dbColumns.stream()
                                .map(col -> ":" + col)
                                .collect(Collectors.joining(", "));

                return String.format("INSERT INTO %s (%s) VALUES (%s)",
                                sheetConfig.table(), columns, placeholders);
        }

        private String buildMergeSql(SheetConfig sheetConfig, List<String> dbColumns) {
                String tableName = sheetConfig.table();
                List<String> keys = sheetConfig.getEffectivePrimaryKey();

                // 1. USING Clause (SELECT :col AS col)
                String usingClause = dbColumns.stream()
                                .map(col -> String.format(":%s AS %s", col, col))
                                .collect(Collectors.joining(", "));

                // 2. ON Clause (t.key = s.key)
                String onClause = keys.stream()
                                .map(key -> String.format("t.%s = s.%s", key, key))
                                .collect(Collectors.joining(" AND "));

                // 3. UPDATE Clause (exclude keys)
                String updateClause = dbColumns.stream()
                                .filter(col -> !keys.contains(col))
                                .map(col -> String.format("t.%s = s.%s", col, col))
                                .collect(Collectors.joining(", "));

                // 4. INSERT Clause
                String insertCols = String.join(", ", dbColumns);
                String insertVals = dbColumns.stream()
                                .map(col -> "s." + col)
                                .collect(Collectors.joining(", "));

                StringBuilder sql = new StringBuilder();
                sql.append("MERGE INTO ").append(tableName).append(" t ");
                sql.append("USING (SELECT ").append(usingClause).append(" FROM dual) s ");
                sql.append("ON (").append(onClause).append(") ");

                if (!updateClause.isEmpty()) {
                        sql.append("WHEN MATCHED THEN UPDATE SET ").append(updateClause).append(" ");
                }

                sql.append("WHEN NOT MATCHED THEN INSERT (").append(insertCols).append(") ");
                sql.append("VALUES (").append(insertVals).append(")");

                return sql.toString();
        }
}
