package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.domain.PersistenceResult;
import com.example.demo.domain.ValidationReport;
import com.example.demo.domain.ValidationStatus;
import com.example.demo.resource.LoadedResource;
import com.example.demo.resource.SourceType;
import com.example.demo.validation.CellTransformer;
import com.example.demo.validation.CellValueValidator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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

  @Mock private CellTransformer cellTransformer;
  @Mock private CellValueValidator cellValueValidator;
  @Mock private ExcelPersistenceService persistenceService;

  private ExcelValidationService excelValidationService;

  @BeforeEach
  void setUp() {
    excelValidationService =
        new ExcelValidationService(cellTransformer, cellValueValidator, persistenceService);
  }

  @Test
  void validate_shouldProcessSheetAndPersistRows() throws IOException {
    // Arrange
    byte[] excelBytes = createExcelWithHeader("Main", "ID", "Name");
    LoadedResource excel =
        new LoadedResource(
            "test.xlsx",
            excelBytes.length,
            SourceType.FILESYSTEM,
            MediaType.APPLICATION_OCTET_STREAM,
            new ByteArrayResource(excelBytes));

    String yaml =
        """
        sheets:
          - name: "Main"
            columns:
              - name: "ID"
                type: "NUMBER"
                required: true
              - name: "Name"
                type: "STRING"
                required: true
            persistence:
              tableName: "users"
        """;
    LoadedResource config =
        new LoadedResource(
            "config.yml",
            yaml.length(),
            SourceType.FILESYSTEM,
            MediaType.TEXT_PLAIN,
            new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));

    when(cellTransformer.apply(any(), any())).thenAnswer(i -> i.getArgument(0));
    when(cellValueValidator.validateWithRowContext(any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());
    when(persistenceService.persistValidRows(anyList(), any(), anyBoolean()))
        .thenReturn(PersistenceResult.success(1, "INSERT INTO users..."));

    // Act
    ValidationReport report = excelValidationService.validate(excel, config, true);

    // Assert
    assertThat(report.status()).isEqualTo(ValidationStatus.SUCCESS);
    assertThat(report.sheetMetrics()).hasSize(1);
    assertThat(report.globalMetrics().persistedRows()).isEqualTo(1);
    verify(persistenceService).persistValidRows(anyList(), any(), anyBoolean());
  }

  @Test
  void validate_shouldHandleMissingSheet() throws IOException {
    // Arrange
    byte[] excelBytes = createExcelWithHeader("Other", "ID", "Name");
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

    // Act
    ValidationReport report = excelValidationService.validate(excel, config, false);

    // Assert
    assertThat(report.status()).isEqualTo(ValidationStatus.FAILED);
    assertThat(report.errors()).contains("Sheet missing: Expected");
  }

  @Test
  void validate_shouldHandlePartialSuccess() throws IOException {
    // Arrange
    byte[] excelBytes =
        createExcelWithRows(
            "Main", new String[] {"ID", "Name"}, new String[] {"1", "A"}, new String[] {"2", "B"});
    LoadedResource excel =
        new LoadedResource(
            "test.xlsx",
            excelBytes.length,
            SourceType.FILESYSTEM,
            MediaType.APPLICATION_OCTET_STREAM,
            new ByteArrayResource(excelBytes));

    String yaml =
        """
        sheets:
          - name: "Main"
            columns:
              - name: "ID"
              - name: "Name"
        """;
    LoadedResource config =
        new LoadedResource(
            "config.yml",
            yaml.length(),
            SourceType.FILESYSTEM,
            MediaType.TEXT_PLAIN,
            new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));

    when(cellTransformer.apply(any(), any())).thenAnswer(i -> i.getArgument(0));
    // Row 1: ID valid, Name invalid -> Invalid
    // Row 2: ID valid, Name valid -> Valid
    when(cellValueValidator.validateWithRowContext(any(), any(), any(), any()))
        .thenReturn(Collections.emptyList()) // R1 C1
        .thenReturn(Collections.singletonList("Error")) // R1 C2
        .thenReturn(Collections.emptyList()) // R2 C1
        .thenReturn(Collections.emptyList()); // R2 C2

    // Act
    ValidationReport report = excelValidationService.validate(excel, config, false);

    // Assert
    assertThat(report.status()).isEqualTo(ValidationStatus.PARTIAL_SUCCESS);
    assertThat(report.globalMetrics().validRows()).isEqualTo(1);
    assertThat(report.globalMetrics().invalidRows()).isEqualTo(1);
  }

  @Test
  void validate_shouldThrowExceptionOnInvalidYaml() {
    // Arrange
    LoadedResource excel =
        new LoadedResource(
            "test.xlsx",
            0,
            SourceType.FILESYSTEM,
            MediaType.ALL,
            new ByteArrayResource(new byte[0]));

    String yaml = "invalid: : yaml";
    LoadedResource config =
        new LoadedResource(
            "config.yml",
            yaml.length(),
            SourceType.FILESYSTEM,
            MediaType.TEXT_PLAIN,
            new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));

    // Act & Assert
    org.junit.jupiter.api.Assertions.assertThrows(
        com.example.demo.exception.MappingException.class,
        () -> excelValidationService.validate(excel, config, false));
  }

  private byte[] createExcelWithRows(String sheetName, String[] headers, String[]... rows)
      throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(sheetName);
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        headerRow.createCell(i).setCellValue(headers[i]);
      }
      for (int r = 0; r < rows.length; r++) {
        Row dataRow = sheet.createRow(r + 1);
        for (int c = 0; c < rows[r].length; c++) {
          dataRow.createCell(c).setCellValue(rows[r][c]);
        }
      }
      workbook.write(bos);
      return bos.toByteArray();
    }
  }

  private byte[] createExcelWithHeader(String sheetName, String... headers) throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(sheetName);
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        headerRow.createCell(i).setCellValue(headers[i]);
      }
      Row dataRow = sheet.createRow(1);
      dataRow.createCell(0).setCellValue("1");
      dataRow.createCell(1).setCellValue("John Doe");

      workbook.write(bos);
      return bos.toByteArray();
    }
  }

  @Test
  void validate_shouldConvertNumericValuesForPersistence() throws IOException {
    // Arrange
    byte[] excelBytes =
        createExcelWithRows(
            "Data",
            new String[] {"Num", "Dec", "Birthday"},
            new String[] {"10.5", "20.123", "1990-01-15"});
    LoadedResource excel =
        new LoadedResource(
            "test.xlsx",
            excelBytes.length,
            SourceType.FILESYSTEM,
            MediaType.APPLICATION_OCTET_STREAM,
            new ByteArrayResource(excelBytes));

    String yaml =
        """
        sheets:
          - name: "Data"
            columns:
              - name: "Num"
                type: "NUMBER"
              - name: "Dec"
                type: "DECIMAL"
              - name: "Birthday"
                type: "DATE"
            persistence:
              tableName: "data_table"
        """;
    LoadedResource config =
        new LoadedResource(
            "config.yml",
            yaml.length(),
            SourceType.FILESYSTEM,
            MediaType.TEXT_PLAIN,
            new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));

    when(cellTransformer.apply(any(), any())).thenAnswer(i -> i.getArgument(0));
    when(cellValueValidator.validateWithRowContext(any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());

    // Act
    excelValidationService.validate(excel, config, true);

    // Assert
    org.mockito.ArgumentCaptor<java.util.List<java.util.Map<String, Object>>> captor =
        org.mockito.ArgumentCaptor.forClass(java.util.List.class);
    verify(persistenceService).persistValidRows(captor.capture(), any(), anyBoolean());

    java.util.List<java.util.Map<String, Object>> persistedRows = captor.getValue();
    assertThat(persistedRows).hasSize(1);
    java.util.Map<String, Object> row = persistedRows.get(0);

    assertThat(row.get("Num")).isInstanceOf(Double.class).isEqualTo(10.5);
    assertThat(row.get("Dec"))
        .isInstanceOf(java.math.BigDecimal.class)
        .isEqualTo(new java.math.BigDecimal("20.123"));
    assertThat(row.get("Birthday"))
        .isInstanceOf(java.sql.Date.class)
        .isEqualTo(java.sql.Date.valueOf("1990-01-15"));
  }
}
