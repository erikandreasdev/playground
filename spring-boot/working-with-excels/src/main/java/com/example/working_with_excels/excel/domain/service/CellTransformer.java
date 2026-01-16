package com.example.working_with_excels.excel.domain.service;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

import com.example.working_with_excels.excel.domain.model.ColumnTransformation;

/**
 * Domain service responsible for applying transformations to Excel cell values.
 *
 * <p>
 * This class encapsulates the core transformation logic for cell values.
 * Transformations are applied in the order they are specified, allowing
 * for chained transformations.
 */
public class CellTransformer {

    /** Default padding character for PAD_LEFT and PAD_RIGHT transformations. */
    private static final String DEFAULT_PAD_CHAR = " ";

    /**
     * Default date format pattern following ISO-8601 standard (e.g., 2026-01-16).
     */
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * Default number format pattern with grouping and two decimal places (e.g.,
     * 1,234.56).
     */
    private static final String DEFAULT_NUMBER_FORMAT = "#,##0.00";

    /**
     * Applies a list of transformations to a cell value.
     *
     * @param cell            the Excel cell to transform
     * @param transformations the ordered list of transformations to apply
     * @return the transformed value as a String, or null if the cell is blank
     */
    public String transform(Cell cell, List<ColumnTransformation> transformations) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        if (transformations == null || transformations.isEmpty()) {
            return extractCellValue(cell);
        }

        String value = extractCellValue(cell);

        for (ColumnTransformation transformation : transformations) {
            value = applyTransformation(value, transformation, cell);
        }

        return value;
    }

    /**
     * Applies a single transformation to a string value.
     *
     * @param value          the current value to transform
     * @param transformation the transformation configuration
     * @param originalCell   the original cell (for date/numeric context)
     * @return the transformed value
     */
    private String applyTransformation(String value, ColumnTransformation transformation, Cell originalCell) {
        if (value == null) {
            return null;
        }

        return switch (transformation.type()) {
            case UPPERCASE -> value.toUpperCase();
            case LOWERCASE -> value.toLowerCase();
            case TRIM -> value.trim();
            case TITLE_CASE -> toTitleCase(value);
            case SENTENCE_CASE -> toSentenceCase(value);
            case REMOVE_WHITESPACE -> value.replaceAll("\\s+", "");
            case NORMALIZE_SPACES -> value.replaceAll("\\s+", " ").trim();
            case DATE_FORMAT -> formatDate(originalCell, transformation.format());
            case NUMBER_FORMAT -> formatNumber(originalCell, transformation.format());
            case REPLACE -> applyReplace(value, transformation);
            case PAD_LEFT -> padLeft(value, transformation);
            case PAD_RIGHT -> padRight(value, transformation);
            case SUBSTRING -> substring(value, transformation);
            case STRIP_CHARS -> stripChars(value, transformation);
        };
    }

    private String extractCellValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDateTime dateTime = cell.getLocalDateTimeCellValue();
                    yield dateTime.toLocalDate().toString();
                }
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue) && !Double.isInfinite(numValue)) {
                    yield String.valueOf((long) numValue);
                }
                yield String.valueOf(numValue);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private String toTitleCase(String value) {
        if (value.isEmpty()) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : value.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    private String toSentenceCase(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }

    private String formatDate(Cell cell, String format) {
        if (cell == null || !DateUtil.isCellDateFormatted(cell)) {
            return "";
        }
        String dateFormat = format != null ? format : DEFAULT_DATE_FORMAT;
        LocalDateTime dateTime = cell.getLocalDateTimeCellValue();
        LocalDate date = dateTime.toLocalDate();
        return date.format(DateTimeFormatter.ofPattern(dateFormat));
    }

    private String formatNumber(Cell cell, String format) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            return "";
        }
        String numberFormat = format != null ? format : DEFAULT_NUMBER_FORMAT;
        DecimalFormat df = new DecimalFormat(numberFormat);
        return df.format(cell.getNumericCellValue());
    }

    private String applyReplace(String value, ColumnTransformation transformation) {
        if (transformation.pattern() == null) {
            return value;
        }
        String replacement = transformation.replacement() != null ? transformation.replacement() : "";
        return value.replaceAll(transformation.pattern(), replacement);
    }

    private String padLeft(String value, ColumnTransformation transformation) {
        if (transformation.length() == null || value.length() >= transformation.length()) {
            return value;
        }
        String padChar = transformation.padChar() != null ? transformation.padChar() : DEFAULT_PAD_CHAR;
        char pad = padChar.isEmpty() ? ' ' : padChar.charAt(0);
        int padLength = transformation.length() - value.length();
        return String.valueOf(pad).repeat(padLength) + value;
    }

    private String padRight(String value, ColumnTransformation transformation) {
        if (transformation.length() == null || value.length() >= transformation.length()) {
            return value;
        }
        String padChar = transformation.padChar() != null ? transformation.padChar() : DEFAULT_PAD_CHAR;
        char pad = padChar.isEmpty() ? ' ' : padChar.charAt(0);
        int padLength = transformation.length() - value.length();
        return value + String.valueOf(pad).repeat(padLength);
    }

    private String substring(String value, ColumnTransformation transformation) {
        if (transformation.start() == null) {
            return value;
        }
        int start = Math.clamp(transformation.start(), 0, value.length());
        int end = transformation.end() != null
                ? Math.clamp(transformation.end(), start, value.length())
                : value.length();
        return value.substring(start, end);
    }

    private String stripChars(String value, ColumnTransformation transformation) {
        if (transformation.pattern() == null) {
            return value;
        }
        return Pattern.compile(transformation.pattern()).matcher(value).replaceAll("");
    }
}
