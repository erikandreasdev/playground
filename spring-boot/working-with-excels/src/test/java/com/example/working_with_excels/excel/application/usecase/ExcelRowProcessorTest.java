package com.example.working_with_excels.excel.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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
                            DbColumnMapping.of("user_name")),
                    new ColumnConfig("Age", ExcelColumnType.INTEGER, null, null,
                            DbColumnMapping.of("age")));

            when(cellValidator.validate(any(), any())).thenReturn(null);
            when(cellValidator.validateTransformedValue(any(), any())).thenReturn(null);
            when(cellValueExtractor.extractTypedValue(eq(nameCell), any())).thenReturn("Alice");
            when(cellValueExtractor.extractTypedValue(eq(ageCell), any())).thenReturn(30);

            // Act
            RowProcessingResult result = rowProcessor.processRow(row, 2, columns);

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
                            DbColumnMapping.of("user_name")),
                    new ColumnConfig("Notes", ExcelColumnType.STRING, null, null, null)); // No mapping

            when(cellValidator.validate(any(), any())).thenReturn(null);
            when(cellValidator.validateTransformedValue(any(), any())).thenReturn(null);
            when(cellValueExtractor.extractTypedValue(eq(nameCell), any())).thenReturn("Bob");

            // Act
            RowProcessingResult result = rowProcessor.processRow(row, 2, columns);

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
                            DbColumnMapping.of("email")));

            when(cellValidator.validate(any(), any())).thenReturn("Invalid email format");

            // Act
            RowProcessingResult result = rowProcessor.processRow(row, 2, columns);

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
                            DbColumnMapping.of("col1")),
                    new ColumnConfig("Col2", ExcelColumnType.STRING, null, null,
                            DbColumnMapping.of("col2")));

            when(cellValidator.validate(any(), any()))
                    .thenReturn("Error 1")
                    .thenReturn("Error 2");

            // Act
            RowProcessingResult result = rowProcessor.processRow(row, 2, columns);

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
                    new ColumnConfig("Country", ExcelColumnType.STRING, null, null, mapping));

            when(cellValidator.validate(any(), any())).thenReturn(null);
            when(cellValidator.validateTransformedValue(any(), any())).thenReturn(null);
            when(cellValueExtractor.extractTypedValue(any(), any())).thenReturn("USA");
            when(databasePort.lookup("COUNTRIES", "CODE", "USA", "ID"))
                    .thenReturn(Optional.of(42));

            // Act
            RowProcessingResult result = rowProcessor.processRow(row, 2, columns);

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
                    new ColumnConfig("Country", ExcelColumnType.STRING, null, null, mapping));

            when(cellValidator.validate(any(), any())).thenReturn(null);
            when(cellValidator.validateTransformedValue(any(), any())).thenReturn(null);
            when(cellValueExtractor.extractTypedValue(any(), any())).thenReturn("INVALID");
            when(databasePort.lookup(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(Optional.empty());

            // Act
            RowProcessingResult result = rowProcessor.processRow(row, 2, columns);

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
                            DbColumnMapping.of("name")));

            when(cellValidator.validate(any(), any())).thenReturn(null);
            when(cellValidator.validateTransformedValue(any(), any())).thenReturn(null);
            when(cellValueExtractor.extractTypedValue(any(), any())).thenReturn("Value");

            // Act
            rowProcessor.processRow(row, 2, columns);

            // Assert
            verify(databasePort, never()).lookup(any(), any(), any(), any());
        }
    }
}
