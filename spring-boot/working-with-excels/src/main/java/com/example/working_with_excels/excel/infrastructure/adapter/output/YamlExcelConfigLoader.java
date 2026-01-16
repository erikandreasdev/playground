package com.example.working_with_excels.excel.infrastructure.adapter.output;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
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
 * This adapter implements the {@link ExcelConfigLoaderPort} output port
 * and provides the concrete implementation for reading and parsing YAML
 * configuration files from the classpath.
 */
@Component
public class YamlExcelConfigLoader implements ExcelConfigLoaderPort {

    @Override
    public FilesConfig loadConfig(String yamlConfigPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream yamlStream = new ClassPathResource(yamlConfigPath).getInputStream()) {
            return mapper.readValue(yamlStream, FilesConfig.class);
        }
    }

    @Override
    public FileConfig findFileConfig(FilesConfig config, String filename) {
        return config.files().stream()
                .filter(f -> f.filename().equals(filename))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No configuration found for file: " + filename));
    }
}
