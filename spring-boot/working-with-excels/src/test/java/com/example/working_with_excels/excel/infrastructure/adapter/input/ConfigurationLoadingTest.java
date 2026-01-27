package com.example.working_with_excels.excel.infrastructure.adapter.input;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.example.working_with_excels.excel.domain.model.ColumnConfig;
import com.example.working_with_excels.excel.domain.model.FilesConfig;
import com.example.working_with_excels.excel.domain.model.RowConstraint;
import com.example.working_with_excels.excel.domain.model.SheetConfig;
import com.example.working_with_excels.excel.infrastructure.adapter.output.YamlExcelConfigLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

class ConfigurationLoadingTest {

    private final YamlExcelConfigLoader loader = new YamlExcelConfigLoader();

    @Test
    @DisplayName("should load excel_import.yml and parse row constraints correctly")
    void shouldLoadExcelImportConfig() throws IOException {
        // Arrange
        ClassPathResource resource = new ClassPathResource("excel_import.yml");

        // Act
        FilesConfig config;
        try (InputStream stream = resource.getInputStream()) {
            config = loader.loadConfigFromStream(stream);
        }

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.files()).hasSize(1);

        SheetConfig sheet1 = config.files().getFirst().sheets().getFirst();
        assertThat(sheet1.name()).isEqualTo("Users");
        assertThat(sheet1.table()).isEqualTo("APP_USERS");

        // Verify Row Constraints
        List<RowConstraint> constraints = sheet1.rowConstraints();
        assertThat(constraints).hasSize(2);

        // Constraint 1: Tags
        RowConstraint c1 = constraints.get(0);
        assertThat(c1.columns()).containsExactly("Country", "Status");
        assertThat(c1.type()).isEqualTo(RowConstraint.ConstraintType.NOT_ALL_EMPTY);
        assertThat(c1.errorMessage()).isEqualTo("Both Country and Status cannot be empty");

        // Constraint 2: SpEL
        RowConstraint c2 = constraints.get(1);
        assertThat(c2.columns()).containsExactly("Account Balance", "Status");
        // Type usually maps to CUSTOM if not specified in YAML but default might be
        // null if field missing
        // YAML says type: "CUSTOM"
        assertThat(c2.type()).isEqualTo(RowConstraint.ConstraintType.CUSTOM);
        assertThat(c2.expression()).contains("#row['Status'] == 'ACTIVE'");

        // Verify Column Mappings
        ColumnConfig countryCol = sheet1.columns().stream()
                .filter(c -> "Country".equals(c.name()))
                .findFirst()
                .orElseThrow();
        assertThat(countryCol.dbMapping().lookup()).isNotNull();
        assertThat(countryCol.dbMapping().lookup().table()).isEqualTo("COUNTRIES");
    }

    @Test
    @DisplayName("should load excel_structure.yml correctly")
    void shouldLoadExcelStructureConfig() throws IOException {
        // Arrange
        ClassPathResource resource = new ClassPathResource("excel_structure.yml");

        // Act
        FilesConfig config;
        try (InputStream stream = resource.getInputStream()) {
            config = loader.loadConfigFromStream(stream);
        }

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.files()).hasSize(1);

        SheetConfig sheet1 = config.files().getFirst().sheets().getFirst();
        assertThat(sheet1.name()).isEqualTo("Users");

        // Verify validation rules
        ColumnConfig emailCol = sheet1.columns().stream()
                .filter(c -> "Email Address".equals(c.name()))
                .findFirst()
                .orElseThrow();

        assertThat(emailCol.validation()).isNotNull();
        assertThat(emailCol.validation().regex()).contains("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");

        ColumnConfig statusCol = sheet1.columns().stream()
                .filter(c -> "Status".equals(c.name()))
                .findFirst()
                .orElseThrow();
        assertThat(statusCol.validation().allowedValues()).contains("ACTIVE", "INACTIVE", "PENDING");
    }
}
