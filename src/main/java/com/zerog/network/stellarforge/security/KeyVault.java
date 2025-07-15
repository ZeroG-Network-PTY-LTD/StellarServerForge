package com.zerog.network.stellarforge.security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Secure key vault for embedded API keys
 * Keys are obfuscated and encrypted to prevent casual exposure
 */
public class KeyVault {
    
    // Obfuscated salt for key derivation (not the actual key)
    private static final String SALT = "StellarForge2025ZeroG";
    
    // Encrypted API key (securely embedded)
    private static final String ENCRYPTED_CURSEFORGE_KEY = "nPEhOCVlD4MuqL0TtE48nGRii7uYqjssIPigJwxbOO/9YPaX1PsiVvo2L3SYIUtX";
    
    // Optional Modrinth key
    private static final String ENCRYPTED_MODRINTH_KEY = "ENCRYPTED_MODRINTH_PLACEHOLDER";
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    /**
     * Generate encryption key from application metadata
     */
    private static SecretKeySpec generateKey() {
        try {
            String keyMaterial = SALT + "ZeroGNetwork" + System.getProperty("java.version");
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(keyMaterial.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
    
    /**
     * Encrypt a plaintext API key (for setup only)
     */
    public static String encryptKey(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, generateKey());
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt key", e);
        }
    }
    
    /**
     * Decrypt an encrypted API key
     */
    private static String decryptKey(String encryptedKey) {
        if ("ENCRYPTED_KEY_PLACEHOLDER".equals(encryptedKey) || 
            "ENCRYPTED_MODRINTH_PLACEHOLDER".equals(encryptedKey)) {
            return null; // Not configured
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, generateKey());
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedKey));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Warning: Failed to decrypt API key - may be corrupted");
            return null;
        }
    }
    
    /**
     * Get the embedded CurseForge API key
     */
    public static String getCurseForgeApiKey() {
        String key = decryptKey(ENCRYPTED_CURSEFORGE_KEY);
        if (key == null) {
            System.err.println("Warning: CurseForge API key not properly configured");
        }
        return key;
    }
    
    /**
     * Get the embedded Modrinth API key
     */
    public static String getModrinthApiKey() {
        return decryptKey(ENCRYPTED_MODRINTH_KEY);
    }
    
    /**
     * Check if API keys are properly configured
     */
    public static boolean isConfigured() {
        return getCurseForgeApiKey() != null;
    }
}
