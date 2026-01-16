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
 * Utility to generate a test Excel file for import testing.
 *
 * <p>
 * Run this class to create test_import.xlsx in src/main/resources.
 */
public class TestExcelGenerator {

    public static void main(String[] args) throws IOException {
        String outputPath = "src/main/resources/test_import.xlsx";

        try (Workbook workbook = new XSSFWorkbook()) {
            createUsersSheet(workbook);
            createProductsSheet(workbook);

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
        }

        System.out.println("✅ Created: " + outputPath);
        System.out.println("Run the import test to see DRY_RUN output!");
    }

    private static void createUsersSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Users");
        CreationHelper createHelper = workbook.getCreationHelper();

        // Date cell style
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));

        // Header row
        Row header = sheet.createRow(0);
        String[] headers = { "User ID", "Full Name", "Email Address", "Birth Date", "Is Active", "Account Balance" };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        // Data rows
        Object[][] data = {
                { 1, "  john DOE  ", "JOHN.DOE@EXAMPLE.COM", LocalDate.of(1990, 5, 15), true, 1234.56 },
                { 2, "jane SMITH", "jane.smith@example.com", LocalDate.of(1985, 3, 22), true, 5678.90 },
                { 3, "  bob WILSON  ", "BOB@EXAMPLE.COM", LocalDate.of(1978, 11, 8), false, 100.00 },
                { 4, "alice BROWN", "alice.brown@test.com", LocalDate.of(2000, 1, 1), true, 999.99 },
                { 5, "charlie DAVIS", "", LocalDate.of(1995, 7, 30), true, 0.0 }, // Empty email - will fail validation
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            Object[] rowData = data[i];

            row.createCell(0).setCellValue((Integer) rowData[0]); // User ID
            row.createCell(1).setCellValue((String) rowData[1]); // Full Name
            row.createCell(2).setCellValue((String) rowData[2]); // Email
            Cell dateCell = row.createCell(3);
            dateCell.setCellValue(toDate((LocalDate) rowData[3]));
            dateCell.setCellStyle(dateStyle);
            row.createCell(4).setCellValue((Boolean) rowData[4]); // Is Active
            row.createCell(5).setCellValue((Double) rowData[5]); // Balance
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static void createProductsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Products");

        // Header row
        Row header = sheet.createRow(0);
        String[] headers = { "Product ID", "Product Name", "Description", "Price", "Category", "Stock Count" };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        // Data rows
        Object[][] data = {
                { 101, "  laptop pro  ", "High-end laptop", 1299.99, "Electronics", 50 },
                { 102, "wireless mouse", "Ergonomic mouse", 49.99, "Electronics", 200 },
                { 103, "  MECHANICAL KEYBOARD  ", "RGB keyboard", 149.99, "Electronics", 75 },
                { 104, "monitor 27\"", "4K display", 399.99, "Electronics", 30 },
                { 105, "USB Hub", "7-port hub", 29.99, "Accessories", 500 },
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            Object[] rowData = data[i];

            row.createCell(0).setCellValue((Integer) rowData[0]);
            row.createCell(1).setCellValue((String) rowData[1]);
            row.createCell(2).setCellValue((String) rowData[2]);
            row.createCell(3).setCellValue((Double) rowData[3]);
            row.createCell(4).setCellValue((String) rowData[4]);
            row.createCell(5).setCellValue((Integer) rowData[5]);
        }
    }

    private static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
