package com.zerog.network.stellarforge.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Test class to verify version fetching functionality
 */
public class VersionTester {
    private static final Logger logger = LoggerFactory.getLogger(VersionTester.class);
    
    public static void main(String[] args) {
        ModLoaderVersionFetcher fetcher = new ModLoaderVersionFetcher();
        
        // Test different Minecraft versions with NeoForge
        String[] mcVersions = {"1.21.1", "1.21", "1.20.6", "1.20.5", "1.20.4", "1.20.3", "1.20.2", "1.20.1", "1.19.4", "1.19.2"};
        
        System.out.println("=== NEOFORGE VERSION TESTING ===");
        for (String mcVersion : mcVersions) {
            List<String> versions = fetcher.getModLoaderVersions("neoforge", mcVersion);
            System.out.println("Minecraft " + mcVersion + " -> NeoForge versions: " + versions);
        }
        
        System.out.println("\n=== FORGE VERSION TESTING ===");
        for (String mcVersion : mcVersions) {
            List<String> versions = fetcher.getModLoaderVersions("forge", mcVersion);
            System.out.println("Minecraft " + mcVersion + " -> Forge versions: " + versions);
        }
        
        System.out.println("\n=== FABRIC VERSION TESTING ===");
        List<String> fabricVersions = fetcher.getAllVersionsForMinecraft("fabric", "1.21.1");
        System.out.println("Minecraft 1.21.1 -> Fabric versions: " + fabricVersions);
        
        System.out.println("\n=== QUILT VERSION TESTING ===");
        List<String> quiltVersions = fetcher.getAllVersionsForMinecraft("quilt", "1.21.1");
        System.out.println("Minecraft 1.21.1 -> Quilt versions: " + quiltVersions);
        
        fetcher.close();
    }
}
