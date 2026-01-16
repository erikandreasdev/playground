package com.example.working_with_excels.excel.application.port.input;

import java.io.IOException;

import com.example.working_with_excels.excel.application.dto.ExcelTransformationResult;
import org.springframework.lang.NonNull;

/**
 * Input port defining the use case for Excel transformation operations.
 *
 * <p>
 * This interface represents the primary port for transforming Excel
 * cell values according to configured transformation rules. Transformations
 * are applied after reading cell values and before validation.
 */
public interface ExcelTransformationUseCase {

    /**
     * Transforms cell values in an Excel file according to the YAML configuration.
     *
     * <p>
     * This method reads the Excel file, applies configured transformations
     * to each cell value, and returns the results. Transformations are applied
     * in the order specified in the configuration.
     *
     * @param excelFileName  the name of the Excel file to process (classpath
     *                       resource)
     * @param yamlConfigPath the path to the YAML configuration file (classpath
     *                       resource)
     * @return a complete transformation result containing all transformed values
     * @throws IOException if the Excel file or configuration cannot be read
     */
    ExcelTransformationResult transformExcel(@NonNull String excelFileName, @NonNull String yamlConfigPath)
            throws IOException;
}
