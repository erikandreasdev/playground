package com.example.working_with_excels.excel.domain.model;

import java.util.List;

/**
 * Value object representing configuration for a single Excel sheet.
 *
 * <p>
 * This record defines the expected sheet name, column configurations,
 * and database import settings for a sheet within an Excel file.
 *
 * @param name            the expected name of the Excel sheet
 * @param columns         the ordered list of column configurations
 * @param table           optional target database table name for imports
 * @param onError         optional error handling strategy (defaults to
 *                        SKIP_ROW)
 * @param errorMessage    optional message to display when a sheet-level error
 *                        occurs
 * @param batchSize       optional batch size for inserts (defaults to 100)
 * @param customSql       optional custom SQL for complex insert logic
 * @param skipExpression  optional SpEL expression to skip rows
 * @param skipExpressions optional list of SpEL expressions to skip rows
 * @param primaryKey      optional list of columns forming the primary key
 * @param rowConstraints  optional list of cross-column validation rules
 */
public record SheetConfig(
        String name,
        List<ColumnConfig> columns,
        String table,
        ErrorStrategy onError,
        String errorMessage,
        Integer batchSize,
        String customSql,
        String skipExpression,
        List<String> skipExpressions,
        List<String> primaryKey,
        List<RowConstraint> rowConstraints) {

    /** Default batch size for database inserts. */
    public static final int DEFAULT_BATCH_SIZE = 100;

    /**
     * Gets the batch size, using default if not specified.
     *
     * @return the configured batch size or DEFAULT_BATCH_SIZE
     */
    public int getEffectiveBatchSize() {
        return batchSize != null ? batchSize : DEFAULT_BATCH_SIZE;
    }

    /**
     * Gets the error strategy, using default if not specified.
     *
     * @return the configured error strategy or SKIP_ROW
     */
    public ErrorStrategy getEffectiveErrorStrategy() {
        return onError != null ? onError : ErrorStrategy.SKIP_ROW;
    }

    /**
     * Checks if this sheet has custom SQL configured.
     *
     * @return true if custom SQL is provided
     */
    public boolean hasCustomSql() {
        return customSql != null && !customSql.isBlank();
    }

    /**
     * Checks if this sheet has a primary key configured.
     *
     * @return true if primary key is provided
     */
    public boolean hasPrimaryKey() {
        return primaryKey != null && !primaryKey.isEmpty();
    }

    /**
     * Gets the primary key columns, returning empty list if null.
     *
     * @return the primary key columns or empty list
     */
    public List<String> getEffectivePrimaryKey() {
        return primaryKey != null ? primaryKey : List.of();
    }

    /**
     * Gets the row constraints, returning empty list if null.
     */
    public List<RowConstraint> getEffectiveRowConstraints() {
        return rowConstraints != null ? rowConstraints : List.of();
    }
}
