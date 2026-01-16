package com.example.working_with_excels;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.example.working_with_excels.excel.application.dto.ExcelValidationReport;
import com.example.working_with_excels.excel.application.dto.SheetValidationReport;
import com.example.working_with_excels.excel.application.usecase.ExcelValidationService;
import com.example.working_with_excels.excel.domain.service.CellValidator;
import com.example.working_with_excels.excel.infrastructure.adapter.output.YamlExcelConfigLoader;

import static org.assertj.core.api.Assertions.assertThat;

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

        // Act
        ExcelValidationReport report = service.validateExcelStructure("excel_data.xlsx", "excel_data_mapping.yml");

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.filename()).isEqualTo("excel_data.xlsx");
        assertThat(report.mappingFile()).isEqualTo("excel_data_mapping.yml");
        assertThat(report.fileSizeFormatted()).contains("KB");

        assertThat(report.sheets()).hasSize(3);

        // Check "Users" sheet
        SheetValidationReport usersSheet = report.sheets().stream()
                .filter(s -> s.sheetName().equals("Users"))
                .findFirst()
                .orElseThrow();

        assertThat(usersSheet.totalRows()).isEqualTo(50);
        assertThat(usersSheet.validRows()).isEqualTo(50);
        assertThat(usersSheet.invalidRows()).isZero();
        assertThat(usersSheet.rowErrors()).isEmpty();
    }
}
