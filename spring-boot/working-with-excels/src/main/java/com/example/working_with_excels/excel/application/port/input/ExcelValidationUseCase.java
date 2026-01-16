package com.example.working_with_excels.excel.application.port.input;

import java.io.IOException;

import com.example.working_with_excels.excel.application.dto.ExcelValidationReport;
import org.springframework.lang.NonNull;

/**
 * Input port defining the use case for Excel validation operations.
 *
 * <p>
 * This interface represents the primary port through which external
 * actors (e.g., REST controllers, CLI) interact with the Excel validation
 * functionality. Implementations contain the application logic.
 */
public interface ExcelValidationUseCase {

    /**
     * Validates the structure and content of an Excel file against a YAML
     * configuration.
     *
     * @param excelFileName  the name of the Excel file to validate (classpath
     *                       resource)
     * @param yamlConfigPath the path to the YAML configuration file (classpath
     *                       resource)
     * @return a comprehensive validation report containing results for all sheets
     * @throws IOException if the Excel file or configuration cannot be read
     */
    ExcelValidationReport validateExcelStructure(@NonNull String excelFileName, @NonNull String yamlConfigPath)
            throws IOException;
}
