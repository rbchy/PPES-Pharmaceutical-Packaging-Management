package com.ppes.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Salted SHA-256 login hashes, shared by the desktop and web apps. */
class PasswordUtilTest {

    @Test
    void theRightPasswordVerifies() {
        String stored = PasswordUtil.hash("Line@2026");
        assertTrue(PasswordUtil.verify("Line@2026", stored));
    }

    @Test
    void aWrongPasswordIsRejected() {
        String stored = PasswordUtil.hash("Line@2026");
        assertFalse(PasswordUtil.verify("line@2026", stored), "passwords are case-sensitive");
        assertFalse(PasswordUtil.verify("", stored));
    }

    @Test
    void storedFormatIsSha256SaltHash() {
        String stored = PasswordUtil.hash("secret");
        String[] parts = stored.split("\\$");
        assertEquals(3, parts.length, stored);
        assertEquals("sha256", parts[0]);
        assertEquals(16, java.util.Base64.getDecoder().decode(parts[1]).length, "16-byte salt");
        assertEquals(32, java.util.Base64.getDecoder().decode(parts[2]).length, "SHA-256 digest");
        assertFalse(stored.contains("secret"), "the plain password must not appear in the stored value");
    }

    @Test
    void theSamePasswordGetsADifferentSaltEachTime() {
        String a = PasswordUtil.hash("same");
        String b = PasswordUtil.hash("same");
        assertNotEquals(a, b, "a random salt means equal passwords do not produce equal hashes");
        assertTrue(PasswordUtil.verify("same", a));
        assertTrue(PasswordUtil.verify("same", b));
    }

    @Test
    void malformedOrMissingStoredValuesNeverVerify() {
        assertFalse(PasswordUtil.verify("x", null));
        assertFalse(PasswordUtil.verify("x", "x"), "plain-text value in the users table");
        assertFalse(PasswordUtil.verify("x", "md5$abc$def"));
        assertFalse(PasswordUtil.verify("x", "sha256$onlyonepart"));
    }

    @Test
    void unicodePasswordsWork() {
        String stored = PasswordUtil.hash("পাসওয়ার্ড-১২৩");
        assertTrue(PasswordUtil.verify("পাসওয়ার্ড-১২৩", stored));
        assertFalse(PasswordUtil.verify("পাসওয়ার্ড-১২", stored));
    }
}
