package com.ppes.excel;

import com.ppes.db.FieldValueConverter;
import com.ppes.model.FieldDef;
import com.ppes.model.FieldType;
import com.ppes.model.TableDef;
import com.ppes.model.TableRegistry;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Export -> import must give back what was exported, and the shipped sample workbook must import cleanly. */
class ExcelRoundTripTest {

    private static final String SAMPLE = "sample_data/PPES_sample_data.xlsx";

    @Test
    void exportThenImportGivesTheSameValues(@TempDir Path dir) throws Exception {
        String file = dir.resolve("product_master.xlsx").toString();
        List<String> headers = List.of("product_code", "product_name", "pack_size", "note");
        List<Object[]> rows = Arrays.asList(
                new Object[]{"PRD-001", "Paracetamol 500 mg", 100, "নমুনা"},
                new Object[]{"PRD-002", "Metformin 850 mg", 30, null});

        ExcelExportUtil.export(file, "product_master", headers, rows);
        List<Map<String, String>> back = ExcelImportUtil.readSheetAsMaps(file, "product_master");

        assertEquals(2, back.size());
        assertEquals(headers, List.copyOf(back.get(0).keySet()));
        assertEquals("PRD-001", back.get(0).get("product_code"));
        assertEquals("100", back.get(0).get("pack_size"), "whole numbers must not come back as 100.0");
        assertEquals("নমুনা", back.get(0).get("note"));
        assertEquals("", back.get(1).get("note"));
    }

    @Test
    void blankRowsAreSkipped(@TempDir Path dir) throws Exception {
        String file = dir.resolve("x.xlsx").toString();
        ExcelExportUtil.export(file, "x", List.of("a"), Arrays.asList(new Object[]{"1"}, new Object[]{null}, new Object[]{"3"}));
        List<Map<String, String>> back = ExcelImportUtil.readSheetAsMaps(file, "x");
        assertEquals(2, back.size());
        assertEquals("3", back.get(1).get("a"));
    }

    @Test
    void anUnknownSheetNameFallsBackToTheFirstSheet(@TempDir Path dir) throws Exception {
        String file = dir.resolve("x.xlsx").toString();
        ExcelExportUtil.export(file, "employee", List.of("employee_name"), List.<Object[]>of(new Object[]{"A"}));
        assertEquals("A", ExcelImportUtil.readSheetAsMaps(file, "no_such_sheet").get(0).get("employee_name"));
    }

    @Test
    void everySampleSheetMatchesTheRegistryAndEveryValueConverts() throws Exception {
        int sheets;
        try (FileInputStream in = new FileInputStream(SAMPLE); Workbook wb = WorkbookFactory.create(in)) {
            sheets = wb.getNumberOfSheets();
            for (int i = 0; i < sheets; i++) {
                String table = wb.getSheetName(i);
                TableDef def = TableRegistry.TABLES.get(table);
                assertNotNull(def, "sheet " + table + " is not a registered table");

                List<Map<String, String>> rows = ExcelImportUtil.readSheetAsMaps(SAMPLE, table);
                assertTrue(rows.size() >= 10, table + " has only " + rows.size() + " sample rows");

                for (Map<String, String> row : rows) {
                    for (Map.Entry<String, String> cell : row.entrySet()) {
                        FieldDef f = def.fields.get(cell.getKey());
                        assertNotNull(f, table + ": column " + cell.getKey() + " is not in the registry");
                        if (f.type == FieldType.FK && !cell.getValue().matches("\\d+")) {
                            continue; // label lookups need the database: DatabaseSchemaIT
                        }
                        Object v = FieldValueConverter.convert(null, f, cell.getValue());
                        if (f.type == FieldType.SELECT && v != null) {
                            assertTrue(f.options.contains(v), table + "." + f.name + " = '" + v + "' is not one of " + f.options);
                        }
                    }
                    for (FieldDef f : def.fields.values()) {
                        if (f.required && row.containsKey(f.name)) {
                            assertFalse(row.get(f.name).isEmpty(), table + "." + f.name + " is required but blank");
                        }
                    }
                }
            }
        }
        assertEquals(16, sheets, "one sheet per table that ships with sample data");
    }
}
