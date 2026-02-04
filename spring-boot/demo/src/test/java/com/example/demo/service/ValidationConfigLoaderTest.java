package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.demo.domain.ValidationConfig;
import com.example.demo.resource.LoadedResource;
import com.example.demo.resource.SourceType;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

/** Unit tests for {@link ValidationConfigLoader}. */
class ValidationConfigLoaderTest {

  private ValidationConfigLoader loader;

  @BeforeEach
  void setUp() {
    loader = new ValidationConfigLoader();
  }

  @Test
  void loadConfig_shouldParseValidYaml() {
    // Arrange
    String yaml =
        """
                sheets:
                  - name: "Test"
                    columns:
                      - name: "ID"
                """;
    LoadedResource config =
        new LoadedResource(
            "config.yml",
            yaml.length(),
            SourceType.FILESYSTEM,
            MediaType.TEXT_PLAIN,
            new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));

    // Act
    ValidationConfig result = loader.loadConfig(config);

    // Assert
    assertThat(result.sheets()).hasSize(1);
    assertThat(result.sheets().get(0).name()).isEqualTo("Test");
  }

  @Test
  void loadConfig_shouldThrowMappingExceptionOnInvalidYaml() {
    // Arrange
    String invalidYaml = "invalid: : yaml";
    LoadedResource config =
        new LoadedResource(
            "config.yml",
            invalidYaml.length(),
            SourceType.FILESYSTEM,
            MediaType.TEXT_PLAIN,
            new ByteArrayResource(invalidYaml.getBytes(StandardCharsets.UTF_8)));

    // Act & Assert
    assertThrows(
        com.example.demo.exception.MappingException.class, () -> loader.loadConfig(config));
  }
}
