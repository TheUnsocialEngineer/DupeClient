package com.dupeclient.client.core.session;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

final class PayloadHmac {
    private PayloadHmac() {
    }

    static boolean verifySha256Hmac(String canonicalPayload, String signatureHex, byte[] key) {
        if (canonicalPayload == null || signatureHex == null || key == null || key.length == 0) {
            return false;
        }
        String expected = hmacSha256Hex(canonicalPayload, key);
        return constantTimeEquals(expected, signatureHex.trim().toLowerCase(Locale.ROOT));
    }

    static String hmacSha256Hex(String payload, byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return toHex(raw);
        } catch (Exception ex) {
            return "";
        }
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return "";
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int v = b & 0xFF;
            if (v < 16) out.append('0');
            out.append(Integer.toHexString(v));
        }
        return out.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
