package com.zerog.network.stellarforge.util;

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
import java.util.regex.Pattern;

/**
 * Improved version fetcher with better error handling and API integration
 */
public class ImprovedVersionFetcher {
    private static final Logger logger = LoggerFactory.getLogger(ImprovedVersionFetcher.class);
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    // API endpoints
    private static final String NEOFORGE_API_URL = "https://api.neoforged.net/versions";
    private static final String FORGE_API_URL = "https://files.minecraftforge.net/net/minecraftforge/forge/index.html";
    private static final String FABRIC_API_URL = "https://meta.fabricmc.net/v2/versions/loader";
    private static final String QUILT_API_URL = "https://meta.quiltmc.org/v3/versions/loader";
    
    public ImprovedVersionFetcher() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
        this.gson = new Gson();
    }
    
    /**
     * Get the latest version for a specific mod loader
     */
    public String getLatestVersion(String modLoader) {
        try {
            switch (modLoader.toLowerCase()) {
                case "neoforge":
                    return getLatestNeoForgeVersion();
                case "forge":
                    return getLatestForgeVersion();
                case "fabric":
                    return getLatestFabricVersion();
                case "quilt":
                    return getLatestQuiltVersion();
                default:
                    logger.warn("Unknown mod loader: {}", modLoader);
                    return "Unknown";
            }
        } catch (Exception e) {
            logger.error("Error fetching latest version for {}: {}", modLoader, e.getMessage());
            return getFallbackVersion(modLoader);
        }
    }
    
    /**
     * Get all available versions for a mod loader
     */
    public List<String> getAllVersions(String modLoader) {
        List<String> versions = new ArrayList<>();
        
        try {
            switch (modLoader.toLowerCase()) {
                case "neoforge":
                    versions = getAllNeoForgeVersions();
                    break;
                case "forge":
                    versions = getAllForgeVersions();
                    break;
                case "fabric":
                    versions = getAllFabricVersions();
                    break;
                case "quilt":
                    versions = getAllQuiltVersions();
                    break;
                default:
                    logger.warn("Unknown mod loader: {}", modLoader);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error fetching versions for {}: {}", modLoader, e.getMessage());
        }
        
        // Add fallback if empty
        if (versions.isEmpty()) {
            versions.add(getFallbackVersion(modLoader));
        }
        
        return versions;
    }
    
    private String getLatestNeoForgeVersion() throws IOException {
        logger.info("Fetching latest NeoForge version from API...");
        
        Request request = new Request.Builder()
            .url(NEOFORGE_API_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .addHeader("Accept", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                logger.debug("NeoForge API response: {}", responseBody);
                
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                
                // Try to get the latest stable version
                if (jsonResponse.has("stable")) {
                    JsonArray stable = jsonResponse.getAsJsonArray("stable");
                    if (stable.size() > 0) {
                        String latest = stable.get(0).getAsString();
                        logger.info("Latest NeoForge stable version: {}", latest);
                        return latest;
                    }
                }
                
                // Fallback to latest version
                if (jsonResponse.has("latest")) {
                    String latest = jsonResponse.get("latest").getAsString();
                    logger.info("Latest NeoForge version: {}", latest);
                    return latest;
                }
                
                // Try to parse versions array
                if (jsonResponse.has("versions")) {
                    JsonArray versions = jsonResponse.getAsJsonArray("versions");
                    if (versions.size() > 0) {
                        String latest = versions.get(0).getAsString();
                        logger.info("Latest NeoForge version from array: {}", latest);
                        return latest;
                    }
                }
            } else {
                logger.warn("NeoForge API request failed: {} - {}", response.code(), response.message());
            }
        }
        
        throw new IOException("Failed to fetch NeoForge version from API");
    }
    
    private List<String> getAllNeoForgeVersions() throws IOException {
        List<String> versions = new ArrayList<>();
        
        Request request = new Request.Builder()
            .url(NEOFORGE_API_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .addHeader("Accept", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                
                // Parse versions from different possible structures
                if (jsonResponse.has("versions")) {
                    JsonArray versionsArray = jsonResponse.getAsJsonArray("versions");
                    for (JsonElement element : versionsArray) {
                        if (element.isJsonPrimitive()) {
                            versions.add(element.getAsString());
                        } else if (element.isJsonObject()) {
                            JsonObject versionObj = element.getAsJsonObject();
                            if (versionObj.has("version")) {
                                versions.add(versionObj.get("version").getAsString());
                            }
                        }
                    }
                }
                
                // Also add stable versions
                if (jsonResponse.has("stable")) {
                    JsonArray stable = jsonResponse.getAsJsonArray("stable");
                    for (JsonElement element : stable) {
                        String version = element.getAsString();
                        if (!versions.contains(version)) {
                            versions.add(version);
                        }
                    }
                }
            }
        }
        
        // Sort versions in descending order (newest first)
        versions.sort((a, b) -> compareVersions(b, a));
        
        return versions;
    }
    
    private String getLatestForgeVersion() throws IOException {
        // For now, return a stable version - Forge API is more complex
        return "1.20.1-47.2.0";
    }
    
    private List<String> getAllForgeVersions() throws IOException {
        List<String> versions = new ArrayList<>();
        // Add some common stable versions
        versions.add("1.20.1-47.2.0");
        versions.add("1.20.1-47.1.0");
        versions.add("1.19.4-45.2.0");
        versions.add("1.19.2-43.3.0");
        return versions;
    }
    
    private String getLatestFabricVersion() throws IOException {
        Request request = new Request.Builder()
            .url(FABRIC_API_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonArray jsonResponse = gson.fromJson(responseBody, JsonArray.class);
                
                if (jsonResponse.size() > 0) {
                    JsonObject latest = jsonResponse.get(0).getAsJsonObject();
                    if (latest.has("version")) {
                        return latest.get("version").getAsString();
                    }
                }
            }
        }
        
        return "0.15.3";
    }
    
    private List<String> getAllFabricVersions() throws IOException {
        List<String> versions = new ArrayList<>();
        
        Request request = new Request.Builder()
            .url(FABRIC_API_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonArray jsonResponse = gson.fromJson(responseBody, JsonArray.class);
                
                for (JsonElement element : jsonResponse) {
                    JsonObject versionObj = element.getAsJsonObject();
                    if (versionObj.has("version")) {
                        versions.add(versionObj.get("version").getAsString());
                    }
                }
            }
        }
        
        return versions;
    }
    
    private String getLatestQuiltVersion() throws IOException {
        Request request = new Request.Builder()
            .url(QUILT_API_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonArray jsonResponse = gson.fromJson(responseBody, JsonArray.class);
                
                if (jsonResponse.size() > 0) {
                    JsonObject latest = jsonResponse.get(0).getAsJsonObject();
                    if (latest.has("version")) {
                        return latest.get("version").getAsString();
                    }
                }
            }
        }
        
        return "0.23.0";
    }
    
    private List<String> getAllQuiltVersions() throws IOException {
        List<String> versions = new ArrayList<>();
        
        Request request = new Request.Builder()
            .url(QUILT_API_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonArray jsonResponse = gson.fromJson(responseBody, JsonArray.class);
                
                for (JsonElement element : jsonResponse) {
                    JsonObject versionObj = element.getAsJsonObject();
                    if (versionObj.has("version")) {
                        versions.add(versionObj.get("version").getAsString());
                    }
                }
            }
        }
        
        return versions;
    }
    
    private String getFallbackVersion(String modLoader) {
        switch (modLoader.toLowerCase()) {
            case "neoforge":
                return "1.20.1-47.1.100";
            case "forge":
                return "1.20.1-47.2.0";
            case "fabric":
                return "0.15.3";
            case "quilt":
                return "0.23.0";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Compare two version strings
     */
    private int compareVersions(String version1, String version2) {
        // Simple version comparison - can be improved
        String[] v1Parts = version1.split("[-.]");
        String[] v2Parts = version2.split("[-.]");
        
        int length = Math.max(v1Parts.length, v2Parts.length);
        for (int i = 0; i < length; i++) {
            int v1Part = i < v1Parts.length ? parseVersionPart(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? parseVersionPart(v2Parts[i]) : 0;
            
            if (v1Part < v2Part) return -1;
            if (v1Part > v2Part) return 1;
        }
        
        return 0;
    }
    
    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public void close() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}
