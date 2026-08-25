package com.zerog.stellarserverforge.net;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Local checksum computation — replaces the original tool's shelling out to {@code certutil}. */
public final class ChecksumUtil {

    private ChecksumUtil() {
    }

    public static String sha1(Path file) throws IOException {
        return hash(file, "SHA-1");
    }

    public static String sha256(Path file) throws IOException {
        return hash(file, "SHA-256");
    }

    public static String md5(Path file) throws IOException {
        return hash(file, "MD5");
    }

    private static String hash(Path file, String algorithm) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " not available", e);
        }
    }

    public static boolean matches(Path file, String expectedHex, String algorithm) throws IOException {
        String actual = switch (algorithm.toUpperCase().replace("-", "")) {
            case "SHA256" -> sha256(file);
            case "MD5" -> md5(file);
            default -> sha1(file);
        };
        return actual.equalsIgnoreCase(expectedHex.trim());
    }
}
