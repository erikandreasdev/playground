package com.example.demo.core.internal.services;

import com.example.demo.core.internal.domain.config.ColumnConfig;
import com.example.demo.core.internal.domain.config.SheetConfig;
import com.example.demo.core.internal.domain.enums.DataType;
import com.example.demo.core.internal.domain.result.RowValidationResult;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

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
public class ExcelRowValidatorService {

  private final CellTransformerService cellTransformer;
  private final CellValidatorService cellValidator;
  private final RowOperationService rowOperationService;
  private final DataFormatter dataFormatter;

  /**
   * Constructs the validator.
   *
   * @param cellTransformer transformer for cell values
   * @param cellValidator validator for cell values
   * @param rowOperationService service for row-level operations
   */
  public ExcelRowValidatorService(
      CellTransformerService cellTransformer,
      CellValidatorService cellValidator,
      RowOperationService rowOperationService) {
    this.cellTransformer = cellTransformer;
    this.cellValidator = cellValidator;
    this.rowOperationService = rowOperationService;
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
      rowOperationService.applyRowOperations(tempRowData, config.rowOperations());
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
          cellValidator.validateWithRowContext(
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
  private Object convertValue(String value, DataType type) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return switch (type) {
        case NUMBER -> Double.valueOf(value);
        case DECIMAL -> new BigDecimal(value);
        case UUID -> UUID.fromString(value).toString(); // DB is VARCHAR
        case DATE -> {
          // Parse ISO 8601 date format (YYYY-MM-DD) to java.sql.Date
          LocalDate localDate = LocalDate.parse(value);
          yield Date.valueOf(localDate);
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
