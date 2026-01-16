package com.example.working_with_excels.excel.domain.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

import com.example.working_with_excels.excel.domain.model.ColumnConfig;

import lombok.RequiredArgsConstructor;

/**
 * Domain service responsible for extracting typed values from Excel cells.
 *
 * <p>
 * This class encapsulates the logic for converting raw Excel cell values
 * to their appropriate Java types based on column configuration. For STRING
 * and EMAIL types, transformations are applied via the CellTransformer.
 */
@RequiredArgsConstructor
public class CellValueExtractor {

    private final CellTransformer cellTransformer;

    /**
     * Extracts a typed value from an Excel cell based on column configuration.
     *
     * @param cell      the Excel cell to extract the value from (may be null)
     * @param colConfig the column configuration specifying the expected type
     * @return the extracted value in the appropriate Java type, or null if the cell
     *         is null
     */
    public Object extractTypedValue(Cell cell, ColumnConfig colConfig) {
        if (cell == null) {
            return null;
        }

        return switch (colConfig.type()) {
            case DATE -> cell.getCellType() == CellType.NUMERIC
                    ? cell.getDateCellValue()
                    : null;
            case INTEGER -> cell.getCellType() == CellType.NUMERIC
                    ? (int) cell.getNumericCellValue()
                    : null;
            case DECIMAL -> cell.getCellType() == CellType.NUMERIC
                    ? cell.getNumericCellValue()
                    : null;
            case BOOLEAN -> cell.getCellType() == CellType.BOOLEAN
                    ? (cell.getBooleanCellValue() ? 1 : 0)
                    : null;
            case STRING, EMAIL -> cellTransformer.transform(cell, colConfig.transformations());
        };
    }
}
