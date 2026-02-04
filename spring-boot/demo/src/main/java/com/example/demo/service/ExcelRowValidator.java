package com.example.demo.service;

import com.example.demo.domain.ColumnConfig;
import com.example.demo.domain.RowValidationResult;
import com.example.demo.domain.SheetConfig;
import com.example.demo.validation.CellTransformer;
import com.example.demo.validation.CellValueValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

/**
 * Service responsible for validating a single Excel row.
 *
 * <p>Orchestrates the row validation process:
 *
 * <ol>
 *   <li>Phase 1: Apply cell transformations (TRIM, UPPERCASE, etc.)
 *   <li>Phase 2: Execute row operations (CONCATENATE, REPLACE, etc.)
 *   <li>Phase 3: Validate all columns (including computed columns)
 *   <li>Phase 4: Convert valid values to target data types
 * </ol>
 */
@Service
public class ExcelRowValidator {

  private final CellTransformer cellTransformer;
  private final CellValueValidator cellValueValidator;
  private final RowOperationProcessor rowOperationProcessor;
  private final DataFormatter dataFormatter;

  /**
   * Constructs the validator.
   *
   * @param cellTransformer transformer for cell values
   * @param cellValueValidator validator for cell values
   * @param rowOperationProcessor processor for row-level operations
   */
  public ExcelRowValidator(
      CellTransformer cellTransformer,
      CellValueValidator cellValueValidator,
      RowOperationProcessor rowOperationProcessor) {
    this.cellTransformer = cellTransformer;
    this.cellValueValidator = cellValueValidator;
    this.rowOperationProcessor = rowOperationProcessor;
    this.dataFormatter = new DataFormatter();
  }

  /**
   * Validates a single Excel row.
   *
   * @param row Excel row to validate
   * @param config sheet configuration
   * @param headerMap mapping of column name to column index
   * @param rowNum row number (for error messages, 1-indexed)
   * @return validation result with errors and row data
   */
  public RowValidationResult validateRow(
      Row row, SheetConfig config, Map<String, Integer> headerMap, int rowNum) {
    boolean isRowValid = true;
    List<String> errors = new ArrayList<>();
    Map<String, Object> rowData = new HashMap<>();

    // PHASE 1: Collect all transformed values for this row
    Map<String, Object> tempRowData = new LinkedHashMap<>();
    for (ColumnConfig colConfig : config.columns()) {
      Integer colIndex = headerMap.get(colConfig.name());
      if (colIndex != null) {
        Cell cell = row.getCell(colIndex);
        String cellValue = dataFormatter.formatCellValue(cell);
        String transformedValue = cellTransformer.apply(cellValue, colConfig.transformations());
        tempRowData.put(colConfig.name(), transformedValue);
      }
    }

    // PHASE 2: Apply row operations to compute additional columns
    if (config.rowOperations() != null && !config.rowOperations().isEmpty()) {
      rowOperationProcessor.applyRowOperations(tempRowData, config.rowOperations());
    }

    // Convert to String map for validation
    Map<String, String> rowValues = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : tempRowData.entrySet()) {
      rowValues.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
    }

    // PHASE 3: Validate each column with row context
    for (ColumnConfig colConfig : config.columns()) {
      Integer colIndex = headerMap.get(colConfig.name());
      if (colIndex == null) {
        // Column missing in Excel -> Invalid row
        isRowValid = false;
        continue;
      }

      String transformedValue = rowValues.get(colConfig.name());

      // Validation with row context (for database lookups)
      List<String> columnErrors =
          cellValueValidator.validateWithRowContext(
              colConfig.name(), transformedValue, rowValues, colConfig);

      if (!columnErrors.isEmpty()) {
        isRowValid = false;
        for (String error : columnErrors) {
          errors.add(
              String.format(
                  "Row %d, Col '%s': %s (value: '%s')",
                  rowNum, colConfig.name(), error, transformedValue));
        }
      } else {
        // PHASE 4: Convert valid value to target type
        rowData.put(colConfig.name(), convertValue(transformedValue, colConfig.type()));
      }
    }

    return new RowValidationResult(isRowValid, rowData, errors);
  }

  /**
   * Converts a string value to the target data type.
   *
   * @param value string value to convert
   * @param type target data type
   * @return converted value or null if value is blank
   */
  private Object convertValue(String value, com.example.demo.domain.DataType type) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return switch (type) {
        case NUMBER -> Double.valueOf(value);
        case DECIMAL -> new java.math.BigDecimal(value);
        case UUID -> java.util.UUID.fromString(value).toString(); // DB is VARCHAR
        case DATE -> {
          // Parse ISO 8601 date format (YYYY-MM-DD) to java.sql.Date
          java.time.LocalDate localDate = java.time.LocalDate.parse(value);
          yield java.sql.Date.valueOf(localDate);
        }
        default -> {
          // For STRING, BOOLEAN, EMAIL, etc., keep as string
          yield value;
        }
      };
    } catch (Exception e) {
      // Should not happen if validation passed
      return value;
    }
  }
}
