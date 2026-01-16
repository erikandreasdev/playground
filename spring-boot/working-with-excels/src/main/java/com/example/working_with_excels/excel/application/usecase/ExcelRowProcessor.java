package com.example.working_with_excels.excel.application.usecase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
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
    public RowProcessingResult processRow(Row row, int rowNumber, List<ColumnConfig> columns) {
        List<ImportError> errors = new ArrayList<>();
        Map<String, Object> namedParams = new HashMap<>();

        for (int i = 0; i < columns.size(); i++) {
            ColumnConfig colConfig = columns.get(i);
            Cell cell = row.getCell(i);

            if (colConfig.dbMapping() == null) {
                continue;
            }

            String validationError = cellValidator.validate(cell, colConfig);
            if (validationError != null) {
                errors.add(ImportError.validation(rowNumber, colConfig.name(), validationError));
                continue;
            }

            Object cellValue = cellValueExtractor.extractTypedValue(cell, colConfig);

            String transformedValidationError = cellValidator.validateTransformedValue(cellValue, colConfig);
            if (transformedValidationError != null) {
                errors.add(ImportError.validation(rowNumber, colConfig.name(), transformedValidationError));
                continue;
            }

            Object finalValue = resolveLookup(cellValue, colConfig.dbMapping(), rowNumber, colConfig.name(), errors);
            if (finalValue == null && colConfig.dbMapping().lookup() != null) {
                // Lookup failed and error was added
                continue;
            }

            namedParams.put(colConfig.dbMapping().dbColumn(), finalValue);
        }

        if (!errors.isEmpty()) {
            return RowProcessingResult.invalid(errors);
        }

        return RowProcessingResult.valid(namedParams);
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
