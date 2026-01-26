package com.example.working_with_excels.excel.application.usecase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import com.example.working_with_excels.excel.application.dto.ImportError;
import com.example.working_with_excels.excel.application.dto.RowProcessingResult;
import com.example.working_with_excels.excel.application.port.output.DatabasePort;
import com.example.working_with_excels.excel.domain.model.ColumnConfig;
import com.example.working_with_excels.excel.domain.model.DbColumnMapping;
import com.example.working_with_excels.excel.domain.model.LookupConfig;
import com.example.working_with_excels.excel.domain.service.CellValidator;
import com.example.working_with_excels.excel.domain.service.CellValueExtractor;

import lombok.RequiredArgsConstructor;

/**
 * Service responsible for processing individual Excel rows.
 *
 * <p>
 * This class orchestrates the row-level processing pipeline:
 * validation, value extraction, transformation, and lookup resolution.
 * Each cell in the row is validated and transformed according to its
 * column configuration.
 */
@Service
@RequiredArgsConstructor
public class ExcelRowProcessor {

    private final CellValidator cellValidator;
    private final CellValueExtractor cellValueExtractor;
    private final DatabasePort databasePort;
    private final org.springframework.expression.ExpressionParser parser = new org.springframework.expression.spel.standard.SpelExpressionParser();

    /**
     * Processes a single Excel row, extracting and validating all cell values.
     *
     * <p>
     * For each column with a database mapping, this method:
     * <ol>
     * <li>Validates the raw cell value</li>
     * <li>Extracts the typed value with transformations</li>
     * <li>Validates the transformed value</li>
     * <li>Resolves any lookup references</li>
     * </ol>
     *
     * @param row       the Excel row to process
     * @param rowNumber the 1-indexed row number (for error reporting)
     * @param columns   the list of column configurations
     * @return the processing result containing either named parameters or errors
     */
    /**
     * Processes a single Excel row, extracting and validating all cell values.
     *
     * <p>
     * For each column with a database mapping, this method:
     * <ol>
     * <li>Did we skip the row?</li>
     * <li>Validates the raw cell value</li>
     * <li>Extracts the typed value with transformations</li>
     * <li>Validates the transformed value</li>
     * <li>Resolves any lookup references</li>
     * </ol>
     *
     * @param row         the Excel row to process
     * @param rowNumber   the 1-indexed row number (for error reporting)
     * @param sheetConfig the sheet configuration
     * @return the processing result containing either named parameters or errors
     */
    public RowProcessingResult processRow(Row row, int rowNumber,
            com.example.working_with_excels.excel.domain.model.SheetConfig sheetConfig) {
        List<ColumnConfig> columns = sheetConfig.columns();

        // 1. Pre-extract values for row-level skip evaluation
        Map<String, Object> rowValues = extractRowValues(row, columns);

        // 2. Check Sheet-level SpEL skip expression
        if (evaluateSheetSkip(sheetConfig, rowValues)) {
            return RowProcessingResult.ofSkipped();
        }

        List<ImportError> errors = new ArrayList<>();
        Map<String, Object> namedParams = new HashMap<>();

        for (int i = 0; i < columns.size(); i++) {
            ColumnConfig colConfig = columns.get(i);
            Cell cell = row.getCell(i);

            // Column-level skip (legacy/specific)
            if (shouldSkipColumn(cell, colConfig)) {
                return RowProcessingResult.ofSkipped();
            }

            if (colConfig.dbMapping() == null) {
                continue;
            }

            String validationError = cellValidator.validate(cell, colConfig);
            if (validationError != null) {
                errors.add(ImportError.validation(rowNumber, colConfig.name(), validationError));
                continue;
            }

            Object cellValue = rowValues.get(colConfig.name());

            String transformedValidationError = cellValidator.validateTransformedValue(cellValue, colConfig);
            if (transformedValidationError != null) {
                errors.add(ImportError.validation(rowNumber, colConfig.name(), transformedValidationError));
                continue;
            }

            Object finalValue = resolveLookup(cellValue, colConfig.dbMapping(), rowNumber, colConfig.name(), errors);
            if (finalValue == null && colConfig.dbMapping().lookup() != null) {
                continue;
            }

            namedParams.put(colConfig.dbMapping().dbColumn(), finalValue);
        }

        if (!errors.isEmpty()) {
            return RowProcessingResult.invalid(errors);
        }

        return RowProcessingResult.valid(namedParams);
    }

