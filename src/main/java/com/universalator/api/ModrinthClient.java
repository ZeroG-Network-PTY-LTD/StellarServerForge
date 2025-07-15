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
 * Modrinth API client
 */
public class ModrinthClient {
    private static final Logger logger = LoggerFactory.getLogger(ModrinthClient.class);
    private static final String API_BASE_URL = "https://api.modrinth.com/v2";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    public ModrinthClient() {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * Search for mods on Modrinth
     */
    public List<ModInfo> searchMods(String query, String minecraftVersion, String modLoader, int limit) {
        List<ModInfo> mods = new ArrayList<>();
        
        try {
            StringBuilder urlBuilder = new StringBuilder(API_BASE_URL + "/search");
            urlBuilder.append("?query=").append(query);
            urlBuilder.append("&limit=").append(limit);
            urlBuilder.append("&facets=[[\"project_type:mod\"]]");
            
            if (minecraftVersion != null && !minecraftVersion.isEmpty()) {
                urlBuilder.append("&facets=[[\"versions:").append(minecraftVersion).append("\"]]");
            }
            
            if (modLoader != null && !modLoader.isEmpty()) {
                urlBuilder.append("&facets=[[\"categories:").append(modLoader.toLowerCase()).append("\"]]");
            }
            
            Request request = new Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "UniversalatorGUI/1.0")
                .build();
            
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
        
        // Popular utility and performance mods
        String[] popularMods = {
            "Lithium", "Phosphor", "Sodium", "Iris", "ModMenu", "Roughly Enough Items",
            "Applied Energistics 2", "Create", "Botania", "Twilight Forest"
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
     * Get project versions for a specific project
     */
    public List<String> getProjectVersions(String projectId, String minecraftVersion, String modLoader) {
        List<String> versions = new ArrayList<>();
        
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
            
            Request request = new Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "UniversalatorGUI/1.0")
                .build();
            
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
        try {
            String url = API_BASE_URL + "/version/" + versionId;
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "UniversalatorGUI/1.0")
                .build();
            
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
