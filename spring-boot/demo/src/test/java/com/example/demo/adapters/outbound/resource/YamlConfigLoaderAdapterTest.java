package com.example.demo.adapters.outbound.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.demo.core.exceptions.MappingException;
import com.example.demo.core.internal.domain.config.ValidationConfig;
import com.example.demo.core.internal.valueobjects.LoadedResource;
import com.example.demo.core.internal.valueobjects.SourceType;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

/** Unit tests for {@link YamlConfigLoaderAdapter}. */
class YamlConfigLoaderAdapterTest {

  private YamlConfigLoaderAdapter loader;

  @BeforeEach
  void setUp() {
    loader = new YamlConfigLoaderAdapter();
  }

  @Test
  void loadConfig_shouldParseValidYaml() {
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

    ValidationConfig result = loader.loadConfig(config);

    assertThat(result.sheets()).hasSize(1);
    assertThat(result.sheets().get(0).name()).isEqualTo("Test");
  }

  @Test
  void loadConfig_shouldThrowMappingExceptionOnInvalidYaml() {
    String invalidYaml = "invalid: : yaml";
    LoadedResource config =
        new LoadedResource(
            "config.yml",
            invalidYaml.length(),
            SourceType.FILESYSTEM,
            MediaType.TEXT_PLAIN,
            new ByteArrayResource(invalidYaml.getBytes(StandardCharsets.UTF_8)));

    assertThrows(MappingException.class, () -> loader.loadConfig(config));
  }
}
