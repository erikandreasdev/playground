package com.example.working_with_excels.excel.application.dto;

/**
 * Aggregated metrics for an Excel import operation.
 *
 * @param totalRows        total number of data rows processed
 * @param insertedRows     number of rows successfully inserted
 * @param skippedRows      number of rows skipped due to validation errors
 * @param errorRows        number of rows that failed during database insert
 * @param lookupsPerformed total number of database lookups executed
 * @param dbTimeMs         total time spent on database operations
 *                         (milliseconds)
 */
public record ImportMetrics(
        int totalRows,
        int insertedRows,
        int skippedRows,
        int errorRows,
        int lookupsPerformed,
        long dbTimeMs) {

    /**
     * Creates an empty metrics instance for initialization.
     *
     * @return a new ImportMetrics with all values set to zero
     */
    public static ImportMetrics empty() {
        return new ImportMetrics(0, 0, 0, 0, 0, 0);
    }

    /**
     * Creates a combined metrics from two metrics instances.
     *
     * @param other the other metrics to combine with
     * @return a new ImportMetrics with summed values
     */
    public ImportMetrics combine(ImportMetrics other) {
        return new ImportMetrics(
                this.totalRows + other.totalRows,
                this.insertedRows + other.insertedRows,
                this.skippedRows + other.skippedRows,
                this.errorRows + other.errorRows,
                this.lookupsPerformed + other.lookupsPerformed,
                this.dbTimeMs + other.dbTimeMs);
    }
}
