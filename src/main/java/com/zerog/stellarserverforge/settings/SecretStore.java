package com.zerog.stellarserverforge.settings;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Encrypts secrets (e.g. the CurseForge API key) at rest with AES-256-GCM, keyed off a random
 * key generated on first use and stored under the user's home directory — outside any server
 * folder and outside this repo, so it never ends up in {@code settings.json} or in git. Without
 * that local key file, the ciphertext in settings.json is unreadable.
 */
public final class SecretStore {

    private SecretStore() {
    }

    private static final Path KEY_FILE = Path.of(System.getProperty("user.home"), ".stellarserverforge", "secret.key");
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private static volatile SecretKey cachedKey;

    private static synchronized SecretKey key() throws Exception {
        if (cachedKey != null) {
            return cachedKey;
        }
        Files.createDirectories(KEY_FILE.getParent());
        if (Files.exists(KEY_FILE)) {
            byte[] raw = Base64.getDecoder().decode(Files.readString(KEY_FILE).trim());
            cachedKey = new SecretKeySpec(raw, "AES");
        } else {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            SecretKey generated = generator.generateKey();
            Files.writeString(KEY_FILE, Base64.getEncoder().encodeToString(generated.getEncoded()));
            restrictToOwnerOnly(KEY_FILE);
            cachedKey = generated;
        }
        return cachedKey;
    }

    private static void restrictToOwnerOnly(Path file) {
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | java.io.IOException ignored) {
            // Windows has no POSIX permission model here; NTFS ACLs already restrict the user
            // profile directory to the owning account by default.
        }
    }

    /** Encrypts a plaintext secret for storage. Returns "" for null/blank input (never encrypts an empty value). */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return "";
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv).put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt secret", e);
        }
    }

    /** Decrypts a value produced by {@link #encrypt}. Returns "" if it's blank or can't be decrypted
     * (e.g. the local key file is missing/different, or the value predates encryption) rather than
     * throwing, so a settings.json load never crashes over a stale/foreign secret. */
    public static String decrypt(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return "";
        }
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            if (all.length <= GCM_IV_LENGTH) {
                return "";
            }
            byte[] iv = Arrays.copyOfRange(all, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(all, GCM_IV_LENGTH, all.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
