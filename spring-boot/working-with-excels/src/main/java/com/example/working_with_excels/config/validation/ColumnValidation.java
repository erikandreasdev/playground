package com.example.working_with_excels.config.validation;

public record ColumnValidation(
        String regex,
        String dateFormat,
        Integer minLength,
        Integer maxLength,
        Double min,
        Double max) {
}
