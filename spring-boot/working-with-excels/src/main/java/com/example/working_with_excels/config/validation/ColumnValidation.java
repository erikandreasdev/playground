package com.example.working_with_excels.config.validation;

import java.util.List;

public record ColumnValidation(
                String regex,
                String dateFormat,
                Integer minLength,
                Integer maxLength,
                Double min,
                Double max,
                List<String> allowedValues,
                List<String> excludedValues,
                Boolean notEmpty) {
}
