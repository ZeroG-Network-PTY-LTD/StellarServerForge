package com.zerog.network.stellarforge.security;

/**
 * Simple test to verify API key functionality
 * This class will be removed in production
 */
public class KeyTest {
    public static void main(String[] args) {
        System.out.println("Testing API key configuration...");
        
        String curseForgeKey = KeyVault.getCurseForgeApiKey();
        if (curseForgeKey != null) {
            System.out.println("✅ CurseForge API key successfully decrypted");
            System.out.println("Key length: " + curseForgeKey.length() + " characters");
            System.out.println("Key starts with: " + curseForgeKey.substring(0, Math.min(10, curseForgeKey.length())) + "...");
        } else {
            System.out.println("❌ CurseForge API key is null");
        }
        
        String modrinthKey = KeyVault.getModrinthApiKey();
        if (modrinthKey != null) {
            System.out.println("✅ Modrinth API key available");
        } else {
            System.out.println("⚠️ Modrinth API key not configured (optional)");
        }
        
        System.out.println("Configuration status: " + (KeyVault.isConfigured() ? "✅ Ready" : "❌ Not configured"));
    }
}
