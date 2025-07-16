package com.universalator.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for fetching mod loader versions from various APIs
 */
public class ModLoaderVersionFetcher {
    private static final Logger logger = LoggerFactory.getLogger(ModLoaderVersionFetcher.class);
    
    private static final String FORGE_MAVEN_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml";
    private static final String FABRIC_META_URL = "https://meta.fabricmc.net/v2/versions/loader";
    private static final String QUILT_META_URL = "https://meta.quiltmc.org/v3/versions/loader";
    private static final String NEOFORGE_MAVEN_URL = "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge";
    private static final String NEOFORGE_API_URL = "https://api.neoforged.net/versions";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    public ModLoaderVersionFetcher() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
    }
    
    /**
     * Get available mod loader versions for a specific loader and Minecraft version
     */
    public List<String> getModLoaderVersions(String modLoader, String minecraftVersion) {
        List<String> versions = new ArrayList<>();
        
        try {
            switch (modLoader.toLowerCase()) {
                case "forge":
                    versions = getForgeVersions(minecraftVersion);
                    break;
                case "fabric":
                    versions = getFabricVersions(minecraftVersion);
                    break;
                case "quilt":
                    versions = getQuiltVersions(minecraftVersion);
                    break;
                case "neoforge":
                    versions = getNeoForgeVersions(minecraftVersion);
                    break;
                default:
                    logger.warn("Unknown mod loader: {}", modLoader);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error fetching versions for {}: {}", modLoader, e.getMessage());
        }
        
        // Add fallback versions if API call failed
        if (versions.isEmpty()) {
            versions = getFallbackVersions(modLoader, minecraftVersion);
        }
        
        return versions;
    }
    
    /**
     * Get Forge versions for a specific Minecraft version
     */
    private List<String> getForgeVersions(String minecraftVersion) throws IOException {
        List<String> versions = new ArrayList<>();
        
        // Use Forge's promotions endpoint for recommended versions
        String url = String.format("https://files.minecraftforge.net/net/minecraftforge/forge/promotions_%s.json", minecraftVersion);
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                
                if (jsonResponse.has("promos")) {
                    JsonObject promos = jsonResponse.getAsJsonObject("promos");
                    
                    // Get recommended and latest versions
                    String recommended = getVersionFromPromos(promos, minecraftVersion + "-recommended");
                    String latest = getVersionFromPromos(promos, minecraftVersion + "-latest");
                    
                    if (recommended != null) {
                        versions.add(recommended);
                    }
                    if (latest != null && !latest.equals(recommended)) {
                        versions.add(latest);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to fetch Forge versions from promotions API: {}", e.getMessage());
        }
        
        return versions;
    }
    
    /**
     * Get Fabric versions
     */
    private List<String> getFabricVersions(String minecraftVersion) throws IOException {
        List<String> versions = new ArrayList<>();
        
        Request request = new Request.Builder()
            .url(FABRIC_META_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonArray jsonArray = gson.fromJson(responseBody, JsonArray.class);
                
                // Get the latest stable versions
                int count = 0;
                for (JsonElement element : jsonArray) {
                    if (count >= 5) break; // Limit to first 5 versions
                    
                    JsonObject version = element.getAsJsonObject();
                    if (version.has("version") && version.get("stable").getAsBoolean()) {
                        versions.add(version.get("version").getAsString());
                        count++;
                    }
                }
            }
        }
        
        return versions;
    }
    
    /**
     * Get Quilt versions
     */
    private List<String> getQuiltVersions(String minecraftVersion) throws IOException {
        List<String> versions = new ArrayList<>();
        
        Request request = new Request.Builder()
            .url(QUILT_META_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonArray jsonArray = gson.fromJson(responseBody, JsonArray.class);
                
                // Get the latest stable versions
                int count = 0;
                for (JsonElement element : jsonArray) {
                    if (count >= 5) break; // Limit to first 5 versions
                    
                    JsonObject version = element.getAsJsonObject();
                    if (version.has("version")) {
                        versions.add(version.get("version").getAsString());
                        count++;
                    }
                }
            }
        }
        
        return versions;
    }
    
    /**
     * Get NeoForge versions with improved API support
     */
    private List<String> getNeoForgeVersions(String minecraftVersion) throws IOException {
        List<String> versions = new ArrayList<>();
        
        // Try the NeoForge API first
        try {
            String apiUrl = String.format("%s/%s", NEOFORGE_API_URL, minecraftVersion);
            Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("User-Agent", "StellarServerForge/1.0.0")
                .addHeader("Accept", "application/json")
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    
                    if (jsonResponse.has("versions")) {
                        JsonArray versionsArray = jsonResponse.getAsJsonArray("versions");
                        
                        // Get the latest versions
                        int count = 0;
                        for (JsonElement element : versionsArray) {
                            if (count >= 5) break; // Limit to first 5 versions
                            
                            String version = element.getAsString();
                            versions.add(version);
                            count++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to fetch NeoForge versions from API: {}", e.getMessage());
        }
        
        // If API failed, try Maven metadata
        if (versions.isEmpty()) {
            try {
                Request request = new Request.Builder()
                    .url(NEOFORGE_MAVEN_URL)
                    .addHeader("User-Agent", "StellarServerForge/1.0.0")
                    .addHeader("Accept", "application/json")
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        
                        if (jsonResponse.has("versions")) {
                            JsonArray versionsArray = jsonResponse.getAsJsonArray("versions");
                            
                            // Filter versions for the specific Minecraft version
                            for (JsonElement element : versionsArray) {
                                String version = element.getAsString();
                                
                                // NeoForge versions are typically in format: 20.4.109-beta (for 1.20.4)
                                if (isNeoForgeVersionCompatible(version, minecraftVersion)) {
                                    versions.add(version);
                                    if (versions.size() >= 5) break; // Limit to 5 versions
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed to fetch NeoForge versions from Maven: {}", e.getMessage());
            }
        }
        
        return versions;
    }
    
    /**
     * Check if a NeoForge version is compatible with a Minecraft version
     */
    private boolean isNeoForgeVersionCompatible(String neoForgeVersion, String minecraftVersion) {
        try {
            // NeoForge uses a different versioning scheme
            // Format: XX.Y.ZZZ where XX.Y corresponds to MC version
            String[] parts = neoForgeVersion.split("\\.");
            if (parts.length >= 2) {
                String neoMajor = parts[0];
                String neoMinor = parts[1];
                
                // Parse Minecraft version
                String[] mcParts = minecraftVersion.split("\\.");
                if (mcParts.length >= 2) {
                    String mcMajor = mcParts[1]; // "20" from "1.20.1"
                    String mcMinor = mcParts.length > 2 ? mcParts[2] : "0"; // "1" from "1.20.1"
                    
                    // Check if NeoForge version matches MC version
                    return neoMajor.equals(mcMajor) && neoMinor.equals(mcMinor);
                }
            }
        } catch (Exception e) {
            logger.debug("Error checking NeoForge version compatibility: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Extract version from Forge promotions
     */
    private String getVersionFromPromos(JsonObject promos, String key) {
        if (promos.has(key)) {
            return promos.get(key).getAsString();
        }
        return null;
    }
    
    /**
     * Get fallback versions when API calls fail
     */
    private List<String> getFallbackVersions(String modLoader, String minecraftVersion) {
        List<String> versions = new ArrayList<>();
        
        switch (modLoader.toLowerCase()) {
            case "forge":
                if ("1.20.1".equals(minecraftVersion)) {
                    versions.add("47.2.0");
                    versions.add("47.1.0");
                    versions.add("47.0.35");
                } else if ("1.19.4".equals(minecraftVersion)) {
                    versions.add("45.1.0");
                    versions.add("45.0.64");
                    versions.add("45.0.43");
                } else if ("1.18.2".equals(minecraftVersion)) {
                    versions.add("40.2.0");
                    versions.add("40.1.0");
                } else if ("1.16.5".equals(minecraftVersion)) {
                    versions.add("36.2.0");
                    versions.add("36.1.0");
                }
                break;
            case "fabric":
                versions.add("0.14.21");
                versions.add("0.14.19");
                versions.add("0.14.17");
                versions.add("0.14.14");
                break;
            case "quilt":
                versions.add("0.19.2");
                versions.add("0.19.1");
                versions.add("0.18.10");
                versions.add("0.18.8");
                break;
            case "neoforge":
                if ("1.20.1".equals(minecraftVersion)) {
                    versions.add("47.1.104");
                    versions.add("47.1.103");
                    versions.add("47.1.100");
                } else if ("1.20.4".equals(minecraftVersion)) {
                    versions.add("20.4.109");
                    versions.add("20.4.108");
                    versions.add("20.4.107");
                } else if ("1.20.6".equals(minecraftVersion)) {
                    versions.add("20.6.119");
                    versions.add("20.6.118");
                    versions.add("20.6.117");
                } else {
                    // Generic fallback for newer versions
                    versions.add("Latest");
                }
                break;
        }
        
        if (versions.isEmpty()) {
            versions.add("Latest");
        }
        
        return versions;
    }
    
    /**
     * Check if a version fetching service is available
     */
    public boolean isServiceAvailable(String modLoader) {
        try {
            String testUrl;
            switch (modLoader.toLowerCase()) {
                case "forge":
                    testUrl = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_1.20.1.json";
                    break;
                case "fabric":
                    testUrl = FABRIC_META_URL;
                    break;
                case "quilt":
                    testUrl = QUILT_META_URL;
                    break;
                case "neoforge":
                    testUrl = NEOFORGE_API_URL;
                    break;
                default:
                    return false;
            }
            
            Request request = new Request.Builder()
                .url(testUrl)
                .addHeader("User-Agent", "StellarServerForge/1.0.0")
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            logger.debug("Service availability check failed for {}: {}", modLoader, e.getMessage());
            return false;
        }
    }
}
