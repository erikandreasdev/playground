package com.example.demo.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for ExcelGenerator.
 *
 * <p>Generates an e-commerce Excel file and verifies basic structure.
 */
class ExcelGeneratorTest {

  @TempDir File tempDir;

  @Test
  void generateFromConfig_shouldCreateEcommerceExcelFile() throws Exception {
    // Arrange
    ExcelGenerator generator = new ExcelGenerator();
    String configPath = "generator/ecommerce-generator.yml";
    File outputFile = new File(tempDir, "ecommerce-data.xlsx");

    // Act
    generator.generateFromConfig(configPath, outputFile.getAbsolutePath());

    // Assert
    assertThat(outputFile).exists().isFile();

    // Verify workbook structure
    try (Workbook workbook = new XSSFWorkbook(outputFile)) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(5);

      Sheet categoriesSheet = workbook.getSheet("Categories");
      assertThat(categoriesSheet).isNotNull();
      assertThat(categoriesSheet.getPhysicalNumberOfRows()).isEqualTo(6); // Header + 5 data rows

      Sheet customersSheet = workbook.getSheet("Customers");
      assertThat(customersSheet).isNotNull();
      assertThat(customersSheet.getPhysicalNumberOfRows()).isEqualTo(11); // Header + 10 data rows

      Sheet productsSheet = workbook.getSheet("Products");
      assertThat(productsSheet).isNotNull();
      assertThat(productsSheet.getPhysicalNumberOfRows()).isEqualTo(21); // Header + 20 data rows

      Sheet ordersSheet = workbook.getSheet("Orders");
      assertThat(ordersSheet).isNotNull();
      assertThat(ordersSheet.getPhysicalNumberOfRows()).isEqualTo(16); // Header + 15 data rows

      Sheet orderItemsSheet = workbook.getSheet("Order Items");
      assertThat(orderItemsSheet).isNotNull();
      assertThat(orderItemsSheet.getPhysicalNumberOfRows()).isEqualTo(31); // Header + 30 data rows

      System.out.println("✅ E-commerce Excel file generated successfully!");
      System.out.println("   Location: " + outputFile.getAbsolutePath());
      System.out.println("   Sheets: " + workbook.getNumberOfSheets());
      System.out.println(
          "   Categories: " + (categoriesSheet.getPhysicalNumberOfRows() - 1) + " rows");
      System.out.println(
          "   Customers: " + (customersSheet.getPhysicalNumberOfRows() - 1) + " rows");
      System.out.println("   Products: " + (productsSheet.getPhysicalNumberOfRows() - 1) + " rows");
      System.out.println("   Orders: " + (ordersSheet.getPhysicalNumberOfRows() - 1) + " rows");
      System.out.println(
          "   Order Items: " + (orderItemsSheet.getPhysicalNumberOfRows() - 1) + " rows");
    }
  }

  /**
   * Main method to generate the Excel file for manual testing.
   *
   * <p>Run this to create the e-commerce Excel file in src/main/resources.
   */
  public static void main(String[] args) throws IOException {
    ExcelGenerator generator = new ExcelGenerator();
    String configPath = "generator/ecommerce-generator.yml";
    String outputPath = "src/main/resources/examples/excel/ecommerce-data.xlsx";

    generator.generateFromConfig(configPath, outputPath);

    System.out.println("\n🎉 Excel file generated successfully!");
    System.out.println("📁 Location: " + outputPath);
    System.out.println(
        "\n📚 Next steps:\n"
            + "1. Validate and Load data:\n"
            + "   API Example:\n"
            + "   GET http://localhost:8080/api/excel/validate?\n"
            + "       excelFilename=examples/excel/ecommerce-data.xlsx&\n"
            + "       validationsFilename=validations/ecommerce-validation.yml&\n"
            + "       persist=true");
  }
}
