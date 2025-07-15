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
 * Modrinth API client for Stellar Server Forge
 */
public class ModrinthClient {
    private static final Logger logger = LoggerFactory.getLogger(ModrinthClient.class);
    private static final String API_BASE_URL = "https://api.modrinth.com/v2";
    private static final String USER_AGENT = "StellarServerForge/1.0.0 (ZeroG Network)";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    public ModrinthClient() {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * Check if Modrinth API is available
     */
    public boolean isAvailable() {
        return SecureConfig.getInstance().isModrinthEnabled();
    }
    
    /**
     * Search for mods on Modrinth
     */
    public List<ModInfo> searchMods(String query, String minecraftVersion, String modLoader, int limit) {
        List<ModInfo> mods = new ArrayList<>();
        
        if (!isAvailable()) {
            logger.warn("Modrinth API is not available or disabled");
            return mods;
        }
        
        try {
            StringBuilder urlBuilder = new StringBuilder(API_BASE_URL + "/search");
            urlBuilder.append("?query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
            urlBuilder.append("&limit=").append(limit);
            urlBuilder.append("&facets=[[\"project_type:mod\"]]");
            
            if (minecraftVersion != null && !minecraftVersion.isEmpty()) {
                urlBuilder.append("&facets=[[\"versions:").append(URLEncoder.encode(minecraftVersion, StandardCharsets.UTF_8)).append("\"]]");
            }
            
            if (modLoader != null && !modLoader.isEmpty()) {
                urlBuilder.append("&facets=[[\"categories:").append(URLEncoder.encode(modLoader.toLowerCase(), StandardCharsets.UTF_8)).append("\"]]");
            }
            
            Request.Builder requestBuilder = new Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", USER_AGENT);
            
            // Add API key if available
            String apiKey = SecureConfig.getInstance().getModrinthApiKey();
            if (apiKey != null && !apiKey.isEmpty()) {
                requestBuilder.addHeader("Authorization", apiKey);
            }
            
            Request request = requestBuilder.build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    
                    if (jsonResponse.has("hits")) {
                        JsonArray hitsArray = jsonResponse.getAsJsonArray("hits");
                        for (JsonElement element : hitsArray) {
                            JsonObject modJson = element.getAsJsonObject();
                            ModInfo mod = parseModFromJson(modJson);
                            if (mod != null) {
                                mods.add(mod);
                            }
                        }
                    }
                    
                    logger.info("Successfully searched Modrinth, found {} mods", mods.size());
                } else {
                    logger.warn("Modrinth API request failed with status: {}", response.code());
                }
            }
        } catch (IOException e) {
            logger.error("Error searching mods on Modrinth", e);
        }
        
        return mods;
    }
    
