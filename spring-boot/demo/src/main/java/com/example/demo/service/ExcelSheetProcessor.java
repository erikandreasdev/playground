package com.example.demo.service;

import com.example.demo.domain.ColumnConfig;
import com.example.demo.domain.PersistenceResult;
import com.example.demo.domain.RowValidationResult;
import com.example.demo.domain.SheetConfig;
import com.example.demo.domain.SheetMetrics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;

/**
 * Service responsible for processing a single Excel sheet.
 *
 * <p>Orchestrates sheet-level validation:
 *
 * <ol>
 *   <li>Build header column mapping
 *   <li>Validate sheet structure (required columns exist)
 *   <li>Process each data row via ExcelRowValidator
 *   <li>Aggregate validation metrics
 *   <li>Persist valid rows to database
 * </ol>
 */
@Service
public class ExcelSheetProcessor {

  private final ExcelRowValidator rowValidator;
  private final ExcelPersistenceService persistenceService;
  private final DataFormatter dataFormatter;

  /**
   * Constructs the processor.
   *
   * @param rowValidator validator for individual rows
   * @param persistenceService service for database persistence
   */
  public ExcelSheetProcessor(
      ExcelRowValidator rowValidator, ExcelPersistenceService persistenceService) {
    this.rowValidator = rowValidator;
    this.persistenceService = persistenceService;
    this.dataFormatter = new DataFormatter();
  }

  /**
   * Processes an Excel sheet and validates all rows.
   *
   * @param sheet Excel sheet to process
   * @param config sheet configuration
   * @param persist whether to persist valid rows to database
   * @return sheet validation metrics
   */
  public SheetMetrics processSheet(Sheet sheet, SheetConfig config, boolean persist) {
    int total = 0;
    int valid = 0;
    int invalid = 0;

    List<String> sheetErrors = new ArrayList<>();
    List<Map<String, Object>> validRowData = new ArrayList<>();

    // Build header map
    Row headerRow = sheet.getRow(0);
    if (headerRow == null) {
      return new SheetMetrics(config.name(), 0, 0, 0, List.of("Empty sheet (no header)"), null);
    }

    Map<String, Integer> headerMap = buildHeaderMap(headerRow);

    // Validate structure
    List<String> structureErrors = validateStructure(config, headerMap);
    sheetErrors.addAll(structureErrors);

    // Process each data row (skip header)
    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
      Row row = sheet.getRow(i);
      if (row == null) {
        continue; // Skip empty rows
      }

      total++;
      RowValidationResult result = rowValidator.validateRow(row, config, headerMap, i + 1);

      if (result.isValid()) {
        valid++;
        validRowData.add(result.rowData());
      } else {
        invalid++;
        sheetErrors.addAll(result.errors());
      }
    }

    // Persist valid rows
    PersistenceResult persistenceResult =
        persistenceService.persistValidRows(validRowData, config.persistence(), !persist);

    return new SheetMetrics(config.name(), total, valid, invalid, sheetErrors, persistenceResult);
  }

  /**
   * Builds a map of column name to column index from the header row.
   *
   * @param headerRow Excel header row
   * @return map of column name (trimmed) to column index
   */
  private Map<String, Integer> buildHeaderMap(Row headerRow) {
    Map<String, Integer> headerMap = new HashMap<>();
    for (Cell cell : headerRow) {
      headerMap.put(dataFormatter.formatCellValue(cell).trim(), cell.getColumnIndex());
    }
    return headerMap;
  }

  /**
   * Validates that all required columns exist in the header.
   *
   * @param config sheet configuration
   * @param headerMap header column mapping
   * @return list of structure validation errors (empty if valid)
   */
  private List<String> validateStructure(SheetConfig config, Map<String, Integer> headerMap) {
    List<String> errors = new ArrayList<>();
    for (ColumnConfig colConfig : config.columns()) {
      if (colConfig.required() && !headerMap.containsKey(colConfig.name())) {
        errors.add("Required column missing: " + colConfig.name());
      }
    }
    return errors;
  }
}
