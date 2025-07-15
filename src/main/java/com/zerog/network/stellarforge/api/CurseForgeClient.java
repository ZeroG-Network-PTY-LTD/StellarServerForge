package com.zerog.network.stellarforge.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zerog.network.stellarforge.config.SecureConfig;
import com.zerog.network.stellarforge.model.ModInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CurseForge API client for Stellar Server Forge
 * Securely handles API authentication using external configuration
 */
public class CurseForgeClient {
    private static final Logger logger = LoggerFactory.getLogger(CurseForgeClient.class);
    private static final String API_BASE_URL = "https://api.curseforge.com/v1";
    private static final int MINECRAFT_GAME_ID = 432;
    private static final String USER_AGENT = "StellarServerForge/1.0.0 (ZeroG Network)";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    
    public CurseForgeClient() {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        
        // Get API key from secure configuration
        try {
            this.apiKey = SecureConfig.getInstance().getCurseForgeApiKey();
            logger.info("CurseForge API client initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize CurseForge API client: {}", e.getMessage());
            throw new RuntimeException("CurseForge API client initialization failed", e);
        }
    }
    
    /**
     * Check if CurseForge API is available
     */
    public boolean isAvailable() {
        return SecureConfig.getInstance().isCurseForgeEnabled();
    }
    
