package com.example.working_with_excels.config.validation;

import java.util.List;

public record FileConfig(String filename, List<SheetConfig> sheets) {
}
