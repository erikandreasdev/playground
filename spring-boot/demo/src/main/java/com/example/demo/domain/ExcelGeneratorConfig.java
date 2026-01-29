package com.example.demo.domain;

import java.util.List;

/**
 * Configuration for generating an Excel file with fake data.
 *
 * <p>This record defines the structure of a YAML file that specifies: - Excel filename - Sheet
 * definitions with columns and their data types - Number of rows to generate per sheet
 */
public record ExcelGeneratorConfig(String filename, List<SheetConfig> sheets) {

  /**
   * Configuration for a single sheet in the Excel file.
   *
   * @param name Sheet name
   * @param rowCount Number of rows to generate
   * @param columns Column definitions
   */
  public record SheetConfig(String name, int rowCount, List<ColumnDef> columns) {}

  /**
   * Column definition specifying how to generate fake data.
   *
   * @param name Column name (header)
   * @param fakerType Type of fake data to generate (e.g., "name.firstName",
   *     "internet.emailAddress")
   * @param fakerMethod Optional sub-method for complex faker types
   * @param format Optional format string for dates or numbers
   * @param allowedValues Optional list of fixed values to randomly select from
   * @param min Optional minimum value for numbers
   * @param max Optional maximum value for numbers
   */
  public record ColumnDef(
      String name,
      String fakerType,
      String fakerMethod,
      String format,
      List<String> allowedValues,
      Integer min,
      Integer max) {}
}
