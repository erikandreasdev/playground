package com.example.working_with_excels.excel.application.usecase;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.working_with_excels.excel.application.port.output.DatabasePort;
import com.example.working_with_excels.excel.domain.model.ImportMode;

import lombok.RequiredArgsConstructor;

/**
 * Service responsible for executing batch SQL operations.
 *
 * <p>
 * Handles batch execution with mode-aware behavior: in DRY_RUN mode,
 * operations are logged but not executed; in EXECUTE mode, operations
 * are performed against the database.
 */
@Service
@RequiredArgsConstructor
public class BatchExecutor {

    private static final Logger log = LoggerFactory.getLogger(BatchExecutor.class);

    private final DatabasePort databasePort;

    /**
     * Executes a batch of SQL operations.
     *
     * <p>
     * In DRY_RUN mode, logs the operations without executing them.
     * In EXECUTE mode, performs the actual database insertions.
     *
     * @param sql   the parameterized SQL statement to execute
     * @param batch the list of parameter maps for batch execution
     * @param mode  the import mode (DRY_RUN or EXECUTE)
     * @return the number of rows that were (or would be) inserted
     */
    public int executeBatch(String sql, List<Map<String, Object>> batch, ImportMode mode) {
        if (mode == ImportMode.DRY_RUN) {
            log.info("[DRY_RUN] Would execute {} inserts", batch.size());
            if (log.isDebugEnabled()) {
                for (Map<String, Object> params : batch) {
                    log.debug("[DRY_RUN] SQL: {} | Params: {}", sql, params);
                }
            }
            return batch.size();
        }

        return databasePort.executeBatch(sql, batch);
    }
}
