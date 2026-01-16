package com.example.working_with_excels;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.example.working_with_excels.excel.application.dto.ExcelValidationReport;
import com.example.working_with_excels.excel.application.dto.SheetValidationReport;
import com.example.working_with_excels.excel.application.usecase.ExcelValidationService;
import com.example.working_with_excels.excel.domain.service.CellValidator;
import com.example.working_with_excels.excel.infrastructure.adapter.output.YamlExcelConfigLoader;

/**
 * Integration tests for the Excel validation use case.
 *
 * <p>
 * These tests verify the complete validation flow using real file resources.
 */
class ExcelValidationTest {

    @Test
    void testExcelStructureValidationReport() throws IOException {
        // Arrange
        YamlExcelConfigLoader configLoader = new YamlExcelConfigLoader();
        CellValidator cellValidator = new CellValidator();
        ExcelValidationService service = new ExcelValidationService(configLoader, cellValidator);

        // Act - use current resource files
        ExcelValidationReport report = service.validateExcelStructure(
                "import_data.xlsx",
                "import_mapping.yml");

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.filename()).isEqualTo("import_data.xlsx");
        assertThat(report.mappingFile()).isEqualTo("import_mapping.yml");
        assertThat(report.fileSizeFormatted()).isNotBlank();

        // Sheets present
        assertThat(report.sheets()).isNotEmpty();

        // Check that at least one sheet exists
        SheetValidationReport firstSheet = report.sheets().getFirst();
        assertThat(firstSheet.sheetName()).isNotBlank();
        assertThat(firstSheet.totalRows()).isGreaterThan(0);
    }
}
