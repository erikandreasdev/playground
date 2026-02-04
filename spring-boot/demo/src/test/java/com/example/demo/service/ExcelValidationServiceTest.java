package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.domain.PersistenceResult;
import com.example.demo.domain.SheetConfig;
import com.example.demo.domain.SheetMetrics;
import com.example.demo.domain.ValidationConfig;
import com.example.demo.domain.ValidationReport;
import com.example.demo.domain.ValidationStatus;
import com.example.demo.resource.LoadedResource;
import com.example.demo.resource.SourceType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

@ExtendWith(MockitoExtension.class)
class ExcelValidationServiceTest {

  @Mock private ValidationConfigLoader configLoader;
  @Mock private ExcelSheetProcessor sheetProcessor;

  private ExcelValidationService excelValidationService;

  @BeforeEach
  void setUp() {
    excelValidationService = new ExcelValidationService(configLoader, sheetProcessor);
  }

  @Test
  void validate_shouldLoadConfigAndProcessSheets() throws IOException {
    // Arrange
    byte[] excelBytes = createExcelWithHeader("Main", "ID", "Name");
    LoadedResource excel =
        new LoadedResource(
            "test.xlsx",
            excelBytes.length,
            SourceType.FILESYSTEM,
            MediaType.APPLICATION_OCTET_STREAM,
            new ByteArrayResource(excelBytes));

    String yaml = """
        sheets:
          - name: "Main"
        """;
    LoadedResource config =
        new LoadedResource(
            "config.yml",
            yaml.length(),
            SourceType.FILESYSTEM,
            MediaType.TEXT_PLAIN,
            new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));

    ValidationConfig validationConfig =
        new ValidationConfig("", List.of(new SheetConfig("Main", List.of(), null, null)));
    when(configLoader.loadConfig(config)).thenReturn(validationConfig);

    SheetMetrics metrics =
        new SheetMetrics("Main", 0, 0, 0, List.of(), PersistenceResult.success(0, "No data"));
    when(sheetProcessor.processSheet(any(Sheet.class), any(SheetConfig.class), anyBoolean()))
        .thenReturn(metrics);

    // Act
    ValidationReport report = excelValidationService.validate(excel, config, false);

    // Assert
    assertThat(report.status()).isEqualTo(ValidationStatus.SUCCESS);
    assertThat(report.sheetMetrics()).hasSize(1);
    verify(configLoader).loadConfig(config);
    verify(sheetProcessor).processSheet(any(), any(), anyBoolean());
  }

  @Test
  void validate_shouldHandleMissingSheet() throws IOException {
    // Arrange
    byte[] excelBytes = createExcelWithHeader("Other", "ID");
    LoadedResource excel =
        new LoadedResource(
            "test.xlsx",
            excelBytes.length,
            SourceType.FILESYSTEM,
            MediaType.APPLICATION_OCTET_STREAM,
            new ByteArrayResource(excelBytes));

    String yaml = """
        sheets:
          - name: "Expected"
        """;
    LoadedResource config =
        new LoadedResource(
            "config.yml",
            yaml.length(),
            SourceType.FILESYSTEM,
            MediaType.TEXT_PLAIN,
            new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));

    ValidationConfig validationConfig =
        new ValidationConfig("", List.of(new SheetConfig("Expected", List.of(), null, null)));
    when(configLoader.loadConfig(config)).thenReturn(validationConfig);

    // Act
    ValidationReport report = excelValidationService.validate(excel, config, false);

    // Assert
    assertThat(report.status()).isEqualTo(ValidationStatus.FAILED);
    assertThat(report.sheetMetrics()).hasSize(1);
    assertThat(report.sheetMetrics().get(0).errors()).contains("Missing from workbook");
  }

  /** Helper to create Excel with headers. */
  private byte[] createExcelWithHeader(String sheetName, String... headers) throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet(sheetName);
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        headerRow.createCell(i).setCellValue(headers[i]);
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      workbook.write(out);
      return out.toByteArray();
    }
  }
}
