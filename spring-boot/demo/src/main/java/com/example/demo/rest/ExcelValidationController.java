package com.example.demo.rest;

import com.example.demo.domain.ValidationReport;
import com.example.demo.resource.LoadedResource;
import com.example.demo.resource.ResourceLoaderService;
import com.example.demo.service.ExcelValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for Excel validation endpoints. */
@RestController
@RequestMapping("/api/excel")
public class ExcelValidationController {

  private final ResourceLoaderService resourceLoaderService;
  private final ExcelValidationService excelValidationService;

  /**
   * Constructs the controller.
   *
   * @param resourceLoaderService service to load resources
   * @param excelValidationService service to validate excel files
   */
  public ExcelValidationController(
      ResourceLoaderService resourceLoaderService, ExcelValidationService excelValidationService) {
    this.resourceLoaderService = resourceLoaderService;
    this.excelValidationService = excelValidationService;
  }

  /**
   * Validates an Excel file against a configuration file (GET with query params).
   *
   * @param excelFilename name/path of the excel file
   * @param validationsFilename name/path of the validation config file
   * @param persist whether to persist valid rows to database (defaults to false)
   * @return the validation report
   */
  @org.springframework.web.bind.annotation.GetMapping("/validate")
  public ResponseEntity<ValidationReport> validateGet(
      @org.springframework.web.bind.annotation.RequestParam String excelFilename,
      @org.springframework.web.bind.annotation.RequestParam String validationsFilename,
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false")
          boolean persist) {
    LoadedResource excelResource = resourceLoaderService.loadResource(excelFilename);
    LoadedResource configResource = resourceLoaderService.loadResource(validationsFilename);

    ValidationReport report =
        excelValidationService.validate(excelResource, configResource, persist);
    return ResponseEntity.ok(report);
  }

  /**
   * Validates an Excel file against a configuration file (POST with JSON body).
   *
   * @param request the validation request containing file names
   * @return the validation report
   */
  @PostMapping("/validate")
  public ResponseEntity<ValidationReport> validatePost(@RequestBody ValidationRequest request) {
    LoadedResource excelResource = resourceLoaderService.loadResource(request.excelFilename());
    LoadedResource configResource =
        resourceLoaderService.loadResource(request.validationsFilename());

    ValidationReport report =
        excelValidationService.validate(excelResource, configResource, request.persist());
    return ResponseEntity.ok(report);
  }
}
