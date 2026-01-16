package com.example.working_with_excels.excel.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for tracking and logging import progress.
 *
 * <p>
 * Provides progress updates at configurable intervals (default: every 10%)
 * to give visibility into long-running import operations without flooding
 * the logs.
 */
@Service
public class ImportProgressTracker {

    private static final Logger log = LoggerFactory.getLogger(ImportProgressTracker.class);
    private static final int DEFAULT_LOG_INTERVAL = 10;

    private int lastLoggedPercentage;
    private final int logInterval;

    /**
     * Creates a progress tracker with the default 10% logging interval.
     */
    public ImportProgressTracker() {
        this.logInterval = DEFAULT_LOG_INTERVAL;
        this.lastLoggedPercentage = 0;
    }

    /**
     * Resets the progress tracker for a new sheet or import operation.
     */
    public void reset() {
        this.lastLoggedPercentage = 0;
    }

    /**
     * Tracks progress and logs when the next interval threshold is reached.
     *
     * @param currentRow the current row being processed (1-indexed)
     * @param totalRows  the total number of rows to process
     * @param sheetName  the name of the sheet being processed (for logging context)
     */
    public void trackProgress(int currentRow, int totalRows, String sheetName) {
        if (totalRows <= 0) {
            return;
        }

        int percentage = (currentRow * 100) / totalRows;
        int nextThreshold = lastLoggedPercentage + logInterval;

        if (percentage >= nextThreshold) {
            lastLoggedPercentage = (percentage / logInterval) * logInterval;
            log.info("[{}] Progress: {}% ({}/{} rows)", sheetName, lastLoggedPercentage, currentRow, totalRows);
        }
    }

    /**
     * Returns the last logged percentage for testing purposes.
     *
     * @return the last percentage that was logged
     */
    public int getLastLoggedPercentage() {
        return lastLoggedPercentage;
    }
}
