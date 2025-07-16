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
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Improved version fetcher with better error handling and API integration
 */
public class ImprovedVersionFetcher {
    private static final Logger logger = LoggerFactory.getLogger(ImprovedVersionFetcher.class);
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    // API endpoints
    private static final String NEOFORGE_MAVEN_URL = "https://maven.neoforged.net/net/neoforged/neoforge/maven-metadata.xml";
    private static final String FORGE_MAVEN_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml";
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
    
    /**
     * Get all available versions for a mod loader filtered by Minecraft version
     */
    public List<String> getAllVersionsForMinecraft(String modLoader, String minecraftVersion) {
        List<String> versions = new ArrayList<>();
        
        try {
            switch (modLoader.toLowerCase()) {
                case "neoforge":
                    versions = getNeoForgeVersionsForMinecraft(minecraftVersion);
                    break;
                case "forge":
                    versions = getForgeVersionsForMinecraft(minecraftVersion);
                    break;
                case "fabric":
                    versions = getFabricVersionsForMinecraft(minecraftVersion);
                    break;
                case "quilt":
                    versions = getQuiltVersionsForMinecraft(minecraftVersion);
                    break;
                default:
                    logger.warn("Unknown mod loader: {}", modLoader);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error fetching versions for {} on Minecraft {}: {}", modLoader, minecraftVersion, e.getMessage());
        }
        
        // Add fallback if empty
        if (versions.isEmpty()) {
            versions.add(getFallbackVersionForMinecraft(modLoader, minecraftVersion));
        }
        
        return versions;
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
        
        // Fallback to hardcoded version
        return "47.3.10";
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
        
        // If no versions found from API, use hardcoded versions
        if (versions.isEmpty()) {
            versions.add("47.3.10");
            versions.add("47.3.7");
            versions.add("47.3.5");
            versions.add("47.3.0");
            versions.add("47.2.23");
            versions.add("47.2.20");
            versions.add("47.2.17");
            versions.add("47.2.0");
        }
        
        // Sort versions in descending order (newest first)
        versions.sort((a, b) -> compareVersions(b, a));
        
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
    
    private List<String> getHardcodedNeoForgeVersions(String minecraftVersion) {
        List<String> versions = new ArrayList<>();
        
        switch (minecraftVersion) {
            case "1.21.1":
                versions.add("21.1.65");
                versions.add("21.1.60");
                versions.add("21.1.55");
                break;
            case "1.21":
                versions.add("21.0.167");
                versions.add("21.0.162");
                versions.add("21.0.150");
                break;
            case "1.20.6":
                versions.add("20.6.139");
                versions.add("20.6.130");
                versions.add("20.6.120");
                break;
            case "1.20.5":
                versions.add("20.5.21");
                versions.add("20.5.14");
                versions.add("20.5.10");
                break;
            case "1.20.4":
                versions.add("20.4.237");
                versions.add("20.4.234");
                versions.add("20.4.230");
                break;
            case "1.20.3":
                versions.add("20.3.7");
                versions.add("20.3.5");
                versions.add("20.3.3");
                break;
            case "1.20.2":
                versions.add("20.2.88");
                versions.add("20.2.86");
                versions.add("20.2.84");
                break;
            case "1.20.1":
                versions.add("47.3.10");
                versions.add("47.3.7");
                versions.add("47.3.5");
                versions.add("47.3.0");
                versions.add("47.2.23");
                versions.add("47.2.20");
                versions.add("47.2.17");
                versions.add("47.2.0");
                versions.add("47.1.100");
                versions.add("47.1.79");
                versions.add("47.1.46");
                break;
            default:
                versions.add("Latest");
                break;
        }
        
        return versions;
    }
    
    private List<String> getForgeVersionsForMinecraft(String minecraftVersion) throws IOException {
        List<String> versions = new ArrayList<>();
        
        // Hardcoded Forge versions for different MC versions
        switch (minecraftVersion) {
            case "1.21.1":
                versions.add("52.0.19");
                versions.add("52.0.15");
                versions.add("52.0.12");
                break;
            case "1.21":
                versions.add("51.0.33");
                versions.add("51.0.30");
                versions.add("51.0.22");
                break;
            case "1.20.6":
                versions.add("50.1.0");
                versions.add("50.0.13");
                versions.add("50.0.7");
                break;
            case "1.20.5":
                versions.add("50.0.13");
                versions.add("50.0.7");
                versions.add("50.0.1");
                break;
            case "1.20.4":
                versions.add("49.1.0");
                versions.add("49.0.50");
                versions.add("49.0.48");
                versions.add("49.0.44");
                break;
            case "1.20.3":
                versions.add("49.0.3");
                versions.add("49.0.2");
                versions.add("49.0.1");
                break;
            case "1.20.2":
                versions.add("48.1.0");
                versions.add("48.0.48");
                versions.add("48.0.42");
                versions.add("48.0.40");
                break;
            case "1.20.1":
                versions.add("47.3.10");
                versions.add("47.3.7");
                versions.add("47.3.5");
                versions.add("47.3.0");
                versions.add("47.2.23");
                versions.add("47.2.20");
                versions.add("47.2.17");
                versions.add("47.2.0");
                versions.add("47.1.0");
                break;
            case "1.19.4":
                versions.add("45.3.0");
                versions.add("45.2.0");
                versions.add("45.1.0");
                break;
            case "1.19.3":
                versions.add("44.1.23");
                versions.add("44.1.0");
                break;
            case "1.19.2":
                versions.add("43.4.0");
                versions.add("43.3.13");
                versions.add("43.3.0");
                versions.add("43.2.0");
                break;
            case "1.18.2":
                versions.add("40.2.21");
                versions.add("40.2.0");
                versions.add("40.1.0");
                break;
            case "1.17.1":
                versions.add("37.1.1");
                versions.add("37.1.0");
                versions.add("37.0.109");
                break;
            case "1.16.5":
                versions.add("36.2.42");
                versions.add("36.2.39");
                versions.add("36.2.35");
                versions.add("36.2.0");
                break;
            case "1.12.2":
                versions.add("14.23.5.2860");
                versions.add("14.23.5.2859");
                versions.add("14.23.5.2858");
                break;
            default:
                versions.add("Latest");
                break;
        }
        
        return versions;
    }
    
    private List<String> getFabricVersionsForMinecraft(String minecraftVersion) throws IOException {
        List<String> versions = new ArrayList<>();
        
        // Fabric loader versions are mostly universal but here are common stable ones
        versions.add("0.16.5");
        versions.add("0.16.4");
        versions.add("0.16.3");
        versions.add("0.16.2");
        versions.add("0.16.1");
        versions.add("0.16.0");
        versions.add("0.15.11");
        versions.add("0.15.10");
        versions.add("0.15.9");
        versions.add("0.15.8");
        versions.add("0.15.7");
        versions.add("0.15.6");
        versions.add("0.15.5");
        versions.add("0.15.4");
        versions.add("0.15.3");
        versions.add("0.15.2");
        versions.add("0.15.1");
        versions.add("0.15.0");
        
        return versions;
    }
    
    private List<String> getQuiltVersionsForMinecraft(String minecraftVersion) throws IOException {
        List<String> versions = new ArrayList<>();
        
        // Quilt loader versions
        versions.add("0.26.4");
        versions.add("0.26.3");
        versions.add("0.26.2");
        versions.add("0.26.1");
        versions.add("0.26.0");
        versions.add("0.25.7");
        versions.add("0.25.6");
        versions.add("0.25.5");
        versions.add("0.25.4");
        versions.add("0.25.3");
        versions.add("0.25.2");
        versions.add("0.25.1");
        versions.add("0.25.0");
        versions.add("0.24.0");
        versions.add("0.23.1");
        versions.add("0.23.0");
        
        return versions;
    }
    
    private String getFallbackVersionForMinecraft(String modLoader, String minecraftVersion) {
        switch (modLoader.toLowerCase()) {
            case "neoforge":
                switch (minecraftVersion) {
                    case "1.21.1": return "21.1.65";
                    case "1.21": return "21.0.167";
                    case "1.20.6": return "20.6.139";
                    case "1.20.5": return "20.5.21";
                    case "1.20.4": return "20.4.237";
                    case "1.20.3": return "20.3.7";
                    case "1.20.2": return "20.2.88";
                    case "1.20.1": return "47.3.10";
                    default: return "47.3.10";
                }
            case "forge":
                switch (minecraftVersion) {
                    case "1.21.1": return "52.0.19";
                    case "1.21": return "51.0.33";
                    case "1.20.6": return "50.1.0";
                    case "1.20.5": return "50.0.13";
                    case "1.20.4": return "49.1.0";
                    case "1.20.3": return "49.0.3";
                    case "1.20.2": return "48.1.0";
                    case "1.20.1": return "47.3.10";
                    case "1.19.4": return "45.3.0";
                    case "1.19.2": return "43.4.0";
                    case "1.18.2": return "40.2.21";
                    case "1.17.1": return "37.1.1";
                    case "1.16.5": return "36.2.42";
                    case "1.12.2": return "14.23.5.2860";
                    default: return "47.3.10";
                }
            case "fabric":
                return "0.16.5";
            case "quilt":
                return "0.26.4";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Compare two version strings, handling NeoForge format (21.1.65) and Forge format (1.20.1-47.2.0)
     */
    private int compareVersions(String version1, String version2) {
        try {
            // Handle NeoForge format (21.1.65)
            if (version1.matches("\\d+\\.\\d+\\.\\d+") && version2.matches("\\d+\\.\\d+\\.\\d+")) {
                return compareSemanticVersions(version1, version2);
            }
            
            // Handle Forge format (1.20.1-47.2.0)
            if (version1.contains("-") && version2.contains("-")) {
                String[] v1Parts = version1.split("-");
                String[] v2Parts = version2.split("-");
                
                // Compare Minecraft version first
                int mcComparison = compareSemanticVersions(v1Parts[0], v2Parts[0]);
                if (mcComparison != 0) return mcComparison;
                
                // If MC versions are the same, compare Forge version
                return compareSemanticVersions(v1Parts[1], v2Parts[1]);
            }
            
            // Fallback to semantic version comparison
            return compareSemanticVersions(version1, version2);
            
        } catch (Exception e) {
            logger.warn("Error comparing versions {} and {}: {}", version1, version2, e.getMessage());
            return version1.compareTo(version2);
        }
    }
    
    private int compareSemanticVersions(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");
        
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
