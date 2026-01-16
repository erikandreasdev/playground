package com.example.working_with_excels.excel.infrastructure.adapter.output;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.working_with_excels.excel.application.port.output.DatabasePort;

/**
 * Dry-run implementation of DatabasePort for testing and validation.
 *
 * <p>
 * This adapter logs all SQL statements and parameters without executing
 * them against a real database. Useful for validating import configurations
 * before running actual imports.
 */
public class DryRunDatabaseAdapter implements DatabasePort {

    private static final Logger log = LoggerFactory.getLogger(DryRunDatabaseAdapter.class);

    private int totalBatches = 0;
    private int totalRows = 0;

    @Override
    public int executeBatch(String sql, List<Map<String, Object>> parameterBatch) {
        if (parameterBatch == null || parameterBatch.isEmpty()) {
            return 0;
        }

        totalBatches++;
        totalRows += parameterBatch.size();

        log.info("[DRY_RUN] Batch #{}: {} rows", totalBatches, parameterBatch.size());
        log.info("[DRY_RUN] SQL: {}", sql);

        for (int i = 0; i < parameterBatch.size(); i++) {
            Map<String, Object> params = parameterBatch.get(i);
            if (log.isDebugEnabled()) {
                log.debug("[DRY_RUN] Row {}: {}", i + 1, formatParams(params));
            }
        }

        return parameterBatch.size();
    }

    @Override
    public Optional<Object> lookup(String table, String matchColumn, Object value, String returnColumn) {
        log.info("[DRY_RUN] Lookup: SELECT {} FROM {} WHERE {} = '{}'",
                returnColumn, table, matchColumn, value);

        // Return a mock value to allow the import to proceed
        String mockValue = "[MOCK_" + returnColumn.toUpperCase() + "]";
        log.debug("[DRY_RUN] Returning mock value: {}", mockValue);

        return Optional.of(mockValue);
    }

    @Override
    public void beginTransaction() {
        log.info("[DRY_RUN] BEGIN TRANSACTION");
    }

    @Override
    public void commitTransaction() {
        log.info("[DRY_RUN] COMMIT TRANSACTION (total batches: {}, total rows: {})", totalBatches, totalRows);
    }

    @Override
    public void rollbackTransaction() {
        log.info("[DRY_RUN] ROLLBACK TRANSACTION");
    }

    /**
     * Gets the total number of batches processed in dry-run mode.
     *
     * @return the batch count
     */
    public int getTotalBatches() {
        return totalBatches;
    }

    /**
     * Gets the total number of rows processed in dry-run mode.
     *
     * @return the row count
     */
    public int getTotalRows() {
        return totalRows;
    }

    /**
     * Resets the counters for a new dry-run session.
     */
    public void reset() {
        totalBatches = 0;
        totalRows = 0;
    }

    private String formatParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(entry.getKey()).append("=");
            if (entry.getValue() == null) {
                sb.append("NULL");
            } else if (entry.getValue() instanceof String) {
                sb.append("'").append(entry.getValue()).append("'");
            } else {
                sb.append(entry.getValue());
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
