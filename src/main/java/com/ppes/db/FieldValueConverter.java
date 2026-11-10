package com.ppes.db;

import com.ppes.model.FieldDef;
import com.ppes.model.TableDef;
import com.ppes.model.TableRegistry;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converts a raw string (from a form field or an Excel cell) into the Java
 * object type appropriate for a JDBC PreparedStatement parameter, based on
 * the column's FieldDef. Shared by the manual "Save" button and the Excel
 * import feature so both go through identical validation/conversion rules.
 *
 * Throws IllegalArgumentException (unchecked) for bad input, so callers can
 * catch it and show a friendly message without every call site declaring it.
 */
public final class FieldValueConverter {

    private FieldValueConverter() {
    }

    public static Object convert(Connection conn, FieldDef f, String rawInput) throws SQLException {
        if (rawInput == null) {
            return null;
        }
        String raw = rawInput.trim();
        if (raw.isEmpty()) {
            return null;
        }

        switch (f.type) {
            case NUMBER:
                try {
                    return Long.parseLong(raw);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("\"" + f.label + "\" এর মান একটি সংখ্যা হতে হবে: " + raw);
                }

            case DECIMAL:
                try {
                    return new BigDecimal(raw);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("\"" + f.label + "\" এর মান একটি সংখ্যা (দশমিকসহ) হতে হবে: " + raw);
                }

            case DATE:
                try {
                    return java.sql.Date.valueOf(raw);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("\"" + f.label + "\" এর তারিখ অবশ্যই yyyy-MM-dd ফরম্যাটে দিন: " + raw);
                }

            case TIME:
                try {
                    return java.sql.Time.valueOf(normalizeTime(raw));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("\"" + f.label + "\" এর সময় অবশ্যই HH:mm বা HH:mm:ss ফরম্যাটে দিন: " + raw);
                }

            case DATETIME:
                try {
                    return java.sql.Timestamp.valueOf(normalizeDateTime(raw));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "\"" + f.label + "\" এর তারিখ-সময় অবশ্যই yyyy-MM-dd HH:mm ফরম্যাটে দিন: " + raw);
                }

            case FK:
                return resolveFk(conn, f, raw);

            case PASSWORD:
                return PasswordUtil.hash(raw);

            case SELECT:
            case LOOKUP:
            case TEXT:
            case TEXTAREA:
            default:
                return raw;
        }
    }

    private static String normalizeTime(String raw) {
        if (raw.matches("\\d{1,2}:\\d{2}")) {
            return raw + ":00";
        }
        return raw;
    }

    private static String normalizeDateTime(String raw) {
        String v = raw.replace('T', ' ');
        if (v.matches("\\d{4}-\\d{2}-\\d{2} \\d{1,2}:\\d{2}")) {
            v = v + ":00";
        }
        return v;
    }

    /** Accepts either a numeric primary-key value, or the referenced row's display label (used by Excel import). */
    private static Long resolveFk(Connection conn, FieldDef f, String raw) throws SQLException {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            // not a plain id - fall through to label lookup below
        }

        TableDef refDef = TableRegistry.TABLES.get(f.refTable);
        if (refDef == null) {
            throw new IllegalArgumentException("Unknown reference table for field \"" + f.label + "\": " + f.refTable);
        }
        String expr = refDef.displayExprFor("`" + refDef.key + "`");
        String sql = "SELECT `" + refDef.pk + "` FROM `" + refDef.key + "` WHERE (" + expr + ") = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, raw);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new IllegalArgumentException(
                "\"" + f.label + "\" এর মান \"" + raw + "\" " + f.refTable + " টেবিলে খুঁজে পাওয়া যায়নি।");
    }
}
