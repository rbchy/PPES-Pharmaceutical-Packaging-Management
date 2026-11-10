package com.ppes.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The registry drives every generic CRUD screen, the Excel import and the base schema. */
class TableRegistryTest {

    @Test
    void allEighteenTablesAreRegistered() {
        assertEquals(18, TableRegistry.TABLES.size(), TableRegistry.TABLES.keySet().toString());
        for (String t : List.of("product_master", "customer_order", "employee", "shift_master", "raw_material_master",
                "users", "batch_lot", "raw_material_receiving", "startup_checklist", "production_target",
                "blister_production", "ipc_inspection", "pallet", "shipper_label", "reconciliation",
                "shift_handover", "in_process_waste", "pallet_placard")) {
            assertTrue(TableRegistry.TABLES.containsKey(t), "missing table " + t);
        }
    }

    @Test
    void everyTableHasAPrimaryKeyFieldsAndARowLabel() {
        for (TableDef t : TableRegistry.TABLES.values()) {
            assertNotNull(t.pk, t.key);
            assertFalse(t.fields.isEmpty(), t.key + " has no fields");
            assertFalse(t.fields.containsKey(t.pk), t.key + ": the primary key must not be an editable field");
            assertTrue(t.displayColumn != null || t.displayExpr != null, t.key + " has no row label");
            assertNotNull(t.label, t.key);
            assertNotNull(t.labelBn, t.key + " has no Bangla label");
        }
    }

    @Test
    void everyForeignKeyPointsAtARegisteredTableDeclaredBeforeIt() {
        List<String> order = List.copyOf(TableRegistry.TABLES.keySet());
        for (TableDef t : TableRegistry.TABLES.values()) {
            for (FieldDef f : t.fields.values()) {
                if (f.type == FieldType.FK) {
                    assertNotNull(f.refTable, t.key + "." + f.name + " has no refTable");
                    assertTrue(TableRegistry.TABLES.containsKey(f.refTable), t.key + "." + f.name + " -> " + f.refTable);
                    assertTrue(order.indexOf(f.refTable) < order.indexOf(t.key),
                            t.key + " must come after " + f.refTable + " so the schema can be created in order");
                }
                if (f.type == FieldType.LOOKUP) {
                    assertNotNull(f.refTable, t.key + "." + f.name);
                    assertNotNull(f.refColumn, t.key + "." + f.name);
                }
            }
        }
    }

    @Test
    void everyDropDownHasOptions() {
        for (TableDef t : TableRegistry.TABLES.values()) {
            for (FieldDef f : t.fields.values()) {
                if (f.type == FieldType.SELECT) {
                    assertNotNull(f.options, t.key + "." + f.name);
                    assertFalse(f.options.isEmpty(), t.key + "." + f.name);
                    assertEquals(f.options.size(), f.options.stream().distinct().count(), t.key + "." + f.name + " has duplicates");
                }
            }
        }
    }

    @Test
    void columnNamesAreSafeSqlIdentifiers() {
        for (TableDef t : TableRegistry.TABLES.values()) {
            assertTrue(t.key.matches("[a-z_]+"), t.key);
            t.fields.keySet().forEach(c -> assertTrue(c.matches("[a-z0-9_]+"), t.key + "." + c));
        }
    }

    @Test
    void theMenuOnlyListsRegisteredTablesEachOnce() {
        long listed = 0;
        for (Map.Entry<String, List<String>> group : TableRegistry.MENU.entrySet()) {
            for (String key : group.getValue()) {
                assertTrue(TableRegistry.TABLES.containsKey(key), "menu '" + group.getKey() + "' lists unknown table " + key);
                listed++;
            }
        }
        long distinct = TableRegistry.MENU.values().stream().flatMap(List::stream).distinct().count();
        assertEquals(distinct, listed, "a table appears in the menu twice");
    }

    @Test
    void theUsersPasswordColumnIsAPasswordField() {
        FieldDef pw = TableRegistry.TABLES.get("users").fields.get("password_hash");
        assertNotNull(pw, "users.password_hash");
        assertEquals(FieldType.PASSWORD, pw.type, "so it is hashed on save, never stored as typed");
    }
}
