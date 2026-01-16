package com.example.working_with_excels.excel.application.port.output;

import java.io.IOException;
import java.io.InputStream;

import com.example.working_with_excels.excel.domain.model.FileConfig;
import com.example.working_with_excels.excel.domain.model.FilesConfig;
import org.springframework.lang.NonNull;

/**
 * Output port for loading Excel configuration from external sources.
 *
 * <p>
 * This interface defines how the application layer retrieves configuration
 * data. Infrastructure adapters implement this port to provide the actual
 * loading mechanism (e.g., from YAML files, databases, streams).
 */
public interface ExcelConfigLoaderPort {

    /**
     * Loads the complete files configuration from a classpath YAML file.
     *
     * @param yamlConfigPath the path to the YAML configuration file
     * @return the parsed configuration
     * @throws IOException if the file cannot be read or parsed
     */
    FilesConfig loadConfig(@NonNull String yamlConfigPath) throws IOException;

    /**
     * Loads the complete files configuration from an input stream.
     *
     * @param inputStream the YAML configuration as an input stream
     * @return the parsed configuration
     * @throws IOException if the stream cannot be read or parsed
     */
    FilesConfig loadConfigFromStream(InputStream inputStream) throws IOException;

    /**
     * Finds the configuration for a specific file within the loaded configuration.
     *
     * @param config   the complete files configuration
     * @param filename the name of the file to find configuration for
     * @return the file configuration matching the given filename
     * @throws IllegalArgumentException if no configuration exists for the filename
     */
    FileConfig findFileConfig(FilesConfig config, String filename);
}