    /**
     * Search for mods on CurseForge
     */
    public List<ModInfo> searchMods(String query, String minecraftVersion, String modLoader, int limit) {
        List<ModInfo> mods = new ArrayList<>();
        
        if (!isAvailable()) {
            logger.warn("CurseForge API is not available or not configured");
            return mods;
        }
        
        try {
            // URL encode the search query
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(API_BASE_URL)
                     .append("/mods/search")
                     .append("?gameId=").append(MINECRAFT_GAME_ID)
                     .append("&searchFilter=").append(encodedQuery)
                     .append("&pageSize=").append(limit)
                     .append("&sortField=2")
                     .append("&sortOrder=desc")
                     .append("&classId=6"); // Mods category
            
            if (minecraftVersion != null && !minecraftVersion.isEmpty()) {
                urlBuilder.append("&gameVersion=").append(URLEncoder.encode(minecraftVersion, StandardCharsets.UTF_8));
            }
            
            if (modLoader != null && !modLoader.isEmpty()) {
                int modLoaderTypeId = getModLoaderTypeId(modLoader);
                urlBuilder.append("&modLoaderType=").append(modLoaderTypeId);
            }
            
            Request request = new Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("Accept", "application/json")
                .addHeader("x-api-key", apiKey)
                .addHeader("User-Agent", USER_AGENT)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    
                    if (jsonResponse.has("data")) {
                        JsonArray dataArray = jsonResponse.getAsJsonArray("data");
                        for (JsonElement element : dataArray) {
                            JsonObject modJson = element.getAsJsonObject();
                            ModInfo mod = parseModFromJson(modJson);
                            if (mod != null) {
                                mods.add(mod);
                            }
                        }
                    }
                    
                    logger.info("Successfully searched CurseForge, found {} mods", mods.size());
                } else {
                    logger.warn("CurseForge API request failed with status: {}", response.code());
                }
            }
        } catch (IOException e) {
            logger.error("Error searching mods on CurseForge", e);
        }
        
        return mods;
    }
    
    /**
     * Get suggested mods for a specific modloader and version
     */
    public List<ModInfo> getSuggestedMods(String minecraftVersion, String modLoader) {
        List<ModInfo> suggestedMods = new ArrayList<>();
        
        if (!isAvailable()) {
            logger.warn("CurseForge API is not available for getting suggested mods");
            return suggestedMods;
        }
        
        // Popular utility and performance mods for different mod loaders
        String[] popularMods;
        
        if ("fabric".equalsIgnoreCase(modLoader)) {
            popularMods = new String[]{
                "Fabric API", "Lithium", "Phosphor", "Sodium", "Iris", "ModMenu",
                "Roughly Enough Items", "Cloth Config", "Architectury API", "JourneyMap"
            };
        } else if ("quilt".equalsIgnoreCase(modLoader)) {
            popularMods = new String[]{
                "Quilt Standard Libraries", "Quilted Fabric API", "Sodium", "Lithium",
                "Phosphor", "Iris", "ModMenu", "Roughly Enough Items", "JourneyMap"
            };
        } else {
            // Forge/NeoForge mods
            popularMods = new String[]{
                "JEI", "Biomes O' Plenty", "Tinkers' Construct", "Applied Energistics 2",
                "Thermal Expansion", "Iron Chests", "Waystones", "JourneyMap", "Create",
                "Botania", "Twilight Forest", "Mantle", "Bookshelf", "Placebo"
            };
        }
        
        for (String modName : popularMods) {
            try {
                List<ModInfo> searchResults = searchMods(modName, minecraftVersion, modLoader, 1);
                if (!searchResults.isEmpty()) {
                    suggestedMods.add(searchResults.get(0));
                }
                
                // Small delay to avoid rate limiting
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        logger.info("Retrieved {} suggested mods from CurseForge", suggestedMods.size());
        return suggestedMods;
    }
    
    /**
     * Get download URL for a mod
     */
    public String getModDownloadUrl(String projectId, String fileId) {
        if (!isAvailable()) {
            logger.warn("CurseForge API is not available for getting download URL");
            return null;
        }
        
        try {
            String url = String.format("%s/mods/%s/files/%s/download-url", API_BASE_URL, projectId, fileId);
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("x-api-key", apiKey)
                .addHeader("User-Agent", USER_AGENT)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    
                    if (jsonResponse.has("data")) {
                        String downloadUrl = jsonResponse.get("data").getAsString();
                        logger.debug("Retrieved download URL for mod {}/{}", projectId, fileId);
                        return downloadUrl;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error getting mod download URL from CurseForge", e);
        }
        
        return null;
    }
    
    /**
     * Get mod details by project ID
     */
    public ModInfo getModDetails(String projectId) {
        if (!isAvailable()) {
            return null;
        }
        
        try {
            String url = String.format("%s/mods/%s", API_BASE_URL, projectId);
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("x-api-key", apiKey)
                .addHeader("User-Agent", USER_AGENT)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    
                    if (jsonResponse.has("data")) {
                        JsonObject modJson = jsonResponse.getAsJsonObject("data");
                        return parseModFromJson(modJson);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error getting mod details from CurseForge", e);
        }
        
        return null;
    }
    
    /**
     * Get mod info by project ID and file ID
     */
    public ModInfo getModInfo(int projectId, int fileId) {
        if (!isAvailable()) {
            logger.warn("CurseForge API is not available or not configured");
            return null;
        }
        
        try {
            // First get project info
            String projectUrl = String.format("%s/mods/%d", API_BASE_URL, projectId);
            Request projectRequest = new Request.Builder()
                .url(projectUrl)
                .addHeader("Accept", "application/json")
                .addHeader("x-api-key", this.apiKey)
                .build();
            
            Response projectResponse = httpClient.newCall(projectRequest).execute();
            if (!projectResponse.isSuccessful()) {
                logger.error("Failed to fetch project info: {}", projectResponse.code());
                return null;
            }
            
            JsonObject projectData = gson.fromJson(projectResponse.body().string(), JsonObject.class);
            JsonObject project = projectData.getAsJsonObject("data");
            
            // Then get file info
            String fileUrl = String.format("%s/mods/%d/files/%d", API_BASE_URL, projectId, fileId);
            Request fileRequest = new Request.Builder()
                .url(fileUrl)
                .addHeader("Accept", "application/json")
                .addHeader("x-api-key", this.apiKey)
                .build();
            
            Response fileResponse = httpClient.newCall(fileRequest).execute();
            if (!fileResponse.isSuccessful()) {
                logger.error("Failed to fetch file info: {}", fileResponse.code());
                return null;
            }
            
            JsonObject fileData = gson.fromJson(fileResponse.body().string(), JsonObject.class);
            JsonObject file = fileData.getAsJsonObject("data");
            
            // Create ModInfo
            ModInfo mod = new ModInfo();
            mod.setName(project.get("name").getAsString());
            mod.setProjectId(String.valueOf(projectId));
            mod.setFileId(String.valueOf(fileId));
            mod.setFileName(file.get("fileName").getAsString());
            mod.setUrl(file.get("downloadUrl").getAsString());
            mod.setFileSize(file.get("fileLength").getAsLong());
            mod.setDescription(project.get("summary").getAsString());
            mod.setSource(ModInfo.ModSource.CURSEFORGE);
            mod.setPlatform("curseforge");
            
            // Extract server-side compatibility
            if (project.has("gameVersionLatestFiles")) {
                JsonArray gameVersions = project.getAsJsonArray("gameVersionLatestFiles");
                for (int i = 0; i < gameVersions.size(); i++) {
                    JsonObject gameVersion = gameVersions.get(i).getAsJsonObject();
                    if (gameVersion.has("projectFileId") && 
                        gameVersion.get("projectFileId").getAsInt() == fileId) {
                        
                        if (gameVersion.has("serverPackFileId")) {
                            mod.setServerCompatible(true);
                            mod.setServerSide(true);
                        }
                        break;
                    }
                }
            }
            
            return mod;
            
        } catch (Exception e) {
            logger.error("Error fetching mod info for project {} file {}: {}", projectId, fileId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Search for mod by slug
     */
    public ModInfo searchModBySlug(String slug, String minecraftVersion) {
        if (!isAvailable()) {
            logger.warn("CurseForge API is not available or not configured");
            return null;
        }
        
        try {
            // Search for mod by slug
            String searchUrl = String.format("%s/mods/search?gameId=432&slug=%s", API_BASE_URL, 
                URLEncoder.encode(slug, StandardCharsets.UTF_8));
            
            Request request = new Request.Builder()
                .url(searchUrl)
                .addHeader("Accept", "application/json")
                .addHeader("x-api-key", this.apiKey)
                .build();
            
            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                logger.error("Failed to search for mod by slug: {}", response.code());
                return null;
            }
            
            JsonObject jsonResponse = gson.fromJson(response.body().string(), JsonObject.class);
            JsonArray data = jsonResponse.getAsJsonArray("data");
            
            if (data.size() == 0) {
                logger.warn("No mod found with slug: {}", slug);
                return null;
            }
            
            JsonObject mod = data.get(0).getAsJsonObject();
            
            // Get latest file for the Minecraft version
            JsonArray latestFiles = mod.getAsJsonArray("latestFiles");
            JsonObject latestFile = null;
            
            for (int i = 0; i < latestFiles.size(); i++) {
                JsonObject file = latestFiles.get(i).getAsJsonObject();
                JsonArray gameVersions = file.getAsJsonArray("gameVersions");
                
                for (int j = 0; j < gameVersions.size(); j++) {
                    if (gameVersions.get(j).getAsString().equals(minecraftVersion)) {
                        latestFile = file;
                        break;
                    }
                }
                
                if (latestFile != null) break;
            }
            
            if (latestFile == null && latestFiles.size() > 0) {
                latestFile = latestFiles.get(0).getAsJsonObject();
            }
            
            if (latestFile != null) {
                ModInfo modInfo = new ModInfo();
                modInfo.setName(mod.get("name").getAsString());
                modInfo.setProjectId(mod.get("id").getAsString());
                modInfo.setFileId(latestFile.get("id").getAsString());
                modInfo.setFileName(latestFile.get("fileName").getAsString());
                modInfo.setUrl(latestFile.get("downloadUrl").getAsString());
                modInfo.setFileSize(latestFile.get("fileLength").getAsLong());
                modInfo.setDescription(mod.get("summary").getAsString());
                modInfo.setSource(ModInfo.ModSource.CURSEFORGE);
                modInfo.setPlatform("curseforge");
                modInfo.setSlug(slug);
                
                return modInfo;
            }
            
        } catch (Exception e) {
            logger.error("Error searching for mod by slug {}: {}", slug, e.getMessage());
        }
        
        return null;
    }
    
    private ModInfo parseModFromJson(JsonObject modJson) {
        try {
            ModInfo mod = new ModInfo();
            mod.setName(modJson.get("name").getAsString());
            mod.setDescription(modJson.get("summary").getAsString());
            mod.setProjectId(modJson.get("id").getAsString());
            mod.setSource(ModInfo.ModSource.CURSEFORGE);
            
            // Set website URL
            if (modJson.has("links") && modJson.getAsJsonObject("links").has("websiteUrl")) {
                mod.setUrl(modJson.getAsJsonObject("links").get("websiteUrl").getAsString());
            }
            
            // Get latest file info
            if (modJson.has("latestFiles") && modJson.getAsJsonArray("latestFiles").size() > 0) {
                JsonObject latestFile = modJson.getAsJsonArray("latestFiles").get(0).getAsJsonObject();
                mod.setFileName(latestFile.get("fileName").getAsString());
                mod.setFileId(latestFile.get("id").getAsString());
                mod.setFileSize(latestFile.get("fileLength").getAsLong());
                
                // Get download URL if available
                if (latestFile.has("downloadUrl")) {
                    mod.setUrl(latestFile.get("downloadUrl").getAsString());
                }
                
                // Get game versions
                if (latestFile.has("gameVersions") && latestFile.getAsJsonArray("gameVersions").size() > 0) {
                    mod.setMinecraftVersion(latestFile.getAsJsonArray("gameVersions").get(0).getAsString());
                }
            }
            
            return mod;
        } catch (Exception e) {
            logger.error("Error parsing mod from JSON", e);
            return null;
        }
    }
    
    private int getModLoaderTypeId(String modLoader) {
        switch (modLoader.toLowerCase()) {
            case "forge": return 1;
            case "cauldron": return 2;
            case "liteloader": return 3;
            case "fabric": return 4;
            case "quilt": return 5;
            case "neoforge": return 6;
            default: 
                logger.warn("Unknown mod loader: {}, defaulting to Forge", modLoader);
                return 1;
        }
    }
}
