package com.example.working_with_excels.excel.infrastructure.adapter.output;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.example.working_with_excels.excel.application.port.output.ExcelConfigLoaderPort;
import com.example.working_with_excels.excel.domain.model.FileConfig;
import com.example.working_with_excels.excel.domain.model.FilesConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Infrastructure adapter for loading Excel configuration from YAML files.
 *
 * <p>
 * Supports loading from classpath resources and input streams.
 */
@Component
public class YamlExcelConfigLoader implements ExcelConfigLoaderPort {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Override
    public FilesConfig loadConfig(@NonNull String yamlConfigPath) throws IOException {
        try (InputStream yamlStream = new ClassPathResource(yamlConfigPath).getInputStream()) {
            return loadConfigFromStream(yamlStream);
        }
    }

    @Override
    public FilesConfig loadConfigFromStream(InputStream inputStream) throws IOException {
        return yamlMapper.readValue(inputStream, FilesConfig.class);
    }

    @Override
    public FileConfig findFileConfig(FilesConfig config, String filename) {
        return config.files().stream()
                .filter(f -> f.filename().equals(filename))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No configuration found for file: " + filename));
    }
}
