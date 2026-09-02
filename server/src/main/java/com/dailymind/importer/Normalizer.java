package com.dailymind.importer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Normalizer {
    public String normalize(String raw) {
        return raw == null ? null : raw.trim().replaceAll("\\s+", " ");
    }

    public String hash(String content) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            var b = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
