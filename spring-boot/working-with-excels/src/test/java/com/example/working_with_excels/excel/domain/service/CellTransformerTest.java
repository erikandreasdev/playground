package com.example.working_with_excels.excel.domain.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.working_with_excels.excel.domain.model.ColumnTransformation;
import com.example.working_with_excels.excel.domain.model.TransformerType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CellTransformer}.
 */
class CellTransformerTest {

    private CellTransformer transformer;
    private Workbook workbook;
    private Sheet sheet;
    private Row row;

    @BeforeEach
    void setUp() {
        transformer = new CellTransformer();
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Test");
        row = sheet.createRow(0);
    }

    @Nested
    @DisplayName("Case Transformations")
    class CaseTransformations {

        @Test
        @DisplayName("UPPERCASE should convert text to uppercase")
        void uppercaseShouldConvertTextToUppercase() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("hello world");
            List<ColumnTransformation> transformations = List.of(ColumnTransformation.of(TransformerType.UPPERCASE));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("HELLO WORLD");
        }

        @Test
        @DisplayName("LOWERCASE should convert text to lowercase")
        void lowercaseShouldConvertTextToLowercase() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("HELLO WORLD");
            List<ColumnTransformation> transformations = List.of(ColumnTransformation.of(TransformerType.LOWERCASE));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("hello world");
        }

        @Test
        @DisplayName("TITLE_CASE should capitalize first letter of each word")
        void titleCaseShouldCapitalizeFirstLetterOfEachWord() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("hello world example");
            List<ColumnTransformation> transformations = List.of(ColumnTransformation.of(TransformerType.TITLE_CASE));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("Hello World Example");
        }

        @Test
        @DisplayName("SENTENCE_CASE should capitalize only first letter")
        void sentenceCaseShouldCapitalizeOnlyFirstLetter() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("HELLO WORLD");
            List<ColumnTransformation> transformations = List
                    .of(ColumnTransformation.of(TransformerType.SENTENCE_CASE));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("Hello world");
        }
    }

    @Nested
    @DisplayName("Whitespace Transformations")
    class WhitespaceTransformations {

        @Test
        @DisplayName("TRIM should remove leading and trailing whitespace")
        void trimShouldRemoveLeadingAndTrailingWhitespace() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("  hello world  ");
            List<ColumnTransformation> transformations = List.of(ColumnTransformation.of(TransformerType.TRIM));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("hello world");
        }

        @Test
        @DisplayName("REMOVE_WHITESPACE should remove all whitespace")
        void removeWhitespaceShouldRemoveAllWhitespace() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("hello world test");
            List<ColumnTransformation> transformations = List.of(
                    ColumnTransformation.of(TransformerType.REMOVE_WHITESPACE));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("helloworldtest");
        }

        @Test
        @DisplayName("NORMALIZE_SPACES should collapse multiple spaces into one")
        void normalizeSpacesShouldCollapseMultipleSpacesIntoOne() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("  hello   world  test  ");
            List<ColumnTransformation> transformations = List.of(
                    ColumnTransformation.of(TransformerType.NORMALIZE_SPACES));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("hello world test");
        }
    }

    @Nested
    @DisplayName("Padding Transformations")
    class PaddingTransformations {

        @Test
        @DisplayName("PAD_LEFT should pad value on the left")
        void padLeftShouldPadValueOnTheLeft() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("42");
            List<ColumnTransformation> transformations = List.of(
                    new ColumnTransformation(TransformerType.PAD_LEFT, null, null, null, 5, "0", null, null));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("00042");
        }

        @Test
        @DisplayName("PAD_RIGHT should pad value on the right")
        void padRightShouldPadValueOnTheRight() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("AB");
            List<ColumnTransformation> transformations = List.of(
                    new ColumnTransformation(TransformerType.PAD_RIGHT, null, null, null, 5, "X", null, null));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("ABXXX");
        }

        @Test
        @DisplayName("PAD_LEFT should use space as default pad character")
        void padLeftShouldUseSpaceAsDefaultPadCharacter() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("A");
            List<ColumnTransformation> transformations = List.of(
                    new ColumnTransformation(TransformerType.PAD_LEFT, null, null, null, 3, null, null, null));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("  A");
        }
    }

    @Nested
    @DisplayName("Replace Transformations")
    class ReplaceTransformations {

        @Test
        @DisplayName("REPLACE should replace pattern with replacement")
        void replaceShouldReplacePatternWithReplacement() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("hello-world-test");
            List<ColumnTransformation> transformations = List.of(ColumnTransformation.replace("-", "_"));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("hello_world_test");
        }

        @Test
        @DisplayName("STRIP_CHARS should remove matching characters")
        void stripCharsShouldRemoveMatchingCharacters() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("a1b2c3d4");
            List<ColumnTransformation> transformations = List.of(
                    new ColumnTransformation(TransformerType.STRIP_CHARS, null, "[0-9]", null, null, null, null, null));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("abcd");
        }
    }

    @Nested
    @DisplayName("Substring Transformations")
    class SubstringTransformations {

        @Test
        @DisplayName("SUBSTRING should extract portion of string")
        void substringShouldExtractPortionOfString() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("hello world");
            List<ColumnTransformation> transformations = List.of(
                    new ColumnTransformation(TransformerType.SUBSTRING, null, null, null, null, null, 0, 5));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("SUBSTRING without end should extract to end of string")
        void substringWithoutEndShouldExtractToEndOfString() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("hello world");
            List<ColumnTransformation> transformations = List.of(
                    new ColumnTransformation(TransformerType.SUBSTRING, null, null, null, null, null, 6, null));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("world");
        }
    }

    @Nested
    @DisplayName("Numeric Transformations")
    class NumericTransformations {

        @Test
        @DisplayName("NUMBER_FORMAT should format numeric values")
        void numberFormatShouldFormatNumericValues() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue(1234567.89);
            List<ColumnTransformation> transformations = List.of(
                    ColumnTransformation.withFormat(TransformerType.NUMBER_FORMAT, "#,##0.00"));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("1,234,567.89");
        }

        @Test
        @DisplayName("Numeric cell without transformations should return value as string")
        void numericCellWithoutTransformationsShouldReturnValueAsString() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue(42);

            // Act
            String result = transformer.transform(cell, null);

            // Assert
            assertThat(result).isEqualTo("42");
        }
    }

    @Nested
    @DisplayName("Chained Transformations")
    class ChainedTransformations {

        @Test
        @DisplayName("Multiple transformations should be applied in order")
        void multipleTransformationsShouldBeAppliedInOrder() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("  hello WORLD  ");
            List<ColumnTransformation> transformations = List.of(
                    ColumnTransformation.of(TransformerType.TRIM),
                    ColumnTransformation.of(TransformerType.LOWERCASE),
                    ColumnTransformation.of(TransformerType.TITLE_CASE));

            // Act
            String result = transformer.transform(cell, transformations);

            // Assert
            assertThat(result).isEqualTo("Hello World");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Null cell should return null")
        void nullCellShouldReturnNull() {
            // Arrange
            List<ColumnTransformation> transformations = List.of(ColumnTransformation.of(TransformerType.UPPERCASE));

            // Act
            String result = transformer.transform(null, transformations);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Empty transformations list should return original value")
        void emptyTransformationsListShouldReturnOriginalValue() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("hello");

            // Act
            String result = transformer.transform(cell, List.of());

            // Assert
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("Null transformations should return original value")
        void nullTransformationsShouldReturnOriginalValue() {
            // Arrange
            Cell cell = row.createCell(0);
            cell.setCellValue("hello");

            // Act
            String result = transformer.transform(cell, null);

            // Assert
            assertThat(result).isEqualTo("hello");
        }
    }
}
