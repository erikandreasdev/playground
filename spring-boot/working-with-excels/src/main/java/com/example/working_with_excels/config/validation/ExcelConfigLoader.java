package com.example.working_with_excels.config.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class ExcelConfigLoader {

    public FilesConfig loadConfig(String yamlConfigPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream yamlStream = new ClassPathResource(yamlConfigPath).getInputStream()) {
            return mapper.readValue(yamlStream, FilesConfig.class);
        }
    }

    public FileConfig findFileConfig(FilesConfig config, String filename) {
        return config.files().stream()
                .filter(f -> f.filename().equals(filename))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No configuration found for file: " + filename));
    }
}
