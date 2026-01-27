package com.example.working_with_excels.excel.domain.model;

import java.util.List;

/**
 * Configuration for a row-level constraint involving multiple columns.
 * 
 * <p>
 * Supports two modes:
 * 1. <b>Tag-based Condition:</b> Uses a predefined {@link ConstraintType} (e.g.
 * NOT_ALL_EMPTY).
 * 2. <b>Dynamic Expression:</b> Uses a SpEL strings for custom validation
 * logic.
 *
 * @param columns        List of column names involved in this constraint
 * @param type           The type of constraint (TAG). If set, 'expression' is
 *                       ignored.
 * @param expression     SpEL expression for custom validation. Used if 'type'
 *                       is CUSTOM or null.
 * @param forbiddenValue Optional value to check against (context-dependent)
 * @param errorMessage   Custom error message to display if constraint fails
 */
public record RowConstraint(
        List<String> columns,
        ConstraintType type,
        String expression,
        String forbiddenValue,
        String errorMessage) {

    public enum ConstraintType {
        NOT_ALL_EMPTY,
        NOT_ALL_EQUAL,
        AT_LEAST_ONE_PRESENT,
        MUTUALLY_EXCLUSIVE,
        CUSTOM // Fallback for pure SpEL
    }
}
