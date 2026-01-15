package com.example.working_with_excels;

import com.example.working_with_excels.config.validation.ExcelValidationReport;
import com.example.working_with_excels.config.validation.ExcelValidationService;
import com.example.working_with_excels.config.validation.SheetValidationReport;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ExcelValidationTest {

    @Test
    void testExcelStructureValidationReport() throws IOException {
        ExcelValidationService service = new ExcelValidationService();

        ExcelValidationReport report = service.validateExcelStructure("excel_data.xlsx", "excel_data_mapping.yml");

        assertThat(report).isNotNull();
        assertThat(report.filename()).isEqualTo("excel_data.xlsx");
        assertThat(report.mappingFile()).isEqualTo("excel_data_mapping.yml");
        assertThat(report.fileSizeMB()).isGreaterThan(0);

        assertThat(report.sheets()).hasSize(3);

        // Check "Users" sheet
        SheetValidationReport usersSheet = report.sheets().stream()
                .filter(s -> s.sheetName().equals("Users"))
                .findFirst()
                .orElseThrow();

        assertThat(usersSheet.totalRows()).isEqualTo(50);
        assertThat(usersSheet.validRows()).isEqualTo(50);
        assertThat(usersSheet.invalidRows()).isEqualTo(0);
        assertThat(usersSheet.rowErrors()).isEmpty();
    }
}
