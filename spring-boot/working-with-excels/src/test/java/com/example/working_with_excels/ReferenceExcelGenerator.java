package com.example.working_with_excels;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Generates reference_import.xlsx matching reference_import_mapping.yml.
 *
 * <p>
 * This Excel file tests ALL import features: column types, validations,
 * transformations, lookups, and custom SQL.
 */
public class ReferenceExcelGenerator {

    public static void main(String[] args) throws IOException {
        String outputPath = "src/main/resources/reference_import.xlsx";

        try (Workbook workbook = new XSSFWorkbook()) {
            createUsersSheet(workbook);
            createProductsSheet(workbook);
            createValidationTestSheet(workbook);
            createTransformationTestSheet(workbook);
            createFailSheetDemoSheet(workbook);

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
        }

        System.out.println("✅ Created: " + outputPath);
        System.out.println("Sheets: Users, Products, Validation Test, Transformation Test, Fail Sheet Demo");
    }

    private static void createUsersSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Users");
        CreationHelper createHelper = workbook.getCreationHelper();

        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));

        // Headers: User ID, Full Name, Email Address, Birth Date, Is Active, Account
        // Balance, Country, Status
        Row header = sheet.createRow(0);
        String[] headers = { "User ID", "Full Name", "Email Address", "Birth Date",
                "Is Active", "Account Balance", "Country", "Status" };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        // Data - matches lookup values in DDL
        Object[][] data = {
                { 1, "  JOHN DOE  ", "JOHN.DOE@EXAMPLE.COM", LocalDate.of(1990, 5, 15), true, 1234.56, "United States",
                        "ACTIVE" },
                { 2, "jane SMITH", "jane.smith@example.com", LocalDate.of(1985, 3, 22), true, 5678.90, "United Kingdom",
                        "ACTIVE" },
                { 3, "  bob   WILSON  ", "BOB@EXAMPLE.COM", LocalDate.of(1978, 11, 8), false, 100.00, "Germany",
                        "INACTIVE" },
                { 4, "alice brown", "alice.brown@test.com", LocalDate.of(2000, 1, 1), true, 999.99, "France",
                        "PENDING" },
                { 5, "charlie DAVIS", "charlie@example.com", LocalDate.of(1995, 7, 30), true, 0.0, "Spain",
                        "ARCHIVED" },
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            Object[] rowData = data[i];

            row.createCell(0).setCellValue((Integer) rowData[0]);
            row.createCell(1).setCellValue((String) rowData[1]);
            row.createCell(2).setCellValue((String) rowData[2]);
            Cell dateCell = row.createCell(3);
            dateCell.setCellValue(toDate((LocalDate) rowData[3]));
            dateCell.setCellStyle(dateStyle);
            row.createCell(4).setCellValue((Boolean) rowData[4]);
            row.createCell(5).setCellValue((Double) rowData[5]);
            row.createCell(6).setCellValue((String) rowData[6]);
            row.createCell(7).setCellValue((String) rowData[7]);
        }

        autoSizeColumns(sheet, headers.length);
    }

    private static void createProductsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Products");

        // Headers: Product Name, Description, Price, Category, SKU, Notes
        Row header = sheet.createRow(0);
        String[] headers = { "Product Name", "Description", "Price", "Category", "SKU", "Notes" };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        // Data - Categories match DDL lookup values
        Object[][] data = {
                { "  Laptop Pro  ", "High-end laptop computer", 1299.99, "Electronics", "LP-12345", "Bestseller" },
                { "wireless mouse", "Ergonomic wireless mouse", 49.99, "Computers", "WM-4567", "New arrival" },
                { "  MECHANICAL KEYBOARD  ", "RGB mechanical keyboard", 149.99, "Computers", "MK-78901", "Popular" },
                { "smartphone x", "Latest smartphone model", 899.99, "Phones", "SP-23456", "Hot item" },
                { "usb hub", "7-port USB 3.0 hub", 29.99, "Electronics", "UH-34567", "Essential" },
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            Object[] rowData = data[i];

            row.createCell(0).setCellValue((String) rowData[0]);
            row.createCell(1).setCellValue((String) rowData[1]);
            row.createCell(2).setCellValue((Double) rowData[2]);
            row.createCell(3).setCellValue((String) rowData[3]);
            row.createCell(4).setCellValue((String) rowData[4]);
            row.createCell(5).setCellValue((String) rowData[5]);
        }

        autoSizeColumns(sheet, headers.length);
    }

    private static void createValidationTestSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Validation Test");

        // Headers matching YAML columns
        Row header = sheet.createRow(0);
        String[] headers = { "ID", "Required Field", "Code", "Phone Number",
                "Postal Code", "Percentage", "Priority", "Username", "Email" };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        // Valid data rows
        Object[][] data = {
                { 1, "Valid Required", "ABC123", "+12345678901", "12345", 50.5, "HIGH", "johndoe", "test@example.com" },
                { 2, "Another Value", "XYZ", "+1234567890123", "12345-6789", 0.0, "low", "janesmith", "jane@test.com" },
                { 3, "Third Row", "QWERTY", "+9876543210", "54321", 100.0, "MEDIUM", "bobwilson", "bob@example.com" },
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            Object[] rowData = data[i];

            row.createCell(0).setCellValue((Integer) rowData[0]);
            row.createCell(1).setCellValue((String) rowData[1]);
            row.createCell(2).setCellValue((String) rowData[2]);
            row.createCell(3).setCellValue((String) rowData[3]);
            row.createCell(4).setCellValue((String) rowData[4]);
            row.createCell(5).setCellValue((Double) rowData[5]);
            row.createCell(6).setCellValue((String) rowData[6]);
            row.createCell(7).setCellValue((String) rowData[7]);
            row.createCell(8).setCellValue((String) rowData[8]);
        }

        autoSizeColumns(sheet, headers.length);
    }

    private static void createTransformationTestSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Transformation Test");

        // Headers matching YAML columns
        Row header = sheet.createRow(0);
        String[] headers = { "ID", "Uppercase Field", "Lowercase Field", "Title Case Field",
                "Trimmed Field", "Normalized Field", "Padded Left", "Padded Right",
                "Replaced Field", "Substring Field" };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        // Data with values that will be transformed
        Object[][] data = {
                { 1, "  hello world  ", "HELLO WORLD", "hello world",
                        "  trim me  ", "too    many   spaces", "123", "abc",
                        "replace-these-dashes", "this is a very long string that should be truncated" },
                { 2, "  test data  ", "TEST DATA", "test data",
                        "   another   ", "single  space", "7", "XY",
                        "more-dashes-here", "short one" },
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            Object[] rowData = data[i];

            row.createCell(0).setCellValue((Integer) rowData[0]);
            for (int j = 1; j < rowData.length; j++) {
                row.createCell(j).setCellValue((String) rowData[j]);
            }
        }

        autoSizeColumns(sheet, headers.length);
    }

    private static void createFailSheetDemoSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Fail Sheet Demo");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Required Field");

        // First row valid
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue(100);
        row1.createCell(1).setCellValue("Valid value");

        // Second row valid
        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue(101);
        row2.createCell(1).setCellValue("Another valid");

        autoSizeColumns(sheet, 2);
    }

    private static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
