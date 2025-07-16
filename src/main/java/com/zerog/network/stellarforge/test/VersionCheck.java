package com.zerog.network.stellarforge.test;

import com.zerog.network.stellarforge.util.ModLoaderVersionFetcher;
import com.zerog.network.stellarforge.util.ServerManager;

import java.util.List;

/**
 * Simple test to verify version functionality
 */
public class VersionCheck {
    public static void main(String[] args) {
        System.out.println("=== Minecraft Versions Test ===");
        String[] mcVersions = ServerManager.getAvailableMinecraftVersions();
        System.out.println("Available Minecraft versions: " + mcVersions.length);
        for (String version : mcVersions) {
            System.out.println("- " + version);
        }
        
        System.out.println("\n=== NeoForge Version Test ===");
        ModLoaderVersionFetcher fetcher = new ModLoaderVersionFetcher();
        
        // Test a few key versions
        String[] testVersions = {"1.21.1", "1.21", "1.20.6", "1.20.4", "1.20.1"};
        
        for (String mcVersion : testVersions) {
            List<String> neoforgeVersions = fetcher.getModLoaderVersions("neoforge", mcVersion);
            System.out.println("Minecraft " + mcVersion + " -> NeoForge: " + neoforgeVersions.size() + " versions");
            if (!neoforgeVersions.isEmpty()) {
                System.out.println("  Latest: " + neoforgeVersions.get(0));
                if (neoforgeVersions.size() > 1) {
                    System.out.println("  Others: " + neoforgeVersions.subList(1, Math.min(4, neoforgeVersions.size())));
                }
            }
        }
        
        System.out.println("\n=== Forge Version Test ===");
        for (String mcVersion : testVersions) {
            List<String> forgeVersions = fetcher.getModLoaderVersions("forge", mcVersion);
            System.out.println("Minecraft " + mcVersion + " -> Forge: " + forgeVersions.size() + " versions");
            if (!forgeVersions.isEmpty()) {
                System.out.println("  Latest: " + forgeVersions.get(0));
            }
        }
        
        fetcher.close();
        System.out.println("\nVersion check completed!");
    }
}
