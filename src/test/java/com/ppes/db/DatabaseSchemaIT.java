package com.ppes.db;

import com.ppes.model.FieldDef;
import com.ppes.model.FieldType;
import com.ppes.model.TableDef;
import com.ppes.model.TableRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs against a real MySQL loaded with database/*.sql (CI does this; locally set PPES_DB_HOST etc.).
 * Skipped when PPES_DB_HOST is not set, so a plain "mvn test" on a laptop without MySQL still passes.
 */
class DatabaseSchemaIT {

    @BeforeAll
    static void needsADatabase() {
        String host = System.getenv("PPES_DB_HOST");
        assumeTrue(host != null && !host.isEmpty(), "PPES_DB_HOST not set - skipping database tests");
    }

    private static Set<String> columns(Connection c, String table) throws Exception {
        Set<String> cols = new HashSet<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) cols.add(rs.getString(1));
            }
        }
        return cols;
    }

    private static long count(Connection c, String sql) throws Exception {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    @Test
    void everyRegistryTableAndColumnExists() throws Exception {
        try (Connection c = Db.getConnection()) {
            for (TableDef t : TableRegistry.TABLES.values()) {
                Set<String> cols = columns(c, t.key);
                assertFalse(cols.isEmpty(), "table " + t.key + " is missing");
                assertTrue(cols.contains(t.pk), t.key + "." + t.pk);
                for (String f : t.fields.keySet()) {
                    assertTrue(cols.contains(f), t.key + "." + f + " is missing (screen would fail to save)");
                }
            }
        }
    }

    @Test
    void theModuleTablesExist() throws Exception {
        try (Connection c = Db.getConnection()) {
            for (String t : List.of("carton_tracking_header", "carton_tracking_hourly", "lot_closing",
                    "shift_production_log", "work_order_packing_spec")) {
                assertFalse(columns(c, t).isEmpty(), "module table " + t + " is missing");
            }
        }
    }

    @Test
    void sampleDataIsLoaded() throws Exception {
        try (Connection c = Db.getConnection()) {
            for (String t : List.of("product_master", "customer_order", "employee", "shift_master", "raw_material_master",
                    "users", "batch_lot", "raw_material_receiving", "startup_checklist", "production_target",
                    "blister_production", "ipc_inspection", "pallet", "shipper_label", "reconciliation", "shift_handover")) {
                assertTrue(count(c, "SELECT COUNT(*) FROM `" + t + "`") >= 10, t + " has fewer than 10 sample rows");
            }
        }
    }

    /** The exact list query CrudPanel builds for each screen, including the FK label joins. */
    @Test
    void everyScreensListQueryRuns() throws Exception {
        try (Connection c = Db.getConnection(); Statement st = c.createStatement()) {
            for (TableDef def : TableRegistry.TABLES.values()) {
                StringBuilder select = new StringBuilder("SELECT t.`").append(def.pk).append("` AS pk_val");
                StringBuilder joins = new StringBuilder();
                int alias = 0;
                for (FieldDef f : def.fields.values()) {
                    if (f.type == FieldType.FK) {
                        String a = "f" + (++alias);
                        TableDef ref = TableRegistry.TABLES.get(f.refTable);
                        select.append(", (").append(ref.displayExprFor(a)).append(") AS `col_").append(f.name).append('`');
                        joins.append(" LEFT JOIN `").append(f.refTable).append("` ").append(a)
                                .append(" ON t.`").append(f.name).append("` = ").append(a).append(".`").append(ref.pk).append('`');
                    } else {
                        select.append(", t.`").append(f.name).append("` AS `col_").append(f.name).append('`');
                    }
                }
                select.append(" FROM `").append(def.key).append("` t").append(joins).append(" LIMIT 5");
                try (ResultSet rs = st.executeQuery(select.toString())) {
                    assertEquals(def.fields.size() + 1, rs.getMetaData().getColumnCount(), def.key);
                } catch (Exception e) {
                    fail(def.key + " list query failed: " + e.getMessage() + "\n" + select);
                }
            }
        }
    }

    @Test
    void everySampleForeignKeyResolvesToARow() throws Exception {
        try (Connection c = Db.getConnection()) {
            for (TableDef t : TableRegistry.TABLES.values()) {
                for (FieldDef f : t.fields.values()) {
                    if (f.type != FieldType.FK) continue;
                    TableDef ref = TableRegistry.TABLES.get(f.refTable);
                    long orphans = count(c, "SELECT COUNT(*) FROM `" + t.key + "` t LEFT JOIN `" + ref.key + "` r ON t.`"
                            + f.name + "` = r.`" + ref.pk + "` WHERE t.`" + f.name + "` IS NOT NULL AND r.`" + ref.pk + "` IS NULL");
                    assertEquals(0, orphans, t.key + "." + f.name + " points at missing " + ref.key + " rows");
                }
            }
        }
    }

    @Test
    void anExcelLabelIsResolvedToItsId() throws Exception {
        try (Connection c = Db.getConnection()) {
            TableDef products = TableRegistry.TABLES.get("product_master");
            long id;
            String label;
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT product_id, (" + products.displayExprFor("p") + ") FROM product_master p ORDER BY product_id LIMIT 1")) {
                assertTrue(rs.next());
                id = rs.getLong(1);
                label = rs.getString(2);
            }
            FieldDef fk = TableRegistry.TABLES.get("batch_lot").fields.get("product_id");
            assertEquals(id, FieldValueConverter.convert(c, fk, label));
            assertThrows(IllegalArgumentException.class, () -> FieldValueConverter.convert(c, fk, "No such product"));
        }
    }

    @Test
    void sampleUsersHaveHashedPasswordsAndTheLoginQueryWorks() throws Exception {
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT username, password_hash, role FROM users WHERE username = ?")) {
            ps.setString(1, "admin");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "admin user");
                assertTrue(rs.getString("password_hash").startsWith("sha256$"), "never store plain passwords");
                assertFalse(PasswordUtil.verify("wrong-password", rs.getString("password_hash")));
            }
            assertEquals(0, count(c, "SELECT COUNT(*) FROM users WHERE password_hash NOT LIKE 'sha256$%'"));
        }
    }

    @Test
    void theDashboardQueriesRun() throws Exception {
        try (Connection c = Db.getConnection()) {
            assertTrue(count(c, "SELECT COUNT(*) FROM customer_order WHERE status = 'Pending'") >= 0);
            assertTrue(count(c, "SELECT COALESCE(SUM(good_qty),0) FROM blister_production") > 0);
            assertTrue(count(c, "SELECT COUNT(*) FROM ipc_inspection WHERE tablet_check='Fail' OR seal_check='Fail' OR coding_check='Fail'") >= 0);
        }
    }
}
