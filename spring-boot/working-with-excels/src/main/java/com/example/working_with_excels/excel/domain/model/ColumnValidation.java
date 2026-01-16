package com.example.working_with_excels.excel.domain.model;

import java.util.List;

/**
 * Value object representing the validation rules for a single Excel column.
 *
 * <p>
 * This record encapsulates various validation constraints that can be
 * applied to cell values, including regex patterns, length limits, numeric
 * ranges, and value restrictions.
 *
 * @param regex          optional regex pattern the cell value must match
 * @param dateFormat     optional format pattern for date values
 * @param minLength      optional minimum string length
 * @param maxLength      optional maximum string length
 * @param min            optional minimum numeric value
 * @param max            optional maximum numeric value
 * @param allowedValues  optional list of allowed string values (enumeration)
 * @param excludedValues optional list of forbidden string values
 * @param notEmpty       whether the cell value is required (cannot be blank)
 */
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
