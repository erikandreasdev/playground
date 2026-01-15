package com.example.working_with_excels.config.validation;

import java.util.List;

public record SheetValidationReport(
        String sheetName,
        int totalRows,
        int validRows,
        int invalidRows,
        List<RowValidationError> rowErrors) {
}
