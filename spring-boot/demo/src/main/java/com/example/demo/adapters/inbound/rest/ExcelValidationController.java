package com.example.demo.adapters.inbound.rest;

import com.example.demo.core.internal.domain.result.ValidationReport;
import com.example.demo.core.internal.valueobjects.LoadedResource;
import com.example.demo.core.ports.inbound.ExcelValidationPort;
import com.example.demo.core.ports.outbound.ResourceLoaderPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller for Excel validation endpoints. */
@RestController
@RequestMapping("/api/excel")
public class ExcelValidationController {

  private final ResourceLoaderPort resourceLoaderPort;
  private final ExcelValidationPort excelValidationPort;

  /**
   * Constructs the controller.
   *
   * @param resourceLoaderPort port to load resources
   * @param excelValidationPort port for validation use case
   */
  public ExcelValidationController(
      ResourceLoaderPort resourceLoaderPort, ExcelValidationPort excelValidationPort) {
    this.resourceLoaderPort = resourceLoaderPort;
    this.excelValidationPort = excelValidationPort;
  }

  /**
   * Validates an Excel file against a configuration file (GET with query params).
   *
   * @param excelFilename name/path of the excel file
   * @param validationsFilename name/path of the validation config file
   * @param persist whether to persist valid rows to database (defaults to false)
   * @return the validation report
   */
  @GetMapping("/validate")
  public ResponseEntity<ValidationReport> validateGet(
      @RequestParam String excelFilename,
      @RequestParam String validationsFilename,
      @RequestParam(defaultValue = "false") boolean persist) {
    LoadedResource excelResource = resourceLoaderPort.loadResource(excelFilename);
    LoadedResource configResource = resourceLoaderPort.loadResource(validationsFilename);

    ValidationReport report =
        excelValidationPort.validate(excelResource, configResource, persist);
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
    LoadedResource excelResource = resourceLoaderPort.loadResource(request.excelFilename());
    LoadedResource configResource =
        resourceLoaderPort.loadResource(request.validationsFilename());

    ValidationReport report =
        excelValidationPort.validate(excelResource, configResource, request.persist());
    return ResponseEntity.ok(report);
  }
}
