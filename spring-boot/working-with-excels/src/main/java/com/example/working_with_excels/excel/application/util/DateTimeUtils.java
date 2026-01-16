package com.example.working_with_excels.excel.application.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Utility class for safe date/time operations in SpEL expressions.
 * <p>
 * This class is designed to be injected into the SpEL evaluation context
 * to allow users to perform date comparisons in `skipExpression` without
 * exposing the full Java Time API or allowing arbitrary code execution.
 */
public class DateTimeUtils {

    /**
     * @return the current date (LocalDate)
     */
    public LocalDate now() {
        return LocalDate.now();
    }

    /**
     * @return the current date and time (LocalDateTime)
     */
    public LocalDateTime nowTs() {
        return LocalDateTime.now();
    }

    /**
     * @return the current date (java.util.Date)
     */
    public Date today() {
        return new Date();
    }

    /**
     * @return the current year
     */
    public int year() {
        return LocalDate.now().getYear();
    }
}
