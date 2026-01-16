package com.example.working_with_excels.excel.application.port.output;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Output port abstracting database operations for Excel imports.
 *
 * <p>
 * This interface defines the contract for database interactions,
 * allowing different implementations for real database operations
 * and dry-run logging. Uses named parameters for order-independent binding.
 */
public interface DatabasePort {

    /**
     * Executes a batch of SQL statements with named parameters.
     *
     * <p>
     * SQL statements use named parameter syntax (e.g., {@code :columnName})
     * which are matched against the keys in the parameter maps.
     *
     * @param sql            the SQL statement with named parameters (e.g., :userId,
     *                       :name)
     * @param parameterBatch list of parameter maps, one per row to insert
     * @return the number of rows successfully inserted
     */
    int executeBatch(String sql, List<Map<String, Object>> parameterBatch);

    /**
     * Performs a lookup query to resolve a value to another value (typically an
     * ID).
     *
     * @param table        the table to query
     * @param matchColumn  the column to match against
     * @param value        the value to search for
     * @param returnColumn the column value to return
     * @return the looked-up value, or empty if not found
     */
    Optional<Object> lookup(String table, String matchColumn, Object value, String returnColumn);

    /**
     * Begins a new database transaction.
     */
    void beginTransaction();

    /**
     * Commits the current transaction.
     */
    void commitTransaction();

    /**
     * Rolls back the current transaction.
     */
    void rollbackTransaction();
}
