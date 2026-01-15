package com.example.working_with_excels.config.validation;

public record ColumnConfig(String name, ExcelColumnType type, ColumnValidation validation) {
}
