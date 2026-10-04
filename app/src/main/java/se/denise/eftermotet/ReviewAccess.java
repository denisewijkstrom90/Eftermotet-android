package se.denise.eftermotet;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Only a supplied high-entropy review code grants the separate reviewer entitlement. */
final class ReviewAccess {
    static boolean accepts(String code, String expectedHash) {
        if (code == null || expectedHash == null || !expectedHash.matches("[0-9a-f]{64}")) return false;
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[0-9A-F]{32}")) return false;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format(Locale.ROOT, "%02x", b & 255));
            return MessageDigest.isEqual(hex.toString().getBytes(StandardCharsets.US_ASCII), expectedHash.getBytes(StandardCharsets.US_ASCII));
        } catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}
