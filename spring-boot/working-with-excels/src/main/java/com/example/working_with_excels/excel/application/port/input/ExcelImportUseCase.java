package com.example.working_with_excels.excel.application.port.input;

import java.io.IOException;

import com.example.working_with_excels.excel.application.dto.ImportReport;
import com.example.working_with_excels.excel.domain.model.FileSource;
import com.example.working_with_excels.excel.domain.model.ImportMode;

/**
 * Input port defining the use case for Excel import operations.
 *
 * <p>
 * This interface represents the primary port for importing Excel data
 * into database tables according to the YAML configuration.
 */
public interface ExcelImportUseCase {

    /**
     * Imports Excel data from classpath resources.
     *
     * @param excelFileName  the Excel filename (classpath resource)
     * @param yamlConfigPath the YAML config filename (classpath resource)
     * @param mode           EXECUTE or DRY_RUN
     * @return the import report
     * @throws IOException if files cannot be read
     */
    ImportReport importExcel(String excelFileName, String yamlConfigPath, ImportMode mode)
            throws IOException;

    /**
     * Imports Excel data from any file source.
     *
     * <p>
     * Supports multiple origins: classpath, filesystem, multipart upload, URL
     * download.
     *
     * @param excelSource  the Excel file source
     * @param configSource the YAML config file source
     * @param mode         EXECUTE or DRY_RUN
     * @return the import report
     * @throws IOException if files cannot be read
     */
    ImportReport importExcel(FileSource excelSource, FileSource configSource, ImportMode mode)
            throws IOException;
}
