package com.example.working_with_excels.config.validation;

import java.util.List;

public record ExcelValidationReport(
        String filename,
        String mappingFile,
        double fileSizeMB,
        List<SheetValidationReport> sheets) {
}
