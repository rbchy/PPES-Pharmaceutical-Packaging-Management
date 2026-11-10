package com.ppes.db;

import com.ppes.model.FieldDef;
import com.ppes.model.FieldType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The single path every form value and every Excel cell goes through before it reaches MySQL.
 * These cases need no database (FK labels are covered by DatabaseSchemaIT).
 */
class FieldValueConverterTest {

    private static Object convert(FieldType type, String raw) throws Exception {
        return FieldValueConverter.convert(null, new FieldDef("f", "Field", type), raw);
    }

    @Test
    void blankAndNullBecomeSqlNull() throws Exception {
        assertNull(convert(FieldType.NUMBER, null));
        assertNull(convert(FieldType.TEXT, "   "));
        assertNull(convert(FieldType.DATE, ""));
    }

    @Test
    void numbersAndDecimals() throws Exception {
        assertEquals(50000L, convert(FieldType.NUMBER, " 50000 "));
        assertEquals(new BigDecimal("12.375"), convert(FieldType.DECIMAL, "12.375"));
    }

    @Test
    void textThatIsNotANumberIsRefusedWithTheFieldLabel() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> convert(FieldType.NUMBER, "12a"));
        assertTrue(e.getMessage().contains("Field") && e.getMessage().contains("12a"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> convert(FieldType.NUMBER, "12.5"), "NUMBER is whole numbers");
        assertThrows(IllegalArgumentException.class, () -> convert(FieldType.DECIMAL, "1,5"));
    }

    @Test
    void datesMustBeIso() throws Exception {
        assertEquals(Date.valueOf("2026-08-05"), convert(FieldType.DATE, "2026-08-05"));
        assertThrows(IllegalArgumentException.class, () -> convert(FieldType.DATE, "08/05/2026"));
    }

    @Test
    void timeAcceptsHoursAndMinutesWithoutSeconds() throws Exception {
        assertEquals(Time.valueOf("07:30:00"), convert(FieldType.TIME, "07:30"));
        assertEquals(Time.valueOf("15:00:45"), convert(FieldType.TIME, "15:00:45"));
        assertThrows(IllegalArgumentException.class, () -> convert(FieldType.TIME, "7.30"));
    }

    @Test
    void dateTimeAcceptsTheHtmlTSeparatorAndMissingSeconds() throws Exception {
        assertEquals(Timestamp.valueOf("2026-08-05 07:00:00"), convert(FieldType.DATETIME, "2026-08-05T07:00"));
        assertEquals(Timestamp.valueOf("2026-08-05 07:00:00"), convert(FieldType.DATETIME, "2026-08-05 07:00"));
        assertThrows(IllegalArgumentException.class, () -> convert(FieldType.DATETIME, "tomorrow"));
    }

    @Test
    void passwordsAreStoredHashedNeverPlain() throws Exception {
        Object stored = convert(FieldType.PASSWORD, "Line@2026");
        assertNotEquals("Line@2026", stored);
        assertTrue(PasswordUtil.verify("Line@2026", (String) stored));
    }

    @Test
    void aNumericForeignKeyIsUsedAsTheId() throws Exception {
        FieldDef fk = new FieldDef("product_id", "Product", FieldType.FK).refTable("product_master");
        assertEquals(3L, FieldValueConverter.convert(null, fk, "3"));
    }

    @Test
    void aForeignKeyToAnUnknownTableIsRefused() {
        FieldDef fk = new FieldDef("x_id", "X", FieldType.FK).refTable("no_such_table");
        assertThrows(IllegalArgumentException.class, () -> FieldValueConverter.convert(null, fk, "Some label"));
    }

    @Test
    void textAndChoicesAreTrimmedButOtherwiseKept() throws Exception {
        assertEquals("Line-1", convert(FieldType.SELECT, "  Line-1 "));
        assertEquals("<b>note</b>", convert(FieldType.TEXTAREA, "<b>note</b>"));
    }
}
