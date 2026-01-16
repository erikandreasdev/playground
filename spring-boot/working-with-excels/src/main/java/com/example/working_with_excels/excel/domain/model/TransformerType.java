package com.example.working_with_excels.excel.domain.model;

/**
 * Enumeration of available cell value transformation types.
 *
 * <p>
 * Each type represents a specific transformation operation that can be
 * applied to cell values during Excel processing. Multiple transformations
 * can be chained together and will be applied in order.
 */
public enum TransformerType {

    /**
     * Converts the cell value to uppercase.
     */
    UPPERCASE,

    /**
     * Converts the cell value to lowercase.
     */
    LOWERCASE,

    /**
     * Removes leading and trailing whitespace from the cell value.
     */
    TRIM,

    /**
     * Capitalizes the first letter of each word.
     */
    TITLE_CASE,

    /**
     * Capitalizes only the first letter of the string.
     */
    SENTENCE_CASE,

    /**
     * Removes all whitespace from the cell value.
     */
    REMOVE_WHITESPACE,

    /**
     * Normalizes multiple spaces into a single space.
     */
    NORMALIZE_SPACES,

    /**
     * Formats a date value according to a specified pattern.
     * Requires a format parameter in the transformation configuration.
     */
    DATE_FORMAT,

    /**
     * Formats a numeric value with a specific number of decimal places.
     * Requires a format parameter in the transformation configuration.
     */
    NUMBER_FORMAT,

    /**
     * Replaces occurrences of a pattern with a replacement string.
     * Requires pattern and replacement parameters.
     */
    REPLACE,

    /**
     * Pads the value on the left to reach a minimum length.
     * Requires length and optional padChar parameters.
     */
    PAD_LEFT,

    /**
     * Pads the value on the right to reach a minimum length.
     * Requires length and optional padChar parameters.
     */
    PAD_RIGHT,

    /**
     * Extracts a substring from the cell value.
     * Requires start and optional end parameters.
     */
    SUBSTRING,

    /**
     * Removes specified characters or patterns from the value.
     * Requires a pattern parameter.
     */
    STRIP_CHARS
}
