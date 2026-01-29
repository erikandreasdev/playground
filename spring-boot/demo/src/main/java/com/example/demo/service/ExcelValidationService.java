package com.example.demo.service;

import com.example.demo.domain.ColumnConfig;
import com.example.demo.domain.FileMetadata;
import com.example.demo.domain.PersistenceResult;
import com.example.demo.domain.SheetConfig;
import com.example.demo.domain.SheetMetrics;
import com.example.demo.domain.ValidationConfig;
import com.example.demo.domain.ValidationMetrics;
import com.example.demo.domain.ValidationReport;
import com.example.demo.domain.ValidationStatus;
import com.example.demo.resource.LoadedResource;
import com.example.demo.validation.CellTransformer;
import com.example.demo.validation.CellValueValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

/** Service to validate Excel files against configuration. */
@Service
public class ExcelValidationService {

  private final CellTransformer cellTransformer;
  private final CellValueValidator cellValueValidator;
  private final ExcelPersistenceService persistenceService;
  private final ObjectMapper yamlMapper;
  private final DataFormatter dataFormatter;

  /**
   * Constructs the service.
   *
   * @param cellTransformer transformer for cell values
   * @param cellValueValidator validator for cell values
   * @param persistenceService service for database persistence
   */
  public ExcelValidationService(
      CellTransformer cellTransformer,
      CellValueValidator cellValueValidator,
      ExcelPersistenceService persistenceService) {
    this.cellTransformer = cellTransformer;
    this.cellValueValidator = cellValueValidator;
    this.persistenceService = persistenceService;
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
    this.dataFormatter = new DataFormatter();
  }

  /**
   * Validates the loaded excel resource against the config resource.
   *
   * @param excel loaded excel resource
   * @param configResource loaded config resource
   * @param persist whether to persist valid rows to database
   * @return report of validation
   */
  public ValidationReport validate(
      LoadedResource excel, LoadedResource configResource, boolean persist) {
    ValidationConfig config;

    try (InputStream is = configResource.resource().getInputStream()) {
      config = yamlMapper.readValue(is, ValidationConfig.class);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new com.example.demo.exception.MappingException(
          "Invalid validation configuration: " + e.getOriginalMessage(), e);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read validation config", e);
    }

    List<String> globalErrors = new ArrayList<>();
    List<SheetMetrics> sheetMetricsList = new ArrayList<>();
    AtomicInteger globalTotal = new AtomicInteger(0);
    AtomicInteger globalValid = new AtomicInteger(0);
    AtomicInteger globalInvalid = new AtomicInteger(0);
    AtomicInteger globalPersisted = new AtomicInteger(0);

    try (InputStream excelIs = excel.resource().getInputStream();
        Workbook workbook = WorkbookFactory.create(excelIs)) {

      for (SheetConfig sheetConfig : config.sheets()) {
        Sheet sheet = workbook.getSheet(sheetConfig.name());
        if (sheet == null) {
          String msg = "Sheet missing: " + sheetConfig.name();
          globalErrors.add(msg);
          sheetMetricsList.add(
              new SheetMetrics(
                  sheetConfig.name(), 0, 0, 0, List.of("Missing from workbook"), null));
          continue;
        }

        SheetMetrics metrics = validateSheet(sheet, sheetConfig, persist);
        sheetMetricsList.add(metrics);

        globalTotal.addAndGet(metrics.totalRows());
        globalValid.addAndGet(metrics.validRows());
        globalInvalid.addAndGet(metrics.invalidRows());
        if (metrics.persistence() != null) {
          globalPersisted.addAndGet(metrics.persistence().rowsAffected());
        }
      }

    } catch (IOException e) {
      throw new RuntimeException("Failed to read Excel file", e);
    }

    ValidationMetrics globalMetrics =
        new ValidationMetrics(
            globalTotal.get(), globalValid.get(), globalInvalid.get(), globalPersisted.get());

    ValidationStatus status =
        (globalInvalid.get() == 0 && globalErrors.isEmpty())
            ? ValidationStatus.SUCCESS
            : ValidationStatus.FAILED;

    if (globalValid.get() > 0 && (globalInvalid.get() > 0 || !globalErrors.isEmpty())) {
      status = ValidationStatus.PARTIAL_SUCCESS;
    }

    FileMetadata excelMetadata =
        new FileMetadata(excel.filename(), excel.size(), excel.sourceType().toString());

    FileMetadata configMetadata =
        new FileMetadata(
            configResource.filename(),
            configResource.size(),
            configResource.sourceType().toString());

    return new ValidationReport(
        status, excelMetadata, configMetadata, globalMetrics, sheetMetricsList, globalErrors);
  }

