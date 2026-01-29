package com.example.demo.util;

import com.example.demo.domain.ExcelGeneratorConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Random;
import net.datafaker.Faker;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Utility class for generating Excel files with fake data based on YAML configuration.
 *
 * <p>This class reads a YAML configuration file that defines the structure of an Excel file
 * (sheets, columns, data types) and uses Datafaker to generate realistic fake data.
 */
public class ExcelGenerator {

  private final Faker faker;
  private final Random random;
  private final ObjectMapper yamlMapper;

  /** Constructs an ExcelGenerator with default Faker instance. */
  public ExcelGenerator() {
    this.faker = new Faker();
    this.random = new Random();
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
  }

  /**
   * Generates an Excel file based on a YAML configuration file.
   *
   * @param configPath Path to the YAML configuration file (can be classpath resource or file path)
   * @param outputPath Where to save the generated Excel file
   * @throws IOException if reading config or writing Excel fails
   */
  public void generateFromConfig(String configPath, String outputPath) throws IOException {
    ExcelGeneratorConfig config = loadConfig(configPath);
    generateExcel(config, outputPath);
  }

  /**
   * Loads configuration from YAML file.
   *
   * @param configPath Path to configuration file
   * @return Parsed configuration
   * @throws IOException if file cannot be read or parsed
   */
  private ExcelGeneratorConfig loadConfig(String configPath) throws IOException {
    // Try loading from classpath first
    InputStream is = getClass().getClassLoader().getResourceAsStream(configPath);
    if (is != null) {
      return yamlMapper.readValue(is, ExcelGeneratorConfig.class);
    }

    // Fall back to file system
    return yamlMapper.readValue(new File(configPath), ExcelGeneratorConfig.class);
  }

  /**
   * Generates the Excel file from configuration.
   *
   * @param config Excel generator configuration
   * @param outputPath Where to save the file
   * @throws IOException if writing Excel file fails
   */
  private void generateExcel(ExcelGeneratorConfig config, String outputPath) throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      for (ExcelGeneratorConfig.SheetConfig sheetConfig : config.sheets()) {
        createSheet(workbook, sheetConfig);
      }

      // Write to file
      try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
        workbook.write(fileOut);
      }

      System.out.println(
          "✅ Generated Excel file: " + outputPath + " with " + config.sheets().size() + " sheets");
    }
  }

  /**
   * Creates a single sheet with fake data.
   *
   * @param workbook The workbook to add the sheet to
   * @param sheetConfig Configuration for this sheet
   */
  private void createSheet(Workbook workbook, ExcelGeneratorConfig.SheetConfig sheetConfig) {
    Sheet sheet = workbook.createSheet(sheetConfig.name());

    // Create header row
    Row headerRow = sheet.createRow(0);
    for (int colIdx = 0; colIdx < sheetConfig.columns().size(); colIdx++) {
      Cell cell = headerRow.createCell(colIdx);
      cell.setCellValue(sheetConfig.columns().get(colIdx).name());
    }

    // Generate data rows
    for (int rowIdx = 1; rowIdx <= sheetConfig.rowCount(); rowIdx++) {
      Row dataRow = sheet.createRow(rowIdx);
      for (int colIdx = 0; colIdx < sheetConfig.columns().size(); colIdx++) {
        ExcelGeneratorConfig.ColumnDef columnDef = sheetConfig.columns().get(colIdx);
        Cell cell = dataRow.createCell(colIdx);
        String value = generateValue(columnDef, rowIdx);
        cell.setCellValue(value);
      }
    }

    // Auto-size columns
    for (int colIdx = 0; colIdx < sheetConfig.columns().size(); colIdx++) {
      sheet.autoSizeColumn(colIdx);
    }

    System.out.println(
        "  📄 Sheet '" + sheetConfig.name() + "' created with " + sheetConfig.rowCount() + " rows");
  }

  /**
   * Generates a fake value for a column based on its definition.
   *
   * @param columnDef Column definition
   * @param rowIndex Current row index (1-based)
   * @return Generated fake value
   */
  private String generateValue(ExcelGeneratorConfig.ColumnDef columnDef, int rowIndex) {
    // If allowed values are specified, randomly select one
    if (columnDef.allowedValues() != null && !columnDef.allowedValues().isEmpty()) {
      return columnDef.allowedValues().get(random.nextInt(columnDef.allowedValues().size()));
    }

    // Generate based on faker type
    String fakerType = columnDef.fakerType();
    if (fakerType == null || fakerType.isBlank()) {
      return "";
    }

    return switch (fakerType.toLowerCase()) {
      case "id", "uuid" -> faker.internet().uuid();
      case "sequence_id" -> String.format("%s%03d", "ID", rowIndex);

      case "firstname" -> faker.name().firstName();
      case "lastname" -> faker.name().lastName();
      case "fullname" -> faker.name().fullName();

      case "email" -> faker.internet().emailAddress();
      case "phone" -> faker.phoneNumber().phoneNumber();

      case "city" -> faker.address().city();
      case "country" -> faker.address().country();
      case "address" -> faker.address().fullAddress();

      case "product_name" -> faker.commerce().productName();
      case "department" -> faker.commerce().department();
      case "price" -> String.format("%.2f", faker.number().randomDouble(2, 10, 1000));
      case "category" -> faker.commerce().department();

      case "number" -> {
        int min = columnDef.min() != null ? columnDef.min() : 1;
        int max = columnDef.max() != null ? columnDef.max() : 100;
        yield String.valueOf(faker.number().numberBetween(min, max));
      }
      case "decimal" -> {
        int min = columnDef.min() != null ? columnDef.min() : 0;
        int max = columnDef.max() != null ? columnDef.max() : 1000;
        yield String.format("%.2f", faker.number().randomDouble(2, min, max));
      }
      case "rating" -> String.format("%.1f", faker.number().randomDouble(1, 1, 5));

      case "boolean" -> faker.bool().bool() ? "true" : "false";

      case "date" -> {
        Date fakeDate = faker.date().birthday(1, 365); // Within last year
        LocalDate localDate = fakeDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String format = columnDef.format() != null ? columnDef.format() : "yyyy-MM-dd";
        yield localDate.format(DateTimeFormatter.ofPattern(format));
      }
      case "past_date" -> {
        Date fakeDate = faker.date().past(365, java.util.concurrent.TimeUnit.DAYS);
        LocalDate localDate = fakeDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String format = columnDef.format() != null ? columnDef.format() : "yyyy-MM-dd";
        yield localDate.format(DateTimeFormatter.ofPattern(format));
      }

      case "status" -> {
        List<String> statuses = List.of("ACTIVE", "INACTIVE", "PENDING", "SUSPENDED");
        yield statuses.get(random.nextInt(statuses.size()));
      }
      case "order_status" -> {
        List<String> statuses = List.of("PENDING", "SHIPPED", "DELIVERED", "CANCELLED");
        yield statuses.get(random.nextInt(statuses.size()));
      }

      default -> faker.lorem().word();
    };
  }

  /**
   * Main method for standalone usage.
   *
   * @param args [0] = config YAML path, [1] = output Excel path
   */
  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println("Usage: java ExcelGenerator <config.yml> <output.xlsx>");
      System.exit(1);
    }

    ExcelGenerator generator = new ExcelGenerator();
    try {
      generator.generateFromConfig(args[0], args[1]);
    } catch (IOException e) {
      System.err.println("Error generating Excel: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
}
