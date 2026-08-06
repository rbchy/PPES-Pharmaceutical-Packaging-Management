package com.ppes.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads one sheet of an .xlsx file into a list of header->value maps. All
 * cell values are read as display strings (via DataFormatter), so the caller
 * (FieldValueConverter) does the actual type conversion — the same
 * conversion path used for manually-typed form values.
 *
 * If the workbook has a sheet whose name matches preferredSheetName (e.g. the
 * table key "employee"), that sheet is used — this lets one multi-sheet
 * workbook (like PPES_sample_data.xlsx, one sheet per table) be reused for
 * every table's Import button without picking a different file each time.
 * Otherwise it falls back to the first sheet, so plain single-sheet exports
 * still work.
 */
public final class ExcelImportUtil {

    private ExcelImportUtil() {
    }

    public static List<Map<String, String>> readSheetAsMaps(String filePath, String preferredSheetName)
            throws IOException {
        List<Map<String, String>> result = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = WorkbookFactory.create(fis)) {

            Sheet sheet = null;
            if (preferredSheetName != null) {
                sheet = wb.getSheet(preferredSheetName);
            }
            if (sheet == null) {
                sheet = wb.getSheetAt(0);
            }
            if (sheet == null) {
                return result;
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return result;
            }

            List<String> headers = new ArrayList<>();
            int lastCol = headerRow.getLastCellNum();
            for (int c = 0; c < lastCol; c++) {
                Cell cell = headerRow.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                headers.add(cell.getStringCellValue().trim());
            }

            DataFormatter formatter = new DataFormatter();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, String> map = new LinkedHashMap<>();
                boolean allBlank = true;
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String val = formatter.formatCellValue(cell).trim();
                    if (!val.isEmpty()) {
                        allBlank = false;
                    }
                    map.put(headers.get(c), val);
                }
                if (!allBlank) {
                    result.add(map);
                }
            }
        }
        return result;
    }
}
