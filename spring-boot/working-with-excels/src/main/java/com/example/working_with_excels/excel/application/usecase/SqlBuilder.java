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

        String columns = String.join(", ", dbColumns);
        String placeholders = dbColumns.stream()
                .map(col -> ":" + col)
                .collect(Collectors.joining(", "));

        return String.format("INSERT INTO %s (%s) VALUES (%s)",
                sheetConfig.table(), columns, placeholders);
    }
}
