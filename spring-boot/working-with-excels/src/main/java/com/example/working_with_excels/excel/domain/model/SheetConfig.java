package com.example.working_with_excels.excel.domain.model;

import java.util.List;

/**
 * Value object representing configuration for a single Excel sheet.
 *
 * <p>
 * This record defines the expected sheet name and the ordered list
 * of column configurations that the sheet should contain.
 *
 * @param name    the expected name of the Excel sheet
 * @param columns the ordered list of column configurations
 */
public record SheetConfig(String name, List<ColumnConfig> columns) {
}
