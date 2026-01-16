package com.example.working_with_excels.excel.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.working_with_excels.excel.application.dto.RowProcessingResult;
import com.example.working_with_excels.excel.application.port.output.DatabasePort;
import com.example.working_with_excels.excel.domain.model.ColumnConfig;
import com.example.working_with_excels.excel.domain.model.DbColumnMapping;
import com.example.working_with_excels.excel.domain.model.ExcelColumnType;
import com.example.working_with_excels.excel.domain.model.LookupConfig;
import com.example.working_with_excels.excel.domain.service.CellValidator;
import com.example.working_with_excels.excel.domain.service.CellValueExtractor;

/**
 * Unit tests for the ExcelRowProcessor service.
 */
@ExtendWith(MockitoExtension.class)
class ExcelRowProcessorTest {

        @Mock
        private CellValidator cellValidator;

        @Mock
        private CellValueExtractor cellValueExtractor;

        @Mock
        private DatabasePort databasePort;

        @InjectMocks
        private ExcelRowProcessor rowProcessor;

        @Nested
        @DisplayName("processRow with valid data")
        class ValidData {

                @Test
                @DisplayName("should extract values for all mapped columns")
                void shouldExtractValuesForMappedColumns() {
                        // Arrange
                        Row row = mock(Row.class);
                        Cell nameCell = mock(Cell.class);
                        Cell ageCell = mock(Cell.class);

                        when(row.getCell(0)).thenReturn(nameCell);
                        when(row.getCell(1)).thenReturn(ageCell);

                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("Name", ExcelColumnType.STRING, null, null,
                                                        DbColumnMapping.of("user_name"), null, null),
                                        new ColumnConfig("Age", ExcelColumnType.INTEGER, null, null,
                                                        DbColumnMapping.of("age"), null, null));

                        when(cellValidator.validate(any(), any())).thenReturn(null);
                        when(cellValidator.validateTransformedValue(any(), any())).thenReturn(null);
                        when(cellValueExtractor.extractTypedValue(eq(nameCell), any())).thenReturn("Alice");
                        when(cellValueExtractor.extractTypedValue(eq(ageCell), any())).thenReturn(30);

                        // Act
                        RowProcessingResult result = rowProcessor.processRow(row, 2, createSheetConfig(columns));

                        // Assert
                        assertThat(result.isValid()).isTrue();
                        assertThat(result.namedParams()).containsEntry("user_name", "Alice");
                        assertThat(result.namedParams()).containsEntry("age", 30);
                        assertThat(result.errors()).isEmpty();
                }

                @Test
                @DisplayName("should skip columns without database mapping")
                void shouldSkipColumnsWithoutMapping() {
                        // Arrange
                        Row row = mock(Row.class);
                        Cell nameCell = mock(Cell.class);
                        Cell notesCell = mock(Cell.class);

                        when(row.getCell(0)).thenReturn(nameCell);
                        when(row.getCell(1)).thenReturn(notesCell);

                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("Name", ExcelColumnType.STRING, null, null,
                                                        DbColumnMapping.of("user_name"), null, null),
                                        new ColumnConfig("Notes", ExcelColumnType.STRING, null, null, null, null,
                                                        null)); // No
                                                                // mapping

                        when(cellValidator.validate(any(), any())).thenReturn(null);
                        when(cellValidator.validateTransformedValue(any(), any())).thenReturn(null);
                        when(cellValueExtractor.extractTypedValue(eq(nameCell), any())).thenReturn("Bob");

                        // Act
                        RowProcessingResult result = rowProcessor.processRow(row, 2, createSheetConfig(columns));

