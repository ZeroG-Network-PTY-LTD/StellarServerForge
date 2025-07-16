package com.zerog.network.stellarforge.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for fetching mod loader versions from various APIs
 */
public class ModLoaderVersionFetcher {
    private static final Logger logger = LoggerFactory.getLogger(ModLoaderVersionFetcher.class);
    
    // API endpoints
    private static final String NEOFORGE_MAVEN_URL = "https://maven.neoforged.net/net/neoforged/neoforge/maven-metadata.xml";
    private static final String FORGE_MAVEN_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml";
    private static final String FABRIC_MAVEN_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml";
    private static final String FABRIC_API_URL = "https://meta.fabricmc.net/v2/versions/loader";
    private static final String QUILT_MAVEN_URL = "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-loader/maven-metadata.xml";
    private static final String QUILT_API_URL = "https://meta.quiltmc.org/v3/versions/loader";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    // Fallback versions for when APIs are unavailable
    private static final Map<String, String> FALLBACK_VERSIONS = Map.of(
        "forge", "47.3.10",
        "fabric", "0.16.5",
        "quilt", "0.26.4",
        "neoforge", "21.1.65"
    );
    
    public ModLoaderVersionFetcher() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
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
                    versions = getForgeVersionsForMinecraft(minecraftVersion);
                    break;
                case "fabric":
                    versions = getFabricVersionsForMinecraft(minecraftVersion);
                    break;
                case "quilt":
                    versions = getQuiltVersionsForMinecraft(minecraftVersion);
                    break;
                case "neoforge":
                    versions = getNeoForgeVersionsForMinecraft(minecraftVersion);
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
            versions.add(getFallbackVersionForMinecraft(modLoader, minecraftVersion));
        }
        
        return versions;
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
            return FALLBACK_VERSIONS.getOrDefault(modLoader.toLowerCase(), "Unknown");
        }
    }
    
    private String getLatestNeoForgeVersion() throws IOException {
        logger.info("Fetching latest NeoForge version from Maven metadata...");
        
        Request request = new Request.Builder()
            .url(NEOFORGE_MAVEN_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .addHeader("Accept", "application/xml")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String xmlResponse = response.body().string();
                logger.debug("NeoForge Maven metadata response: {}", xmlResponse);
                
                Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(xmlResponse.getBytes()));
                    
                NodeList versions = doc.getElementsByTagName("version");
                if (versions.getLength() > 0) {
                    // Get the latest version (last in the list)
                    String latest = versions.item(versions.getLength() - 1).getTextContent();
                    logger.info("Latest NeoForge version from Maven: {}", latest);
                    return latest;
                }
            } else {
                logger.warn("NeoForge Maven request failed: {} - {}", response.code(), response.message());
            }
        } catch (Exception e) {
            logger.error("Error parsing NeoForge Maven metadata: {}", e.getMessage());
        }
        
        throw new IOException("Failed to fetch NeoForge version from Maven metadata");
    }
    
    private List<String> getNeoForgeVersionsForMinecraft(String minecraftVersion) throws IOException {
        List<String> versions = new ArrayList<>();
        
        // First try to get versions from Maven metadata
        List<String> allVersions = getAllNeoForgeVersions();
        
        // Filter versions based on Minecraft version
        // NeoForge follows semantic versioning: MCMAJOR.MCMINOR.MODLOADERPATCH
        // e.g., 1.21.1-21.1.65 or just 21.1.65 (where 21.1 matches MC 1.21.1)
        for (String version : allVersions) {
            if (isVersionCompatibleWithMinecraft(version, minecraftVersion)) {
                versions.add(version);
            }
        }
        
        // If no versions found from API, use hardcoded versions
        if (versions.isEmpty()) {
            versions.addAll(getHardcodedNeoForgeVersions(minecraftVersion));
        }
        
        logger.info("Found {} NeoForge versions for Minecraft {}", versions.size(), minecraftVersion);
        return versions;
    }
    
    private List<String> getAllNeoForgeVersions() throws IOException {
        List<String> versions = new ArrayList<>();
        
        Request request = new Request.Builder()
            .url(NEOFORGE_MAVEN_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .addHeader("Accept", "application/xml")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String xmlResponse = response.body().string();
                logger.debug("NeoForge Maven metadata response: {}", xmlResponse);
                
                Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(xmlResponse.getBytes()));
                    
                NodeList versionNodes = doc.getElementsByTagName("version");
                for (int i = 0; i < versionNodes.getLength(); i++) {
                    String version = versionNodes.item(i).getTextContent();
                    // Filter out beta and snapshot versions for main list
                    if (!version.contains("-beta") && !version.contains("-SNAPSHOT")) {
                        versions.add(version);
                    }
                }
                
                logger.info("Found {} NeoForge versions from Maven", versions.size());
            }
        } catch (Exception e) {
            logger.error("Error parsing NeoForge Maven metadata: {}", e.getMessage());
        }
        
        // Sort versions in descending order (newest first)
        versions.sort((a, b) -> compareVersions(b, a));
        
        return versions;
    }
    
    private boolean isVersionCompatibleWithMinecraft(String neoforgeVersion, String minecraftVersion) {
        // NeoForge version format: 21.1.65 (for MC 1.21.1), 20.4.237 (for MC 1.20.4), etc.
        // The major version number corresponds to MC version
        
        try {
            String[] mcParts = minecraftVersion.split("\\.");
            if (mcParts.length < 2) return false;
            
            int mcMajor = Integer.parseInt(mcParts[1]); // 1.21.1 -> 21
            int mcMinor = mcParts.length > 2 ? Integer.parseInt(mcParts[2]) : 0; // 1.21.1 -> 1
            
            String[] nfParts = neoforgeVersion.split("\\.");
            if (nfParts.length < 2) return false;
            
            int nfMajor = Integer.parseInt(nfParts[0]); // 21.1.65 -> 21
            int nfMinor = Integer.parseInt(nfParts[1]); // 21.1.65 -> 1
            
            // Check if NeoForge version matches Minecraft version
            return nfMajor == mcMajor && nfMinor == mcMinor;
            
        } catch (NumberFormatException e) {
            // If parsing fails, check against hardcoded mappings
            return checkHardcodedCompatibility(neoforgeVersion, minecraftVersion);
        }
    }
    
    private boolean checkHardcodedCompatibility(String neoforgeVersion, String minecraftVersion) {
        // Hardcoded compatibility for specific versions
        switch (minecraftVersion) {
            case "1.21.1":
                return neoforgeVersion.startsWith("21.1.") || neoforgeVersion.startsWith("1.21.1-");
            case "1.21":
                return neoforgeVersion.startsWith("21.0.") || neoforgeVersion.startsWith("1.21-");
            case "1.20.6":
                return neoforgeVersion.startsWith("20.6.") || neoforgeVersion.startsWith("1.20.6-");
            case "1.20.5":
                return neoforgeVersion.startsWith("20.5.") || neoforgeVersion.startsWith("1.20.5-");
            case "1.20.4":
                return neoforgeVersion.startsWith("20.4.") || neoforgeVersion.startsWith("1.20.4-");
            case "1.20.3":
                return neoforgeVersion.startsWith("20.3.") || neoforgeVersion.startsWith("1.20.3-");
            case "1.20.2":
                return neoforgeVersion.startsWith("20.2.") || neoforgeVersion.startsWith("1.20.2-");
            case "1.20.1":
                return neoforgeVersion.startsWith("47.") || neoforgeVersion.startsWith("1.20.1-");
            default:
                return false;
        }
    }
    
    private List<String> getForgeVersionsForMinecraft(String minecraftVersion) throws IOException {
        List<String> versions = new ArrayList<>();
        
        // First try to get versions from Maven metadata
        List<String> allVersions = getAllForgeVersions();
        
        // Filter versions based on Minecraft version
        // Forge follows format: 1.20.1-47.2.0 (MC-ForgeVersion)
        for (String version : allVersions) {
            if (version.startsWith(minecraftVersion + "-")) {
                versions.add(version);
            }
        }
        
        // If no versions found from API, use hardcoded versions
        if (versions.isEmpty()) {
            versions.addAll(getHardcodedForgeVersions(minecraftVersion));
        }
        
        logger.info("Found {} Forge versions for Minecraft {}", versions.size(), minecraftVersion);
        return versions;
    }
    
    private List<String> getAllForgeVersions() throws IOException {
        List<String> versions = new ArrayList<>();
        
        Request request = new Request.Builder()
            .url(FORGE_MAVEN_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .addHeader("Accept", "application/xml")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String xmlResponse = response.body().string();
                logger.debug("Forge Maven metadata response: {}", xmlResponse);
                
                Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(xmlResponse.getBytes()));
                    
                NodeList versionNodes = doc.getElementsByTagName("version");
                for (int i = 0; i < versionNodes.getLength(); i++) {
                    String version = versionNodes.item(i).getTextContent();
                    // Filter out beta and snapshot versions for main list
                    if (!version.contains("-beta") && !version.contains("-SNAPSHOT")) {
                        versions.add(version);
                    }
                }
                
                logger.info("Found {} Forge versions from Maven", versions.size());
            }
        } catch (Exception e) {
            logger.error("Error parsing Forge Maven metadata: {}", e.getMessage());
        }
        
        // Sort versions in descending order (newest first)
        versions.sort((a, b) -> compareVersions(b, a));
        
        return versions;
    }
    
    private String getLatestForgeVersion() throws IOException {
        logger.info("Fetching latest Forge version from Maven metadata...");
        
        Request request = new Request.Builder()
            .url(FORGE_MAVEN_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .addHeader("Accept", "application/xml")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String xmlResponse = response.body().string();
                logger.debug("Forge Maven metadata response: {}", xmlResponse);
                
                Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(xmlResponse.getBytes()));
                    
                NodeList versions = doc.getElementsByTagName("version");
                if (versions.getLength() > 0) {
                    // Get the latest version (last in the list)
                    String latest = versions.item(versions.getLength() - 1).getTextContent();
                    logger.info("Latest Forge version from Maven: {}", latest);
                    return latest;
                }
            } else {
                logger.warn("Forge Maven request failed: {} - {}", response.code(), response.message());
            }
        } catch (Exception e) {
            logger.error("Error parsing Forge Maven metadata: {}", e.getMessage());
        }
        
        throw new IOException("Failed to fetch Forge version from Maven metadata");
    }
    
    private List<String> getFabricVersionsForMinecraft(String minecraftVersion) throws IOException {
        List<String> versions = new ArrayList<>();
        
        Request request = new Request.Builder()
            .url(FABRIC_API_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .addHeader("Accept", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body().string();
                logger.debug("Fabric API response: {}", jsonResponse);
                
                JsonArray fabricVersions = JsonParser.parseString(jsonResponse).getAsJsonArray();
                
                for (JsonElement element : fabricVersions) {
                    JsonObject versionObj = element.getAsJsonObject();
                    String gameVersion = versionObj.get("gameVersion").getAsString();
                    String loaderVersion = versionObj.get("loader").getAsJsonObject().get("version").getAsString();
                    
                    if (gameVersion.equals(minecraftVersion) && versionObj.get("loader").getAsJsonObject().get("stable").getAsBoolean()) {
                        versions.add(loaderVersion);
                    }
                }
                
                logger.info("Found {} Fabric versions for Minecraft {}", versions.size(), minecraftVersion);
            }
        } catch (Exception e) {
            logger.error("Error parsing Fabric API response: {}", e.getMessage());
        }
        
        // If no versions found from API, use hardcoded versions
        if (versions.isEmpty()) {
            versions.addAll(getHardcodedFabricVersions(minecraftVersion));
        }
        
        return versions;
    }
    
    private String getLatestFabricVersion() throws IOException {
        logger.info("Fetching latest Fabric version from API...");
        
        Request request = new Request.Builder()
            .url(FABRIC_API_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .addHeader("Accept", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body().string();
                logger.debug("Fabric API response: {}", jsonResponse);
                
                JsonArray fabricVersions = JsonParser.parseString(jsonResponse).getAsJsonArray();
                
                for (JsonElement element : fabricVersions) {
                    JsonObject versionObj = element.getAsJsonObject();
                    JsonObject loaderObj = versionObj.get("loader").getAsJsonObject();
                    
                    if (loaderObj.get("stable").getAsBoolean()) {
                        String latest = loaderObj.get("version").getAsString();
                        logger.info("Latest Fabric version from API: {}", latest);
                        return latest;
                    }
                }
            } else {
                logger.warn("Fabric API request failed: {} - {}", response.code(), response.message());
            }
        } catch (Exception e) {
            logger.error("Error parsing Fabric API response: {}", e.getMessage());
        }
        
        throw new IOException("Failed to fetch Fabric version from API");
    }
    
    private List<String> getQuiltVersionsForMinecraft(String minecraftVersion) throws IOException {
        List<String> versions = new ArrayList<>();
        
        Request request = new Request.Builder()
            .url(QUILT_API_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .addHeader("Accept", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body().string();
                logger.debug("Quilt API response: {}", jsonResponse);
                
                JsonArray quiltVersions = JsonParser.parseString(jsonResponse).getAsJsonArray();
                
                for (JsonElement element : quiltVersions) {
                    JsonObject versionObj = element.getAsJsonObject();
                    String gameVersion = versionObj.get("gameVersion").getAsString();
                    String loaderVersion = versionObj.get("loader").getAsJsonObject().get("version").getAsString();
                    
                    if (gameVersion.equals(minecraftVersion) && versionObj.get("loader").getAsJsonObject().get("stable").getAsBoolean()) {
                        versions.add(loaderVersion);
                    }
                }
                
                logger.info("Found {} Quilt versions for Minecraft {}", versions.size(), minecraftVersion);
            }
        } catch (Exception e) {
            logger.error("Error parsing Quilt API response: {}", e.getMessage());
        }
        
        // If no versions found from API, use hardcoded versions
        if (versions.isEmpty()) {
            versions.addAll(getHardcodedQuiltVersions(minecraftVersion));
        }
        
        return versions;
    }
    
    private String getLatestQuiltVersion() throws IOException {
        logger.info("Fetching latest Quilt version from API...");
        
        Request request = new Request.Builder()
            .url(QUILT_API_URL)
            .addHeader("User-Agent", "StellarServerForge/1.0.0")
            .addHeader("Accept", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body().string();
                logger.debug("Quilt API response: {}", jsonResponse);
                
                JsonArray quiltVersions = JsonParser.parseString(jsonResponse).getAsJsonArray();
                
                for (JsonElement element : quiltVersions) {
                    JsonObject versionObj = element.getAsJsonObject();
                    JsonObject loaderObj = versionObj.get("loader").getAsJsonObject();
                    
                    if (loaderObj.get("stable").getAsBoolean()) {
                        String latest = loaderObj.get("version").getAsString();
                        logger.info("Latest Quilt version from API: {}", latest);
                        return latest;
                    }
                }
            } else {
                logger.warn("Quilt API request failed: {} - {}", response.code(), response.message());
            }
        } catch (Exception e) {
            logger.error("Error parsing Quilt API response: {}", e.getMessage());
        }
        
        throw new IOException("Failed to fetch Quilt version from API");
    }
    
    // Helper methods for hardcoded versions
    private List<String> getHardcodedNeoForgeVersions(String minecraftVersion) {
        List<String> versions = new ArrayList<>();
        
        switch (minecraftVersion) {
            case "1.21.1":
                versions.add("21.1.65");
                versions.add("21.1.64");
                versions.add("21.1.63");
                break;
            case "1.21":
                versions.add("21.0.167");
                versions.add("21.0.166");
                versions.add("21.0.165");
                break;
            case "1.20.6":
                versions.add("20.6.119");
                versions.add("20.6.118");
                versions.add("20.6.117");
                break;
            case "1.20.4":
                versions.add("20.4.237");
                versions.add("20.4.236");
                versions.add("20.4.235");
                break;
            case "1.20.1":
                versions.add("47.1.104");
                versions.add("47.1.103");
                versions.add("47.1.100");
                break;
            default:
                versions.add("Latest");
                break;
        }
        
        return versions;
    }
    
    private List<String> getHardcodedForgeVersions(String minecraftVersion) {
        List<String> versions = new ArrayList<>();
        
        switch (minecraftVersion) {
            case "1.21.1":
                versions.add("1.21.1-52.0.17");
                versions.add("1.21.1-52.0.16");
                versions.add("1.21.1-52.0.15");
                break;
            case "1.20.1":
                versions.add("1.20.1-47.2.0");
                versions.add("1.20.1-47.1.0");
                versions.add("1.20.1-47.0.35");
                break;
            case "1.19.4":
                versions.add("1.19.4-45.1.0");
                versions.add("1.19.4-45.0.64");
                versions.add("1.19.4-45.0.43");
                break;
            case "1.18.2":
                versions.add("1.18.2-40.2.0");
                versions.add("1.18.2-40.1.0");
                break;
            case "1.16.5":
                versions.add("1.16.5-36.2.0");
                versions.add("1.16.5-36.1.0");
                break;
            default:
                versions.add("Latest");
                break;
        }
        
        return versions;
    }
    
    private List<String> getHardcodedFabricVersions(String minecraftVersion) {
        List<String> versions = new ArrayList<>();
        
        // Fabric versions are usually stable across MC versions
        versions.add("0.16.5");
        versions.add("0.16.4");
        versions.add("0.16.3");
        versions.add("0.16.2");
        versions.add("0.15.11");
        
        return versions;
    }
    
    private List<String> getHardcodedQuiltVersions(String minecraftVersion) {
        List<String> versions = new ArrayList<>();
        
        // Quilt versions are usually stable across MC versions
        versions.add("0.26.4");
        versions.add("0.26.3");
        versions.add("0.26.2");
        versions.add("0.26.1");
        versions.add("0.25.15");
        
        return versions;
    }
    
    private String getFallbackVersionForMinecraft(String modLoader, String minecraftVersion) {
        switch (modLoader.toLowerCase()) {
            case "neoforge":
                return "Latest";
            case "forge":
                return "Latest";
            case "fabric":
                return "0.16.5";
            case "quilt":
                return "0.26.4";
            default:
                return "Latest";
        }
    }
    
    private int compareVersions(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        
        int maxLength = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionNumber(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionNumber(parts2[i]) : 0;
            
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        
        return 0;
    }
    
    private int parseVersionNumber(String part) {
        try {
            // Extract numeric part (handle cases like "1.21.1-52.0.17")
            String[] split = part.split("-");
            return Integer.parseInt(split[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

