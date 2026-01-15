package com.example.working_with_excels.config.validation;

import java.util.List;

public record SheetConfig(String name, List<ColumnConfig> columns) {
}
