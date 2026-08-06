package com.ppes.db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Simple salted SHA-256 password hashing (no external crypto dependency).
 * Stored format: "sha256$<base64-salt>$<base64-hash>"
 *
 * NOTE: This is adequate for an internal tool but is not as strong as
 * bcrypt/scrypt/Argon2. If this app is exposed beyond a trusted internal
 * network, swap this for a proper password-hashing library (e.g. jBCrypt).
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] digest = digest(plainPassword, salt);
        return "sha256$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(digest);
    }

    public static boolean verify(String plainPassword, String storedValue) {
        if (storedValue == null || !storedValue.startsWith("sha256$")) {
            return false;
        }
        String[] parts = storedValue.split("\\$");
        if (parts.length != 3) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expected = Base64.getDecoder().decode(parts[2]);
        byte[] actual = digest(plainPassword, salt);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] digest(String plainPassword, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(plainPassword.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
