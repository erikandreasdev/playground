package com.example.working_with_excels.util;

import net.datafaker.Faker;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility script to generate a random Excel file with mixed data types.
 * <p>
 * This class is designed to be run as a standalone Java application (via main
 * method)
 * to produce test data for development purposes.
 * </p>
 */
public class ExcelGenerator {

    private static final int ROW_COUNT = 50;
    private static final String FILE_NAME = "generated_test_data.xlsx";

    public static void main(String[] args) {
        System.out.println("Starting Excel Generation...");
        Faker faker = new Faker();

        try (Workbook workbook = new XSSFWorkbook()) {
            createUsersSheet(workbook, faker);
            createProductsSheet(workbook, faker);
            createOrdersSheet(workbook, faker);

            try (FileOutputStream fileOut = new FileOutputStream(FILE_NAME)) {
                workbook.write(fileOut);
                System.out.println("File created: " + FILE_NAME);
            }
        } catch (IOException e) {
            System.err.println("Error creating excel file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createUsersSheet(Workbook workbook, Faker faker) {
        Sheet sheet = workbook.createSheet("Users");
        String[] headers = { "User ID", "Full Name", "Email Address", "Birth Date", "Is Active", "Account Balance" };
        createHeader(workbook, sheet, headers);

        for (int i = 1; i <= ROW_COUNT; i++) {
            Row row = sheet.createRow(i);
            // Number (Integer)
            row.createCell(0).setCellValue(i);
            // String
            row.createCell(1).setCellValue(faker.name().fullName());
            // String (Email)
            row.createCell(2).setCellValue(faker.internet().emailAddress());
            // Date
            Cell dateCell = row.createCell(3);
            dateCell.setCellValue(faker.date().birthday(18, 90));
            setDateStyle(workbook, dateCell);
            // Boolean
            row.createCell(4).setCellValue(faker.bool().bool());
            // Number (Double)
            row.createCell(5).setCellValue(faker.number().randomDouble(2, 0, 10000));
        }
        autoSizeColumns(sheet, headers.length);
    }

    private static void createProductsSheet(Workbook workbook, Faker faker) {
        Sheet sheet = workbook.createSheet("Products");
        String[] headers = { "Product ID", "Product Name", "Description", "Price", "Category", "Stock Count" };
        createHeader(workbook, sheet, headers);

        for (int i = 1; i <= ROW_COUNT; i++) {
            Row row = sheet.createRow(i);
            // Number (Long/ID)
            row.createCell(0).setCellValue(1000 + i);
            // String
            row.createCell(1).setCellValue(faker.commerce().productName());
            // String
            row.createCell(2).setCellValue(faker.lorem().sentence());
            // Number (Double)
            row.createCell(3).setCellValue(Double.parseDouble(faker.commerce().price()));
            // String
            row.createCell(4).setCellValue(faker.commerce().department());
            // Number (Integer)
            row.createCell(5).setCellValue(faker.number().numberBetween(0, 500));
        }
        autoSizeColumns(sheet, headers.length);
    }

    private static void createOrdersSheet(Workbook workbook, Faker faker) {
        Sheet sheet = workbook.createSheet("Orders");
        String[] headers = { "Order Ref", "Customer Email", "Order Date", "Total Amount", "Shipped" };
        createHeader(workbook, sheet, headers);

        for (int i = 1; i <= ROW_COUNT; i++) {
            Row row = sheet.createRow(i);
            // String (UUID/Ref)
            row.createCell(0).setCellValue(faker.internet().uuid());
            // String (Email)
            row.createCell(1).setCellValue(faker.internet().emailAddress());
            // Date
            Cell dateCell = row.createCell(2);
            dateCell.setCellValue(faker.date().past(365, TimeUnit.DAYS));
            setDateStyle(workbook, dateCell);
            // Number (Double)
            row.createCell(3).setCellValue(faker.number().randomDouble(2, 20, 500));
            // Boolean
            row.createCell(4).setCellValue(faker.bool().bool());
        }
        autoSizeColumns(sheet, headers.length);
    }

    private static void createHeader(Workbook workbook, Sheet sheet, String[] headers) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private static void setDateStyle(Workbook workbook, Cell cell) {
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
        cell.setCellStyle(dateStyle);
    }

    private static void autoSizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
