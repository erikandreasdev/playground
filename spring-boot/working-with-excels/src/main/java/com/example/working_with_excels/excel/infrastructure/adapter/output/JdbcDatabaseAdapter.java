package com.example.working_with_excels.excel.infrastructure.adapter.output;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.example.working_with_excels.excel.application.port.output.DatabasePort;

/**
 * JDBC implementation of DatabasePort for real database operations.
 *
 * <p>
 * Uses Spring's NamedParameterJdbcTemplate for batch inserts with named
 * parameters,
 * making parameter order irrelevant. Handles Oracle-specific quirks and
 * provides
 * transaction management.
 */
public class JdbcDatabaseAdapter implements DatabasePort {

    private static final Logger log = LoggerFactory.getLogger(JdbcDatabaseAdapter.class);

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private TransactionStatus currentTransaction;

    /**
     * Creates a new JDBC database adapter.
     *
     * @param namedJdbcTemplate  the named parameter JDBC template
     * @param transactionManager the transaction manager for transaction control
     */
    public JdbcDatabaseAdapter(NamedParameterJdbcTemplate namedJdbcTemplate,
            PlatformTransactionManager transactionManager) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.transactionManager = transactionManager;
    }

    @Override
    public int executeBatch(String sql, List<Map<String, Object>> parameterBatch) {
        if (parameterBatch == null || parameterBatch.isEmpty()) {
            return 0;
        }

        log.debug("Executing batch insert: {} rows", parameterBatch.size());

        try {

            SqlParameterSource[] batch = parameterBatch.stream()
                    .map(MapSqlParameterSource::new)
                    .toArray(SqlParameterSource[]::new);
            int[] results = namedJdbcTemplate.batchUpdate(Objects.requireNonNull(sql), Objects.requireNonNull(batch));

            int totalInserted = 0;
            for (int result : results) {
                if (result >= 0) {
                    totalInserted += result;
                } else if (result == java.sql.Statement.SUCCESS_NO_INFO) {
                    totalInserted++;
                }
            }

            log.debug("Batch insert completed: {} rows inserted", totalInserted);
            return totalInserted;

        } catch (DataAccessException e) {
            throw new DatabaseAdapterException("Batch insert failed", e);
        }
    }

    @Override
    public Optional<Object> lookup(String table, String matchColumn, Object value, String returnColumn) {
        if (value == null) {
            return Optional.empty();
        }

        String sql = String.format("SELECT %s FROM %s WHERE %s = :value", returnColumn, table, matchColumn);

        log.debug("Executing lookup: {} WHERE {}={}", table, matchColumn, value);

        try {
            List<Object> results = namedJdbcTemplate.query(Objects.requireNonNull(sql),
                    new MapSqlParameterSource("value", value),
                    (rs, rowNum) -> rs.getObject(1));

            if (results.isEmpty()) {
                log.debug("Lookup returned no results");
                return Optional.empty();
            }

            log.debug("Lookup found: {}", results.getFirst());
            return Optional.of(results.getFirst());

        } catch (DataAccessException e) {
            log.error("Lookup failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void beginTransaction() {
        if (currentTransaction != null) {
            log.warn("Transaction already in progress, not starting a new one");
            return;
        }

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

        currentTransaction = transactionManager.getTransaction(def);
        log.debug("Transaction started");
    }

    @Override
    public void commitTransaction() {
        if (currentTransaction == null) {
            log.warn("No transaction to commit");
            return;
        }

        transactionManager.commit(currentTransaction);
        currentTransaction = null;
        log.debug("Transaction committed");
    }

    @Override
    public void rollbackTransaction() {
        if (currentTransaction == null) {
            log.warn("No transaction to rollback");
            return;
        }

        transactionManager.rollback(currentTransaction);
        currentTransaction = null;
        log.debug("Transaction rolled back");
    }
}
