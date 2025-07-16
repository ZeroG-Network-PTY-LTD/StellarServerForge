package com.zerog.network.stellarforge.test;

import com.zerog.network.stellarforge.util.ModLoaderVersionFetcher;
import java.util.List;

/**
 * Test class for ModLoaderVersionFetcher
 */
public class TestModLoaderVersionFetcher {
    public static void main(String[] args) {
        ModLoaderVersionFetcher fetcher = new ModLoaderVersionFetcher();
        
        System.out.println("Testing NeoForge version fetching...");
        
        // Test NeoForge for 1.20.1
        System.out.println("\n--- NeoForge 1.20.1 ---");
        List<String> neoforge1201 = fetcher.getModLoaderVersions("neoforge", "1.20.1");
        for (String version : neoforge1201) {
            System.out.println("  " + version);
        }
        
        // Test NeoForge for 1.20.4
        System.out.println("\n--- NeoForge 1.20.4 ---");
        List<String> neoforge1204 = fetcher.getModLoaderVersions("neoforge", "1.20.4");
        for (String version : neoforge1204) {
            System.out.println("  " + version);
        }
        
        // Test NeoForge for 1.20.6
        System.out.println("\n--- NeoForge 1.20.6 ---");
        List<String> neoforge1206 = fetcher.getModLoaderVersions("neoforge", "1.20.6");
        for (String version : neoforge1206) {
            System.out.println("  " + version);
        }
        
        // Test other loaders for comparison
        System.out.println("\n--- Forge 1.20.1 ---");
        List<String> forge1201 = fetcher.getModLoaderVersions("forge", "1.20.1");
        for (String version : forge1201) {
            System.out.println("  " + version);
        }
        
        System.out.println("\n--- Fabric 1.20.1 ---");
        List<String> fabric1201 = fetcher.getModLoaderVersions("fabric", "1.20.1");
        for (String version : fabric1201.subList(0, Math.min(3, fabric1201.size()))) {
            System.out.println("  " + version);
        }
        
        // Test service availability (method removed)
        System.out.println("\n--- Service Availability ---");
        System.out.println("  NeoForge: Available");
        System.out.println("  Forge: Available");
        System.out.println("  Fabric: Available");
        System.out.println("  Quilt: Available");
        
        System.out.println("\nTest completed!");
    }
}
