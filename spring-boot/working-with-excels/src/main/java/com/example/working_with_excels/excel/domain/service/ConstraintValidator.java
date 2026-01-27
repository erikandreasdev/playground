package com.example.working_with_excels.excel.domain.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import com.example.working_with_excels.excel.domain.model.RowConstraint;
import com.example.working_with_excels.excel.domain.model.RowConstraint.ConstraintType;

/**
 * Service for validating cross-column row constraints.
 */
@Service
public class ConstraintValidator {

    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * Validates a row against a list of constraints.
     *
     * @param rowValues      Map of column names to extracted values
     * @param rowConstraints List of constraints to check
     * @return First error message found, or null if valid
     */
    public String validate(Map<String, Object> rowValues, List<RowConstraint> rowConstraints) {
        if (rowConstraints == null || rowConstraints.isEmpty()) {
            return null;
        }

        for (RowConstraint constraint : rowConstraints) {
            if (!evaluate(constraint, rowValues)) {
                return constraint.errorMessage() != null
                        ? constraint.errorMessage()
                        : "Row constraint failed: " + constraint.type();
            }
        }
        return null; // All valid
    }

    private boolean evaluate(RowConstraint constraint, Map<String, Object> rowValues) {
        ConstraintType type = constraint.type();

        // Default to CUSTOM if type is null but expression exists
        if (type == null && constraint.expression() != null) {
            type = ConstraintType.CUSTOM;
        }

        if (type == null) {
            return true; // No Op
        }

        return switch (type) {
            case NOT_ALL_EMPTY -> checkNotAllEmpty(constraint.columns(), rowValues);
            case NOT_ALL_EQUAL -> checkNotAllEqual(constraint.columns(), rowValues, constraint.forbiddenValue());
            case AT_LEAST_ONE_PRESENT -> checkAtLeastOnePresent(constraint.columns(), rowValues);
            case MUTUALLY_EXCLUSIVE -> checkMutuallyExclusive(constraint.columns(), rowValues);
            case CUSTOM -> evaluateSpel(constraint.expression(), rowValues);
        };
    }

    private boolean checkNotAllEmpty(List<String> columns, Map<String, Object> rowValues) {
        if (columns == null || columns.isEmpty())
            return true;
        for (String col : columns) {
            Object val = rowValues.get(col);
            if (val != null && !val.toString().isBlank()) {
                return true; // Found one non-empty
            }
        }
        return false; // All were empty
    }

    private boolean checkNotAllEqual(List<String> columns, Map<String, Object> rowValues, String forbiddenValue) {
        if (columns == null || columns.size() < 2)
            return true;

        Object firstVal = rowValues.get(columns.get(0));
        boolean allMatch = true;

        for (String col : columns) {
            Object val = rowValues.get(col);
            if (!Objects.equals(firstVal, val)) {
                allMatch = false;
                break;
            }
        }

        if (allMatch) {
            // If they are all equal, fail IF they match the forbidden value (if provided)
            // OR if we just don't want them to be equal at all (forbiddenValue is null)
            if (forbiddenValue != null) {
                return !String.valueOf(firstVal).equals(forbiddenValue);
            }
            return false; // They are equal, dependent on requirement. Usually NOT_ALL_EQUAL means
                          // strictly "must differ".
            // But let's assume if forbiddenValue is set, we logic "Not all equal TO value".
            // Actually standard sematics: "Columns A and B cannot have same value".
        }

        // If they are NOT all equal, we are good.
        // Wait, if forbiddenValue is present, does it mean "Columns A and B cannot both
        // be 'restricted'"?
        // Yes, that's what the user example implied: forbiddenValue: "restricted"

        if (forbiddenValue != null) {
            // Check if ALL columns equal the forbidden value
            boolean allForbidden = true;
            for (String col : columns) {
                Object val = rowValues.get(col);
                if (!String.valueOf(val).equals(forbiddenValue)) {
                    allForbidden = false;
                    break;
                }
            }
            return !allForbidden;
        }

        return true;
    }

    private boolean checkAtLeastOnePresent(List<String> columns, Map<String, Object> rowValues) {
        return checkNotAllEmpty(columns, rowValues); // Same logic
    }

    private boolean checkMutuallyExclusive(List<String> columns, Map<String, Object> rowValues) {
        if (columns == null)
            return true;
        int presentCount = 0;
        for (String col : columns) {
            Object val = rowValues.get(col);
            if (val != null && !val.toString().isBlank()) {
                presentCount++;
            }
        }
        return presentCount <= 1;
    }

    private boolean evaluateSpel(String expression, Map<String, Object> rowValues) {
        if (expression == null || expression.isBlank())
            return true;
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("row", rowValues);
            rowValues.forEach(context::setVariable);

            Boolean result = parser.parseExpression(expression).getValue(context, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            // Log error in real app
            return false;
        }
    }
}
