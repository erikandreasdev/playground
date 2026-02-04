package com.example.demo.service;

import com.example.demo.domain.FileMetadata;
import com.example.demo.domain.SheetConfig;
import com.example.demo.domain.SheetMetrics;
import com.example.demo.domain.ValidationConfig;
import com.example.demo.domain.ValidationMetrics;
import com.example.demo.domain.ValidationReport;
import com.example.demo.domain.ValidationStatus;
import com.example.demo.resource.LoadedResource;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

/**
 * Service to validate Excel files against configuration.
 *
 * <p>Orchestrates the validation process:
 *
 * <ol>
 *   <li>STEP 1: Load YAML configuration
 *   <li>STEP 2: Open Excel workbook
 *   <li>STEP 3: Process each sheet via ExcelSheetProcessor
 *   <li>STEP 4: Aggregate metrics and build ValidationReport
 * </ol>
 */
@Service
public class ExcelValidationService {

  private final ValidationConfigLoader configLoader;
  private final ExcelSheetProcessor sheetProcessor;

  /**
   * Constructs the service.
   *
   * @param configLoader loader for YAML configuration
   * @param sheetProcessor processor for individual sheets
   */
  public ExcelValidationService(
      ValidationConfigLoader configLoader, ExcelSheetProcessor sheetProcessor) {
    this.configLoader = configLoader;
    this.sheetProcessor = sheetProcessor;
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

    // STEP 1: Load configuration
    ValidationConfig config = configLoader.loadConfig(configResource);

    List<String> globalErrors = new ArrayList<>();
    List<SheetMetrics> sheetMetricsList = new ArrayList<>();
    AtomicInteger globalTotal = new AtomicInteger(0);
    AtomicInteger globalValid = new AtomicInteger(0);
    AtomicInteger globalInvalid = new AtomicInteger(0);
    AtomicInteger globalPersisted = new AtomicInteger(0);

    // STEP 2: Open Excel workbook
    try (InputStream excelIs = excel.resource().getInputStream();
        Workbook workbook = WorkbookFactory.create(excelIs)) {

      // STEP 3: Process each sheet
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

        SheetMetrics metrics = sheetProcessor.processSheet(sheet, sheetConfig, persist);
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

    // STEP 4: Build validation report
    return buildValidationReport(
        excel,
        configResource,
        globalTotal.get(),
        globalValid.get(),
        globalInvalid.get(),
        globalPersisted.get(),
        sheetMetricsList,
        globalErrors);
  }

  /**
   * Builds the final validation report.
   *
   * @param excel Excel resource
   * @param configResource configuration resource
   * @param totalRows total rows processed
   * @param validRows valid rows count
   * @param invalidRows invalid rows count
   * @param persistedRows persisted rows count
   * @param sheetMetricsList metrics per sheet
   * @param globalErrors global validation errors
   * @return validation report
   */
  private ValidationReport buildValidationReport(
      LoadedResource excel,
      LoadedResource configResource,
      int totalRows,
      int validRows,
      int invalidRows,
      int persistedRows,
      List<SheetMetrics> sheetMetricsList,
      List<String> globalErrors) {

    ValidationMetrics globalMetrics =
        new ValidationMetrics(totalRows, validRows, invalidRows, persistedRows);

    ValidationStatus status =
        (invalidRows == 0 && globalErrors.isEmpty())
            ? ValidationStatus.SUCCESS
            : ValidationStatus.FAILED;

    if (validRows > 0 && (invalidRows > 0 || !globalErrors.isEmpty())) {
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
}