    private Map<String, Object> extractRowValues(Row row, List<ColumnConfig> columns) {
        Map<String, Object> rowValues = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            ColumnConfig colConfig = columns.get(i);
            Cell cell = row.getCell(i);
            Object val = cellValueExtractor.extractTypedValue(cell, colConfig);
            rowValues.put(colConfig.name(), val);
        }
        return rowValues;
    }

    private boolean evaluateSheetSkip(com.example.working_with_excels.excel.domain.model.SheetConfig sheetConfig,
            Map<String, Object> rowValues) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setRootObject(rowValues);
        context.setVariable("dateTime", new com.example.working_with_excels.excel.application.util.DateTimeUtils());
        context.setVariable("db", new DbHelper(databasePort));
        rowValues.forEach(context::setVariable);

        // Check single expression
        if (sheetConfig.skipExpression() != null && !sheetConfig.skipExpression().isBlank()) {
            if (evaluateExpression(sheetConfig.skipExpression(), context)) {
                return true;
            }
        }

        // Check list of expressions
        if (sheetConfig.skipExpressions() != null) {
            for (String expression : sheetConfig.skipExpressions()) {
                if (expression != null && !expression.isBlank()) {
                    if (evaluateExpression(expression, context)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean evaluateExpression(String expression, StandardEvaluationContext context) {
        try {
            Boolean result = parser.parseExpression(expression).getValue(context, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            // Log and proceed (don't skip on error, safe default)
            return false;
        }
    }

    private boolean shouldSkipColumn(Cell cell, ColumnConfig colConfig) {
        if (shouldSkipByList(cell, colConfig)) {
            return true;
        }
        return shouldSkipByExpression(cell, colConfig);
    }

    private boolean shouldSkipByList(Cell cell, ColumnConfig colConfig) {
        if (colConfig.skipIf() == null || colConfig.skipIf().isEmpty()) {
            return false;
        }
        Object cellValue = cellValueExtractor.extractTypedValue(cell, colConfig);
        for (Object skipValue : colConfig.skipIf()) {
            if (valuesMatch(cellValue, skipValue)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldSkipByExpression(Cell cell, ColumnConfig colConfig) {
        boolean hasSkipExpression = colConfig.skipExpression() != null && !colConfig.skipExpression().isBlank();
        boolean hasSkipExpressions = colConfig.skipExpressions() != null && !colConfig.skipExpressions().isEmpty();

        if (!hasSkipExpression && !hasSkipExpressions) {
            return false;
        }

        Object cellValue = cellValueExtractor.extractTypedValue(cell, colConfig);
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setRootObject(cellValue);
        context.setVariable("dateTime", new com.example.working_with_excels.excel.application.util.DateTimeUtils());

        if (hasSkipExpression && evaluateExpression(colConfig.skipExpression(), context)) {
            return true;
        }

        if (hasSkipExpressions) {
            for (String expression : colConfig.skipExpressions()) {
                if (expression != null && !expression.isBlank() && evaluateExpression(expression, context)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class DbHelper {
        private final DatabasePort db;

        public DbHelper(DatabasePort db) {
            this.db = db;
        }

        public boolean exists(String table, String column, Object value) {
            if (value == null)
                return false;
            return db.lookup(table, column, value, column).isPresent();
        }

        public Object lookup(String table, String column, Object value, String returnCol) {
            return db.lookup(table, column, value, returnCol).orElse(null);
        }
    }

    private boolean valuesMatch(Object cellValue, Object skipValue) {
        if (cellValue == null) {
            return skipValue == null || "null".equalsIgnoreCase(skipValue.toString())
                    || "None".equalsIgnoreCase(skipValue.toString());
        }
        if (skipValue == null) {
            return false;
        }

        // Handle numeric comparison specifically (e.g. 1 == 1.0)
        if (cellValue instanceof Number n1 && skipValue instanceof Number n2) {
            return n1.doubleValue() == n2.doubleValue();
        }

        return cellValue.toString().equalsIgnoreCase(skipValue.toString());
    }

    /**
     * Resolves a lookup value if configured, otherwise returns the original value.
     *
     * @param cellValue  the extracted cell value
     * @param mapping    the database column mapping with optional lookup config
     * @param rowNumber  the row number for error reporting
     * @param columnName the column name for error reporting
     * @param errors     the error list to append lookup failures to
     * @return the resolved value, or null if lookup failed
     */
    private Object resolveLookup(Object cellValue, DbColumnMapping mapping,
            int rowNumber, String columnName, List<ImportError> errors) {

        if (mapping.lookup() == null) {
            return cellValue;
        }

        LookupConfig lookup = mapping.lookup();
        String lookupKey = cellValue != null ? cellValue.toString() : null;
        Optional<Object> lookedUp = databasePort.lookup(
                lookup.table(), lookup.matchColumn(), lookupKey, lookup.returnColumn());

        if (lookedUp.isEmpty()) {
            errors.add(ImportError.lookup(rowNumber, columnName,
                    String.format("Lookup failed: no match for '%s' in %s.%s",
                            lookupKey, lookup.table(), lookup.matchColumn())));
            return null;
        }

        return lookedUp.get();
    }
}