                        // Assert
                        assertThat(result.isValid()).isTrue();
                        assertThat(result.namedParams()).containsOnlyKeys("user_name");
                }
        }

        @Nested
        @DisplayName("processRow with validation errors")
        class ValidationErrors {

                @Test
                @DisplayName("should return invalid result when validation fails")
                void shouldReturnInvalidWhenValidationFails() {
                        // Arrange
                        Row row = mock(Row.class);
                        Cell cell = mock(Cell.class);
                        when(row.getCell(0)).thenReturn(cell);

                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("Email", ExcelColumnType.EMAIL, null, null,
                                                        DbColumnMapping.of("email"), null, null));

                        when(cellValidator.validate(any(), any())).thenReturn("Invalid email format");

                        // Act
                        RowProcessingResult result = rowProcessor.processRow(row, 2, createSheetConfig(columns));

                        // Assert
                        assertThat(result.isValid()).isFalse();
                        assertThat(result.errors()).hasSize(1);
                        assertThat(result.errors().getFirst().errorMessage()).contains("Invalid email format");
                }

                @Test
                @DisplayName("should collect multiple validation errors")
                void shouldCollectMultipleErrors() {
                        // Arrange
                        Row row = mock(Row.class);
                        Cell cell1 = mock(Cell.class);
                        Cell cell2 = mock(Cell.class);
                        when(row.getCell(0)).thenReturn(cell1);
                        when(row.getCell(1)).thenReturn(cell2);

                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("Col1", ExcelColumnType.STRING, null, null,
                                                        DbColumnMapping.of("col1"), null, null),
                                        new ColumnConfig("Col2", ExcelColumnType.STRING, null, null,
                                                        DbColumnMapping.of("col2"), null, null));

                        when(cellValidator.validate(any(), any()))
                                        .thenReturn("Error 1")
                                        .thenReturn("Error 2");

                        // Act
                        RowProcessingResult result = rowProcessor.processRow(row, 2, createSheetConfig(columns));

                        // Assert
                        assertThat(result.isValid()).isFalse();
                        assertThat(result.errors()).hasSize(2);
                }
        }

        @Nested
        @DisplayName("processRow with lookups")
        class Lookups {

                @Test
                @DisplayName("should resolve lookup values")
                void shouldResolveLookupValues() {
                        // Arrange
                        Row row = mock(Row.class);
                        Cell countryCell = mock(Cell.class);
                        when(row.getCell(0)).thenReturn(countryCell);

                        LookupConfig lookup = new LookupConfig("COUNTRIES", "CODE", "ID");
                        DbColumnMapping mapping = new DbColumnMapping("country_id", null, lookup);
                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("Country", ExcelColumnType.STRING, null, null, mapping, null,
                                                        null));

                        when(cellValidator.validate(any(), any())).thenReturn(null);
                        when(cellValidator.validateTransformedValue(any(), any())).thenReturn(null);
                        when(cellValueExtractor.extractTypedValue(any(), any())).thenReturn("USA");
                        when(databasePort.lookup("COUNTRIES", "CODE", "USA", "ID"))
                                        .thenReturn(Optional.of(42));

                        // Act
                        RowProcessingResult result = rowProcessor.processRow(row, 2, createSheetConfig(columns));

                        // Assert
                        assertThat(result.isValid()).isTrue();
                        assertThat(result.namedParams()).containsEntry("country_id", 42);
                }

                @Test
                @DisplayName("should add error when lookup fails")
                void shouldAddErrorWhenLookupFails() {
                        // Arrange
                        Row row = mock(Row.class);
                        Cell countryCell = mock(Cell.class);
                        when(row.getCell(0)).thenReturn(countryCell);

                        LookupConfig lookup = new LookupConfig("COUNTRIES", "CODE", "ID");
                        DbColumnMapping mapping = new DbColumnMapping("country_id", null, lookup);
                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("Country", ExcelColumnType.STRING, null, null, mapping, null,
                                                        null));

                        when(cellValidator.validate(any(), any())).thenReturn(null);
                        when(cellValidator.validateTransformedValue(any(), any())).thenReturn(null);
                        when(cellValueExtractor.extractTypedValue(any(), any())).thenReturn("INVALID");
                        when(databasePort.lookup(anyString(), anyString(), anyString(), anyString()))
                                        .thenReturn(Optional.empty());

                        // Act
                        RowProcessingResult result = rowProcessor.processRow(row, 2, createSheetConfig(columns));

                        // Assert
                        assertThat(result.isValid()).isFalse();
                        assertThat(result.errors()).hasSize(1);
                        assertThat(result.errors().getFirst().errorMessage()).contains("Lookup failed");
                }

                @Test
                @DisplayName("should not call lookup when no lookup configured")
                void shouldNotCallLookupWhenNotConfigured() {
                        // Arrange
                        Row row = mock(Row.class);
                        Cell cell = mock(Cell.class);
                        when(row.getCell(0)).thenReturn(cell);

                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("Name", ExcelColumnType.STRING, null, null,
                                                        DbColumnMapping.of("name"), null, null));

                        when(cellValidator.validate(any(), any())).thenReturn(null);
                        when(cellValidator.validateTransformedValue(any(), any())).thenReturn(null);
                        when(cellValueExtractor.extractTypedValue(any(), any())).thenReturn("Value");

                        // Act
                        rowProcessor.processRow(row, 2, createSheetConfig(columns));

                        // Assert
                        verify(databasePort, never()).lookup(any(), any(), any(), any());
                }
        }

        @Nested
        @DisplayName("processRow with skip conditions")
        class SkippingLogic {

                @Test
                @DisplayName("should skip row when value matches skip condition")
                void shouldSkipRowWhenValueMatches() {
                        // Arrange
                        Row row = mock(Row.class);
                        Cell statusCell = mock(Cell.class);
                        when(row.getCell(0)).thenReturn(statusCell);

                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("Status", ExcelColumnType.STRING, null, null,
                                                        DbColumnMapping.of("status"),
                                                        List.of("Inactive", "Deleted"), null));

                        when(cellValueExtractor.extractTypedValue(eq(statusCell), any()))
                                        .thenReturn("Inactive");

                        // Act
                        RowProcessingResult result = rowProcessor.processRow(row, 2, createSheetConfig(columns));

                        // Assert
                        assertThat(result.skipped()).isTrue();
                }

                @Test
                @DisplayName("should not skip row when value does not match skip condition")
                void shouldNotSkipRowWhenValueDoesNotMatch() {
                        // Arrange
                        Row row = mock(Row.class);
                        Cell statusCell = mock(Cell.class);
                        when(row.getCell(0)).thenReturn(statusCell);

                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("Status", ExcelColumnType.STRING, null, null,
                                                        DbColumnMapping.of("status"), List.of("Inactive"),
                                                        null));

                        when(cellValidator.validate(any(), any())).thenReturn(null);
                        when(cellValidator.validateTransformedValue(any(), any())).thenReturn(null);
                        when(cellValueExtractor.extractTypedValue(eq(statusCell), any())).thenReturn("Active");

                        // Act
                        RowProcessingResult result = rowProcessor.processRow(row, 2, createSheetConfig(columns));

                        // Assert
                        assertThat(result.skipped()).isFalse();
                }

                @Test
                @DisplayName("should skip row when column SpEL expression evaluates to true")
                void shouldSkipRowWhenColumnSpelExpressionIsTrue() {
                        // Arrange
                        Row row = mock(Row.class);
                        Cell dateCell = mock(Cell.class);
                        when(row.getCell(0)).thenReturn(dateCell);

                        // Expression: Skip if date is after now (future date)
                        String expression = "#root.isAfter(#dateTime.now())";

                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("Date", ExcelColumnType.DATE, null, null,
                                                        DbColumnMapping.of("date"), null, expression));

                        when(cellValueExtractor.extractTypedValue(eq(dateCell), any()))
                                        .thenReturn(java.time.LocalDate.now().plusDays(1));

                        // Act
                        RowProcessingResult result = rowProcessor.processRow(row, 2, createSheetConfig(columns));

                        // Assert
                        assertThat(result.skipped()).isTrue();
                }

                @Test
                @DisplayName("should skip row when Sheet SpEL expression evaluates to true using multiple columns and DB")
                void shouldSkipRowWhenSheetSpelExpressionIsTrue() {
                        // Arrange
                        Row row = mock(Row.class);
                        Cell colACell = mock(Cell.class);
                        Cell colBCell = mock(Cell.class);
                        when(row.getCell(0)).thenReturn(colACell);
                        when(row.getCell(1)).thenReturn(colBCell);

                        List<ColumnConfig> columns = List.of(
                                        new ColumnConfig("ColA", ExcelColumnType.STRING, null, null,
                                                        DbColumnMapping.of("col_a"), null, null),
                                        new ColumnConfig("ColB", ExcelColumnType.INTEGER, null, null,
                                                        DbColumnMapping.of("col_b"), null, null));

                        when(cellValueExtractor.extractTypedValue(eq(colACell), any())).thenReturn("Special");
                        when(cellValueExtractor.extractTypedValue(eq(colBCell), any())).thenReturn(100);

                        when(databasePort.lookup("EXISTING_TABLE", "KEY", "Special", "KEY"))
                                        .thenReturn(Optional.of("Special"));

                        // Expression: Skip if ColA == 'Special' AND ColB > 50 AND row exists in DB
                        String expression = "#ColA == 'Special' && #ColB > 50 && #db.exists('EXISTING_TABLE', 'KEY', #ColA)";

                        // Act
                        RowProcessingResult result = rowProcessor.processRow(row, 2,
                                        createSheetConfigWithSkip(columns, expression));

                        // Assert
                        assertThat(result.skipped()).isTrue();
                }
        }

        private com.example.working_with_excels.excel.domain.model.SheetConfig createSheetConfig(
                        List<ColumnConfig> columns) {
                return new com.example.working_with_excels.excel.domain.model.SheetConfig("Sheet", columns, "Table",
                                null, null, null, null, null);
        }

        private com.example.working_with_excels.excel.domain.model.SheetConfig createSheetConfigWithSkip(
                        List<ColumnConfig> columns, String skipExpression) {
                return new com.example.working_with_excels.excel.domain.model.SheetConfig("Sheet", columns, "Table",
                                null, null, null, skipExpression, null);
        }
}
