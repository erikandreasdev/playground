package com.example.working_with_excels.excel.application.dto;

import java.time.Duration;
import java.util.List;

import com.example.working_with_excels.excel.domain.model.ImportMode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Complete report for an Excel import operation.
 *
 * <p>
 * Contains metadata, metrics, and per-sheet results for the entire import.
 *
 * @param filename    the name of the imported Excel file
 * @param mappingFile the path to the YAML configuration file used
 * @param mode        the import mode (EXECUTE or DRY_RUN)
 * @param duration    total time taken for the import operation
 * @param sheets      list of per-sheet import results
 * @param metrics     aggregated metrics across all sheets
 */
public record ImportReport(
        String filename,
        String mappingFile,
        ImportMode mode,
        @JsonIgnore Duration duration,
        List<SheetImportResult> sheets,
        ImportMetrics metrics) {

    /**
     * Returns the duration in human-readable format.
     *
     * @return formatted duration (e.g., "1.5s", "250ms", "2m 30s")
     */
    @JsonProperty("duration")
    public String getDurationFormatted() {
        if (duration == null) {
            return "0ms";
        }

        long totalMs = duration.toMillis();
        if (totalMs < 1000) {
            return totalMs + "ms";
        } else if (totalMs < 60000) {
            return String.format("%.2fs", totalMs / 1000.0);
        } else {
            long minutes = totalMs / 60000;
            long seconds = (totalMs % 60000) / 1000;
            return minutes + "m " + seconds + "s";
        }
    }

    /**
     * Checks if the entire import completed without errors.
     *
     * @return true if all sheets were imported successfully
     */
    public boolean isSuccess() {
        return sheets.stream().allMatch(SheetImportResult::isSuccess);
    }

    /**
     * Gets the total number of errors across all sheets.
     *
     * @return total error count
     */
    public int getTotalErrors() {
        return sheets.stream()
                .mapToInt(s -> s.errors() != null ? s.errors().size() : 0)
                .sum();
    }
}
