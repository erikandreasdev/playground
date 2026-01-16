package com.example.working_with_excels.excel.domain.model;

/**
 * Value object representing a single transformation to apply to a cell value.
 *
 * <p>
 * This record encapsulates a transformation type along with optional
 * parameters needed for certain transformations (e.g., date format patterns,
 * replacement strings).
 *
 * @param type        the type of transformation to apply
 * @param format      optional format pattern (for DATE_FORMAT, NUMBER_FORMAT)
 * @param pattern     optional regex pattern (for REPLACE, STRIP_CHARS)
 * @param replacement optional replacement string (for REPLACE)
 * @param length      optional length parameter (for PAD_LEFT, PAD_RIGHT,
 *                    SUBSTRING)
 * @param padChar     optional padding character (for PAD_LEFT, PAD_RIGHT),
 *                    defaults to space
 * @param start       optional start index (for SUBSTRING)
 * @param end         optional end index (for SUBSTRING)
 */
public record ColumnTransformation(
        TransformerType type,
        String format,
        String pattern,
        String replacement,
        Integer length,
        String padChar,
        Integer start,
        Integer end) {

    /**
     * Creates a simple transformation with only a type.
     *
     * @param type the transformation type
     * @return a new ColumnTransformation with no additional parameters
     */
    public static ColumnTransformation of(TransformerType type) {
        return new ColumnTransformation(type, null, null, null, null, null, null, null);
    }

    /**
     * Creates a transformation with a format pattern.
     *
     * @param type   the transformation type
     * @param format the format pattern
     * @return a new ColumnTransformation with the specified format
     */
    public static ColumnTransformation withFormat(TransformerType type, String format) {
        return new ColumnTransformation(type, format, null, null, null, null, null, null);
    }

    /**
     * Creates a replacement transformation.
     *
     * @param pattern     the pattern to match
     * @param replacement the replacement string
     * @return a new ColumnTransformation for REPLACE operations
     */
    public static ColumnTransformation replace(String pattern, String replacement) {
        return new ColumnTransformation(TransformerType.REPLACE, null, pattern, replacement, null, null, null, null);
    }
}