  private SheetMetrics validateSheet(Sheet sheet, SheetConfig config, boolean persist) {
    int total = 0;
    int valid = 0;
    int invalid = 0;

    List<String> sheetErrors = new ArrayList<>();
    List<Map<String, Object>> validRowData = new ArrayList<>();

    // Map column name to index
    Row headerRow = sheet.getRow(0);
    if (headerRow == null) {
      return new SheetMetrics(config.name(), 0, 0, 0, List.of("Empty sheet (no header)"), null);
    }

    Map<String, Integer> headerMap = new HashMap<>();
    for (Cell cell : headerRow) {
      headerMap.put(dataFormatter.formatCellValue(cell).trim(), cell.getColumnIndex());
    }

    // Structural check: do all required columns exist?
    for (ColumnConfig colConfig : config.columns()) {
      if (colConfig.required() && !headerMap.containsKey(colConfig.name())) {
        sheetErrors.add("Required column missing: " + colConfig.name());
      }
    }

    // Iterate rows (skip header)
    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
      Row row = sheet.getRow(i);
      if (row == null) {
        continue; // Skip empty rows
      }

      total++;
      Map<String, Object> rowData = new HashMap<>();
      if (validateRow(row, config, headerMap, sheetErrors, i + 1, rowData)) {
        valid++;
        validRowData.add(rowData);
      } else {
        invalid++;
      }
    }

    PersistenceResult persistenceResult =
        persistenceService.persistValidRows(validRowData, config.persistence(), !persist);

    return new SheetMetrics(config.name(), total, valid, invalid, sheetErrors, persistenceResult);
  }

  private boolean validateRow(
      Row row,
      SheetConfig config,
      Map<String, Integer> headerMap,
      List<String> sheetErrors,
      int rowNum,
      Map<String, Object> rowData) {
    boolean isRowValid = true;

    // First pass: collect all transformed values for this row
    Map<String, String> rowValues = new java.util.LinkedHashMap<>();
    for (ColumnConfig colConfig : config.columns()) {
      Integer colIndex = headerMap.get(colConfig.name());
      if (colIndex != null) {
        Cell cell = row.getCell(colIndex);
        String cellValue = dataFormatter.formatCellValue(cell);
        String transformedValue = cellTransformer.apply(cellValue, colConfig.transformations());
        rowValues.put(colConfig.name(), transformedValue);
      }
    }

    // Second pass: validate each column with row context
    for (ColumnConfig colConfig : config.columns()) {
      Integer colIndex = headerMap.get(colConfig.name());
      if (colIndex == null) {
        // Column missing in Excel -> Invalid row
        // Already captured in validateSheet structural check, but mark row invalid
        isRowValid = false;
        continue;
      }

      String transformedValue = rowValues.get(colConfig.name());

      // Validation with row context (for database lookups)
      List<String> errors =
          cellValueValidator.validateWithRowContext(
              colConfig.name(), transformedValue, rowValues, colConfig);
      if (!errors.isEmpty()) {
        isRowValid = false;
        for (String error : errors) {
          sheetErrors.add(
              String.format(
                  "Row %d, Col '%s': %s (value: '%s')",
                  rowNum, colConfig.name(), error, transformedValue));
        }
      } else {
        rowData.put(colConfig.name(), convertValue(transformedValue, colConfig.type()));
      }
    }
    return isRowValid;
  }

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
