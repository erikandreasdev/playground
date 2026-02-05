package com.example.demo.core.internal.usecases;

import com.example.demo.core.internal.domain.config.SheetConfig;
import com.example.demo.core.internal.domain.config.ValidationConfig;
import com.example.demo.core.internal.domain.enums.ValidationStatus;
import com.example.demo.core.internal.domain.result.FileMetadata;
import com.example.demo.core.internal.domain.result.SheetMetrics;
import com.example.demo.core.internal.domain.result.ValidationMetrics;
import com.example.demo.core.internal.domain.result.ValidationReport;
import com.example.demo.core.internal.services.ExcelSheetProcessorService;
import com.example.demo.core.internal.valueobjects.LoadedResource;
import com.example.demo.core.ports.inbound.ExcelValidationPort;
import com.example.demo.core.ports.outbound.ConfigLoaderPort;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Use case for validating Excel files against configuration.
 *
 * <p>This use case implements the inbound port and orchestrates the validation process:
 *
 * <ol>
 *   <li>STEP 1: Load YAML configuration via ConfigLoaderPort
 *   <li>STEP 2: Open Excel workbook
 *   <li>STEP 3: Process each sheet via ExcelSheetProcessorService
 *   <li>STEP 4: Aggregate metrics and build ValidationReport
 * </ol>
 */
public class ValidateExcelUseCase implements ExcelValidationPort {

  private final ConfigLoaderPort configLoader;
  private final ExcelSheetProcessorService sheetProcessor;

  /**
   * Constructs the use case.
   *
   * @param configLoader port for loading configuration
   * @param sheetProcessor service for processing sheets
   */
  public ValidateExcelUseCase(
      ConfigLoaderPort configLoader, ExcelSheetProcessorService sheetProcessor) {
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
  @Override
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
