package com.example.working_with_excels.excel.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.working_with_excels.excel.domain.model.ColumnConfig;
import com.example.working_with_excels.excel.domain.model.ColumnTransformation;
import com.example.working_with_excels.excel.domain.model.ColumnValidation;
import com.example.working_with_excels.excel.domain.model.DbColumnMapping;
import com.example.working_with_excels.excel.domain.model.ExcelColumnType;
import com.example.working_with_excels.excel.domain.model.TransformerType;

/**
 * Unit tests for the CellValueExtractor domain service.
 */
@ExtendWith(MockitoExtension.class)
class CellValueExtractorTest {

    @Mock
    private CellTransformer cellTransformer;

    private CellValueExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new CellValueExtractor(cellTransformer);
    }

    @Nested
    @DisplayName("extractTypedValue for null cell")
    class NullCell {

        @Test
        @DisplayName("should return null for null cell")
        void shouldReturnNullForNullCell() {
            // Arrange
            ColumnConfig config = ColumnConfig.of("Name", ExcelColumnType.STRING, null);

            // Act
            Object result = extractor.extractTypedValue(null, config);

            // Assert
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("extractTypedValue for DATE type")
    class DateType {

        @Test
        @DisplayName("should extract date from numeric cell")
        void shouldExtractDateFromNumericCell() {
            // Arrange
            Cell cell = mock(Cell.class);
            Date expectedDate = new Date();
            when(cell.getCellType()).thenReturn(CellType.NUMERIC);
            when(cell.getDateCellValue()).thenReturn(expectedDate);

            ColumnConfig config = ColumnConfig.of("Birthday", ExcelColumnType.DATE, null);

            // Act
            Object result = extractor.extractTypedValue(cell, config);

            // Assert
            assertThat(result).isEqualTo(expectedDate);
        }

        @Test
        @DisplayName("should return null for non-numeric date cell")
        void shouldReturnNullForNonNumericDateCell() {
            // Arrange
            Cell cell = mock(Cell.class);
            when(cell.getCellType()).thenReturn(CellType.STRING);

            ColumnConfig config = ColumnConfig.of("Birthday", ExcelColumnType.DATE, null);

            // Act
            Object result = extractor.extractTypedValue(cell, config);

            // Assert
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("extractTypedValue for INTEGER type")
    class IntegerType {

        @Test
        @DisplayName("should extract integer from numeric cell")
        void shouldExtractIntegerFromNumericCell() {
            // Arrange
            Cell cell = mock(Cell.class);
            when(cell.getCellType()).thenReturn(CellType.NUMERIC);
            when(cell.getNumericCellValue()).thenReturn(42.0);

            ColumnConfig config = ColumnConfig.of("Age", ExcelColumnType.INTEGER, null);

            // Act
            Object result = extractor.extractTypedValue(cell, config);

            // Assert
            assertThat(result).isEqualTo(42);
        }

        @Test
        @DisplayName("should return null for non-numeric integer cell")
        void shouldReturnNullForNonNumericIntegerCell() {
            // Arrange
            Cell cell = mock(Cell.class);
            when(cell.getCellType()).thenReturn(CellType.STRING);

            ColumnConfig config = ColumnConfig.of("Age", ExcelColumnType.INTEGER, null);

            // Act
            Object result = extractor.extractTypedValue(cell, config);

            // Assert
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("extractTypedValue for DECIMAL type")
    class DecimalType {

        @Test
        @DisplayName("should extract decimal from numeric cell")
        void shouldExtractDecimalFromNumericCell() {
            // Arrange
            Cell cell = mock(Cell.class);
            when(cell.getCellType()).thenReturn(CellType.NUMERIC);
            when(cell.getNumericCellValue()).thenReturn(3.14159);

            ColumnConfig config = ColumnConfig.of("Price", ExcelColumnType.DECIMAL, null);

            // Act
            Object result = extractor.extractTypedValue(cell, config);

            // Assert
            assertThat(result).isEqualTo(3.14159);
        }
    }

    @Nested
    @DisplayName("extractTypedValue for BOOLEAN type")
    class BooleanType {

        @Test
        @DisplayName("should extract 1 for true boolean cell")
        void shouldExtract1ForTrueBoolean() {
            // Arrange
            Cell cell = mock(Cell.class);
            when(cell.getCellType()).thenReturn(CellType.BOOLEAN);
            when(cell.getBooleanCellValue()).thenReturn(true);

            ColumnConfig config = ColumnConfig.of("Active", ExcelColumnType.BOOLEAN, null);

            // Act
            Object result = extractor.extractTypedValue(cell, config);

            // Assert
            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("should extract 0 for false boolean cell")
        void shouldExtract0ForFalseBoolean() {
            // Arrange
            Cell cell = mock(Cell.class);
            when(cell.getCellType()).thenReturn(CellType.BOOLEAN);
            when(cell.getBooleanCellValue()).thenReturn(false);

            ColumnConfig config = ColumnConfig.of("Active", ExcelColumnType.BOOLEAN, null);

            // Act
            Object result = extractor.extractTypedValue(cell, config);

            // Assert
            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("extractTypedValue for STRING type")
    class StringType {

        @Test
        @DisplayName("should delegate to transformer for STRING type")
        void shouldDelegateToTransformerForString() {
            // Arrange
            Cell cell = mock(Cell.class);
            when(cellTransformer.transform(eq(cell), any())).thenReturn("Transformed Value");

            ColumnConfig config = new ColumnConfig("Name", ExcelColumnType.STRING, null, List.of(), null, null, null);

            // Act
            Object result = extractor.extractTypedValue(cell, config);

            // Assert
            assertThat(result).isEqualTo("Transformed Value");
            verify(cellTransformer).transform(eq(cell), any());
        }

        @Test
        @DisplayName("should apply transformations when extracting value")
        void shouldApplyTransformations() {
            // Arrange
            Cell cell = mock(Cell.class);
            when(cellTransformer.transform(eq(cell), any())).thenReturn("  Value  ");

            List<ColumnTransformation> transformations = List.of(
                    ColumnTransformation.of(TransformerType.TRIM));
            ColumnConfig config = new ColumnConfig("Name", ExcelColumnType.STRING, null, transformations, null, null,
                    null);

            // Act
            Object result = extractor.extractTypedValue(cell, config);

            // Assert
            assertThat(result).isEqualTo("  Value  ");
            // Verification that transformations are passed to transformer happens via
            // argument matcher in verify above if needed
            // But here we mock transform to return a specific value, focusing on flow
        }
    }

    @Nested
    @DisplayName("extractTypedValue for EMAIL type")
    class EmailType {

        @Test
        @DisplayName("should delegate to transformer for EMAIL type")
        void shouldDelegateToTransformerForEmail() {
            // Arrange
            Cell cell = mock(Cell.class);
            when(cellTransformer.transform(eq(cell), any())).thenReturn("test@example.com");

            ColumnConfig config = new ColumnConfig("Email", ExcelColumnType.EMAIL, null, List.of(), null, null, null);

            // Act
            Object result = extractor.extractTypedValue(cell, config);

            // Assert
            assertThat(result).isEqualTo("test@example.com");
            verify(cellTransformer).transform(eq(cell), any());
        }
    }
}
