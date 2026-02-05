package com.example.demo.core.internal.domain.enums;

/**
 * Supported row-level operation types for computing values from multiple columns.
 *
 * <p>Operations can be chained sequentially to perform complex transformations. The CONCATENATE
 * operation is typically used first to join values from multiple columns, followed by additional
 * transformations like UPPERCASE or REPLACE.
 */
public enum OperationType {
  /** Join values from multiple columns with a separator. */
  CONCATENATE,

  /** Replace a pattern in the current value with a replacement string. */
  REPLACE,

  /** Extract a substring from the current value using start and end indices. */
  SUBSTRING,

  /** Convert the current value to uppercase. */
  UPPERCASE,

  /** Convert the current value to lowercase. */
  LOWERCASE,

  /** Trim leading and trailing whitespace from the current value. */
  TRIM
}