    /**
     * Get suggested mods for a specific modloader and version
     */
    public List<ModInfo> getSuggestedMods(String minecraftVersion, String modLoader) {
        List<ModInfo> suggestedMods = new ArrayList<>();
        
        if (!isAvailable()) {
            logger.warn("Modrinth API is not available for getting suggested mods");
            return suggestedMods;
        }
        
        // Popular mods for different loaders
        String[] popularMods;
        
        if ("fabric".equalsIgnoreCase(modLoader)) {
            popularMods = new String[]{
                "Fabric API", "Lithium", "Phosphor", "Sodium", "Iris", "ModMenu",
                "Roughly Enough Items", "Cloth Config", "Architectury API", "JourneyMap",
                "Mod Menu", "Fabric Language Kotlin", "Indium", "Continuity"
            };
        } else if ("quilt".equalsIgnoreCase(modLoader)) {
            popularMods = new String[]{
                "Quilt Standard Libraries", "Quilted Fabric API", "Sodium", "Lithium",
                "Phosphor", "Iris", "ModMenu", "Roughly Enough Items", "JourneyMap",
                "Quilt Kotlin Libraries", "Continuity", "Indium"
            };
        } else {
            // Forge/NeoForge - fewer mods available on Modrinth
            popularMods = new String[]{
                "Create", "Botania", "Applied Energistics 2", "JourneyMap",
                "Biomes O' Plenty", "Twilight Forest", "Iron Chests", "Waystones"
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
        
        logger.info("Retrieved {} suggested mods from Modrinth", suggestedMods.size());
        return suggestedMods;
    }
    
    /**
     * Get project versions for a specific project
     */
    public List<String> getProjectVersions(String projectId, String minecraftVersion, String modLoader) {
        List<String> versions = new ArrayList<>();
        
        if (!isAvailable()) {
            return versions;
        }
        
        try {
            StringBuilder urlBuilder = new StringBuilder(API_BASE_URL + "/project/" + projectId + "/version");
            List<String> params = new ArrayList<>();
            
            if (minecraftVersion != null && !minecraftVersion.isEmpty()) {
                params.add("game_versions=[\"" + minecraftVersion + "\"]");
            }
            
            if (modLoader != null && !modLoader.isEmpty()) {
                params.add("loaders=[\"" + modLoader.toLowerCase() + "\"]");
            }
            
            if (!params.isEmpty()) {
                urlBuilder.append("?").append(String.join("&", params));
            }
            
            Request.Builder requestBuilder = new Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", USER_AGENT);
            
            String apiKey = SecureConfig.getInstance().getModrinthApiKey();
            if (apiKey != null && !apiKey.isEmpty()) {
                requestBuilder.addHeader("Authorization", apiKey);
            }
            
            Request request = requestBuilder.build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonArray versionsArray = gson.fromJson(responseBody, JsonArray.class);
                    
                    for (JsonElement element : versionsArray) {
                        JsonObject versionJson = element.getAsJsonObject();
                        if (versionJson.has("files") && versionJson.getAsJsonArray("files").size() > 0) {
                            JsonObject file = versionJson.getAsJsonArray("files").get(0).getAsJsonObject();
                            versions.add(file.get("url").getAsString());
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error getting project versions from Modrinth", e);
        }
        
        return versions;
    }
    
    /**
     * Get download URL for a mod version
     */
    public String getModDownloadUrl(String projectId, String versionId) {
        if (!isAvailable()) {
            return null;
        }
        
        try {
            String url = API_BASE_URL + "/version/" + versionId;
            
            Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", USER_AGENT);
            
            String apiKey = SecureConfig.getInstance().getModrinthApiKey();
            if (apiKey != null && !apiKey.isEmpty()) {
                requestBuilder.addHeader("Authorization", apiKey);
            }
            
            Request request = requestBuilder.build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject versionJson = gson.fromJson(responseBody, JsonObject.class);
                    
                    if (versionJson.has("files") && versionJson.getAsJsonArray("files").size() > 0) {
                        JsonObject file = versionJson.getAsJsonArray("files").get(0).getAsJsonObject();
                        return file.get("url").getAsString();
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error getting mod download URL from Modrinth", e);
        }
        
        return null;
    }
    
    private ModInfo parseModFromJson(JsonObject modJson) {
        try {
            ModInfo mod = new ModInfo();
            mod.setName(modJson.get("title").getAsString());
            mod.setDescription(modJson.get("description").getAsString());
            mod.setProjectId(modJson.get("project_id").getAsString());
            mod.setSource(ModInfo.ModSource.MODRINTH);
            
            // Get latest version info
            if (modJson.has("latest_version")) {
                mod.setVersion(modJson.get("latest_version").getAsString());
            }
            
            // Get game versions
            if (modJson.has("versions") && modJson.getAsJsonArray("versions").size() > 0) {
                mod.setMinecraftVersion(modJson.getAsJsonArray("versions").get(0).getAsString());
            }
            
            // Get categories for mod loader info
            if (modJson.has("categories") && modJson.getAsJsonArray("categories").size() > 0) {
                JsonArray categories = modJson.getAsJsonArray("categories");
                for (JsonElement category : categories) {
                    String cat = category.getAsString();
                    if (cat.equals("forge") || cat.equals("fabric") || cat.equals("quilt") || cat.equals("neoforge")) {
                        mod.setModLoaderType(cat);
                        break;
                    }
                }
            }
            
            return mod;
        } catch (Exception e) {
            logger.error("Error parsing mod from JSON", e);
            return null;
        }
    }
}
