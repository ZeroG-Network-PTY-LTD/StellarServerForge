package com.universalator.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.universalator.model.ModInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CurseForge API client
 */
public class CurseForgeClient {
    private static final Logger logger = LoggerFactory.getLogger(CurseForgeClient.class);
    private static final String API_BASE_URL = "https://api.curseforge.com/v1";
    private static final String API_KEY = "$2a$10$wWqDGJXsRy0FhxdnzTCJLO4hDmAQJIjkgTVQFBZJZqOdGQoIUNKHK"; // Default API key
    private static final int MINECRAFT_GAME_ID = 432;
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    public CurseForgeClient() {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * Search for mods on CurseForge
     */
    public List<ModInfo> searchMods(String query, String minecraftVersion, String modLoader, int limit) {
        List<ModInfo> mods = new ArrayList<>();
        
        try {
            String url = String.format("%s/mods/search?gameId=%d&searchFilter=%s&pageSize=%d&sortField=2&sortOrder=desc",
                API_BASE_URL, MINECRAFT_GAME_ID, query, limit);
            
            if (minecraftVersion != null && !minecraftVersion.isEmpty()) {
                url += "&gameVersion=" + minecraftVersion;
            }
            
            if (modLoader != null && !modLoader.isEmpty()) {
                url += "&modLoaderType=" + getModLoaderTypeId(modLoader);
            }
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("x-api-key", API_KEY)
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
        
        // Popular utility and performance mods
        String[] popularMods = {
            "JEI", "Optifine", "Biomes O' Plenty", "Tinkers' Construct", "Applied Energistics 2",
            "Thermal Expansion", "Iron Chests", "Waystones", "JourneyMap", "Pam's HarvestCraft"
        };
        
        for (String modName : popularMods) {
            List<ModInfo> searchResults = searchMods(modName, minecraftVersion, modLoader, 1);
            if (!searchResults.isEmpty()) {
                suggestedMods.add(searchResults.get(0));
            }
        }
        
        return suggestedMods;
    }
    
    /**
     * Get download URL for a mod
     */
    public String getModDownloadUrl(String projectId, String fileId) {
        try {
            String url = String.format("%s/mods/%s/files/%s/download-url", API_BASE_URL, projectId, fileId);
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("x-api-key", API_KEY)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    
                    if (jsonResponse.has("data")) {
                        return jsonResponse.get("data").getAsString();
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error getting mod download URL from CurseForge", e);
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
            
            // Get latest file info
            if (modJson.has("latestFiles") && modJson.getAsJsonArray("latestFiles").size() > 0) {
                JsonObject latestFile = modJson.getAsJsonArray("latestFiles").get(0).getAsJsonObject();
                mod.setFileName(latestFile.get("fileName").getAsString());
                mod.setFileId(latestFile.get("id").getAsString());
                mod.setFileSize(latestFile.get("fileLength").getAsLong());
                mod.setUrl(latestFile.get("downloadUrl").getAsString());
                
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
            case "fabric": return 4;
            case "quilt": return 5;
            case "neoforge": return 6;
            default: return 1;
        }
    }
}
