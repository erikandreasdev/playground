package com.example.working_with_excels;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.working_with_excels.excel.application.dto.ImportReport;
import com.example.working_with_excels.excel.application.dto.SheetImportResult;
import com.example.working_with_excels.excel.application.port.input.ExcelImportUseCase;
import com.example.working_with_excels.excel.domain.model.ImportMode;

/**
 * Integration test for Excel import in DRY_RUN mode.
 *
 * <p>
 * This test demonstrates the import pipeline without requiring a database.
 * Watch the console output to see the logged SQL statements!
 */
@SpringBootTest
class ExcelImportDryRunTest {

    @Autowired
    private ExcelImportUseCase importUseCase;

    @Test
    void testImportDryRun() throws IOException {
        // Act
        ImportReport report = importUseCase.importExcel(
                "test_import.xlsx",
                "test_import_mapping.yml",
                ImportMode.DRY_RUN);

        // Assert - Print the report
        System.out.println("\n" + "=".repeat(60));
        System.out.println("IMPORT REPORT (DRY_RUN MODE)");
        System.out.println("=".repeat(60));
        System.out.println("File: " + report.filename());
        System.out.println("Duration: " + report.duration().toMillis() + "ms");
        System.out.println("Mode: " + report.mode());
        System.out.println();

        for (SheetImportResult sheet : report.sheets()) {
            System.out.println("Sheet: " + sheet.sheetName() + " -> " + sheet.tableName());
            System.out.println("  Total rows: " + sheet.totalRows());
            System.out.println("  Inserted:   " + sheet.insertedRows());
            System.out.println("  Skipped:    " + sheet.skippedRows());

            if (!sheet.errors().isEmpty()) {
                System.out.println("  Errors:");
                sheet.errors()
                        .forEach(err -> System.out.println("    Row " + err.rowNumber() + ": " + err.errorMessage()));
            }
            System.out.println();
        }

        System.out.println("METRICS:");
        System.out.println("  Total rows processed: " + report.metrics().totalRows());
        System.out.println("  Total inserted:       " + report.metrics().insertedRows());
        System.out.println("  Total skipped:        " + report.metrics().skippedRows());
        System.out.println("  Total errors:         " + report.metrics().errorRows());
        System.out.println("=".repeat(60) + "\n");

        // Basic assertions
        assertThat(report).isNotNull();
        assertThat(report.mode()).isEqualTo(ImportMode.DRY_RUN);
        assertThat(report.sheets()).hasSize(2);

        // Users sheet: 5 rows, 1 should fail (empty email)
        SheetImportResult usersSheet = report.sheets().get(0);
        assertThat(usersSheet.sheetName()).isEqualTo("Users");
        assertThat(usersSheet.totalRows()).isEqualTo(5);
        assertThat(usersSheet.insertedRows()).isEqualTo(4); // 4 valid
        assertThat(usersSheet.skippedRows()).isEqualTo(1); // 1 invalid (empty email)

        // Products sheet: 5 rows, all should succeed
        SheetImportResult productsSheet = report.sheets().get(1);
        assertThat(productsSheet.sheetName()).isEqualTo("Products");
        assertThat(productsSheet.totalRows()).isEqualTo(5);
        assertThat(productsSheet.insertedRows()).isEqualTo(5);
    }
}
