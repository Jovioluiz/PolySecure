package com.polysecure.security.anonymization;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Anonymizers {

    private Anonymizers() {}

    public static String hashSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // "user@domain.com" → "u***@***.com"
    public static String truncateDomain(String value) {
        int at = value.indexOf('@');
        if (at < 0) return "***";
        int dot = value.lastIndexOf('.');
        String tld = dot > at ? value.substring(dot) : "";
        return value.charAt(0) + "***@***" + tld;
    }

    // Masks the last half of digit characters: "99999-1234" → "99999-****"
    public static String maskLastDigits(String value) {
        char[] chars = value.toCharArray();
        int digits = 0;
        for (char c : chars) if (Character.isDigit(c)) digits++;
        int mask = Math.max(1, digits / 2);
        int masked = 0;
        for (int i = chars.length - 1; i >= 0 && masked < mask; i--) {
            if (Character.isDigit(chars[i])) { chars[i] = '*'; masked++; }
        }
        return new String(chars);
    }

    // Consistent but irreversible: "Alice" → "Usr_a3f5c2b1"
    public static String pseudonymize(String value) {
        String hash = hashSha256(value);
        return "Usr_" + hash.substring(0, 8);
    }

    public static String redact(@SuppressWarnings("unused") String value) {
        return "***";
    }
}
