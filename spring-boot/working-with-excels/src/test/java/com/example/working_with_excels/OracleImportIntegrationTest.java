package com.example.working_with_excels;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.example.working_with_excels.excel.application.dto.ImportReport;
import com.example.working_with_excels.excel.application.dto.SheetImportResult;
import com.example.working_with_excels.excel.application.port.input.ExcelImportUseCase;
import com.example.working_with_excels.excel.domain.model.ImportMode;

/**
 * Integration test for Excel import against real Oracle database.
 *
 * <p>
 * Prerequisites:
 * <ol>
 * <li>Start Oracle: {@code docker compose up -d}</li>
 * <li>Init tables: {@code ./docker/init-db.sh}</li>
 * <li>Generate Excel:
 * {@code ./mvnw exec:java -Dexec.mainClass=com.example.working_with_excels.ReferenceExcelGenerator -Dexec.classpathScope=test}</li>
 * <li>Run test:
 * {@code ./mvnw test -Dtest=OracleImportIntegrationTest -Dspring.profiles.active=oracle}</li>
 * </ol>
 *
 * <p>
 * Or set RUN_ORACLE_TESTS=true to enable this test in CI.
 */
@SpringBootTest
@ActiveProfiles("oracle")
@EnabledIfEnvironmentVariable(named = "RUN_ORACLE_TESTS", matches = "true")
class OracleImportIntegrationTest {

    @Autowired
    private ExcelImportUseCase importUseCase;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void testFullImportPipeline() throws IOException {
        // Verify database connectivity
        Integer result = jdbcTemplate.queryForObject(
                "SELECT 1 FROM DUAL", new java.util.HashMap<>(), Integer.class);
        assertThat(result).isEqualTo(1);
        System.out.println("✅ Database connection verified");

        // Verify lookup tables have data
        Integer countryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM COUNTRIES", new java.util.HashMap<>(), Integer.class);
        assertThat(countryCount).isGreaterThan(0);
        System.out.println("✅ Lookup tables populated: " + countryCount + " countries");

        // Run import in EXECUTE mode
        ImportReport report = importUseCase.importExcel(
                "reference_import.xlsx",
                "reference_import_mapping.yml",
                ImportMode.EXECUTE);

        // Print report
        printReport(report);

        // Assertions
        assertThat(report).isNotNull();
        assertThat(report.mode()).isEqualTo(ImportMode.EXECUTE);
        assertThat(report.sheets()).isNotEmpty();

        // Verify data was inserted
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM APP_USERS", new java.util.HashMap<>(), Integer.class);
        System.out.println("✅ Users inserted: " + userCount);
        assertThat(userCount).isGreaterThan(0);

        Integer productCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PRODUCTS", new java.util.HashMap<>(), Integer.class);
        System.out.println("✅ Products inserted: " + productCount);
        assertThat(productCount).isGreaterThan(0);

        // Verify transformations applied
        String userName = jdbcTemplate.queryForObject(
                "SELECT FULL_NAME FROM APP_USERS WHERE USER_ID = 1",
                new java.util.HashMap<>(), String.class);
        System.out.println("✅ User name (should be Title Case, trimmed): " + userName);
        assertThat(userName).isEqualTo("John Doe");

        // Verify lookups resolved
        Integer countryId = jdbcTemplate.queryForObject(
                "SELECT COUNTRY_ID FROM APP_USERS WHERE USER_ID = 1",
                new java.util.HashMap<>(), Integer.class);
        System.out.println("✅ Country lookup resolved: ID=" + countryId);
        assertThat(countryId).isEqualTo(1); // United States = 1
    }

    @Test
    void testDryRunMode() throws IOException {
        // Run import in DRY_RUN mode - no data should be inserted
        ImportReport report = importUseCase.importExcel(
                "reference_import.xlsx",
                "reference_import_mapping.yml",
                ImportMode.DRY_RUN);

        printReport(report);

        assertThat(report.mode()).isEqualTo(ImportMode.DRY_RUN);
        assertThat(report.metrics().insertedRows()).isGreaterThan(0);
    }

    private void printReport(ImportReport report) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("IMPORT REPORT (" + report.mode() + " MODE)");
        System.out.println("=".repeat(60));
        System.out.println("File: " + report.filename());
        System.out.println("Duration: " + report.duration().toMillis() + "ms");

        for (SheetImportResult sheet : report.sheets()) {
            System.out.println("\nSheet: " + sheet.sheetName() + " -> " + sheet.tableName());
            System.out.println("  Total: " + sheet.totalRows() +
                    " | Inserted: " + sheet.insertedRows() +
                    " | Skipped: " + sheet.skippedRows());

            if (!sheet.errors().isEmpty()) {
                System.out.println("  Errors:");
                sheet.errors()
                        .forEach(err -> System.out.println("    Row " + err.rowNumber() + ": " + err.errorMessage()));
            }
        }

        System.out.println("\nTOTALS: Rows=" + report.metrics().totalRows() +
                " | Inserted=" + report.metrics().insertedRows() +
                " | Errors=" + report.metrics().errorRows());
        System.out.println("=".repeat(60) + "\n");
    }
}
