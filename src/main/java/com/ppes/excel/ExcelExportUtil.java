package com.ppes.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/** Writes a simple single-sheet .xlsx file: a bold header row followed by data rows. */
public final class ExcelExportUtil {

    private ExcelExportUtil() {
    }

    public static void export(String filePath, String sheetName, List<String> headers, List<Object[]> rows)
            throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Object[] rowData : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int c = 0; c < rowData.length; c++) {
                    Cell cell = row.createCell(c);
                    Object v = rowData[c];
                    if (v == null) {
                        cell.setBlank();
                    } else if (v instanceof Number) {
                        cell.setCellValue(((Number) v).doubleValue());
                    } else {
                        cell.setCellValue(v.toString());
                    }
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                try {
                    sheet.autoSizeColumn(i);
                } catch (Exception ignored) {
                    // autoSizeColumn needs AWT font metrics; if that ever
                    // fails in a headless/CI environment, just skip sizing.
                }
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                wb.write(fos);
            }
        }
    }
}
