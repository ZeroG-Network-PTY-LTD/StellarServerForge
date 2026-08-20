package com.zerog.network.stellarforge.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zerog.network.stellarforge.config.SecureConfig;
import com.zerog.network.stellarforge.model.ModInfo;
import com.zerog.network.stellarforge.utils.CacheManager;
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
        
        // ── Cache check ───────────────────────────────────────────────────────
        String cacheKey = "mr-search-" + query + "-" + minecraftVersion + "-" + modLoader + "-" + limit;
        String cached = CacheManager.getInstance().get(cacheKey);
        if (cached != null) {
            logger.debug("Modrinth search cache hit for: {}", query);
            return deserializeModList(cached);
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
                    // Cache result for 1 hour
                    CacheManager.getInstance().putSeconds(cacheKey, serializeModList(mods), 3600);
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
     * Get the latest compatible version for a project.
     * Returns null if no compatible version is found or the API is unavailable.
     *
     * @param projectId       Modrinth project ID
     * @param minecraftVersion MC version to filter by (may be null)
     * @param loaderType      Mod loader name, e.g. "fabric" (may be null)
     */
    public ProjectVersion getLatestVersion(String projectId, String minecraftVersion, String loaderType) {
        if (!isAvailable() || projectId == null || projectId.isEmpty()) return null;

        String cacheKey = "mr-latest-" + projectId + "-" + minecraftVersion + "-" + loaderType;
        String cached = CacheManager.getInstance().get(cacheKey);
        if (cached != null) {
            int sep = cached.indexOf("||");
            if (sep > 0) return new ProjectVersion(cached.substring(0, sep), cached.substring(sep + 2));
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(API_BASE_URL + "/project/" + projectId + "/version");
            List<String> params = new ArrayList<>();
            if (minecraftVersion != null && !minecraftVersion.isEmpty()) {
                params.add("game_versions=[\"" + minecraftVersion + "\"]");
            }
            if (loaderType != null && !loaderType.isEmpty()) {
                params.add("loaders=[\"" + loaderType.toLowerCase() + "\"]");
            }
            if (!params.isEmpty()) {
                urlBuilder.append("?").append(String.join("&", params));
            }

            Request.Builder rb = new Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", USER_AGENT);
            String apiKey = SecureConfig.getInstance().getModrinthApiKey();
            if (apiKey != null && !apiKey.isEmpty()) rb.addHeader("Authorization", apiKey);

            try (Response response = httpClient.newCall(rb.build()).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonArray versions = gson.fromJson(response.body().string(), JsonArray.class);
                    if (versions != null && versions.size() > 0) {
                        JsonObject latest = versions.get(0).getAsJsonObject();
                        String versionNumber = latest.has("version_number")
                                ? latest.get("version_number").getAsString() : null;
                        String downloadUrl = null;
                        if (latest.has("files") && latest.getAsJsonArray("files").size() > 0) {
                            JsonObject file = latest.getAsJsonArray("files").get(0).getAsJsonObject();
                            if (file.has("url")) downloadUrl = file.get("url").getAsString();
                        }
                        if (versionNumber != null) {
                            // Cache for 6 hours
                            String cacheVal = versionNumber + "||" + (downloadUrl != null ? downloadUrl : "");
                            CacheManager.getInstance().putSeconds(cacheKey, cacheVal, 21600);
                            return new ProjectVersion(versionNumber, downloadUrl);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error getting latest version from Modrinth for project {}", projectId, e);
        }
        return null;
    }

    /** Holds a version number + primary download URL for a Modrinth project version. */
    public static class ProjectVersion {
        public final String versionNumber;
        public final String downloadUrl;

        public ProjectVersion(String versionNumber, String downloadUrl) {
            this.versionNumber = versionNumber;
            this.downloadUrl   = downloadUrl;
        }
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

            // Slug
            if (modJson.has("slug")) mod.setSlug(modJson.get("slug").getAsString());

            // Author
            if (modJson.has("author")) mod.setAuthor(modJson.get("author").getAsString());

            // Icon
            if (modJson.has("icon_url") && !modJson.get("icon_url").isJsonNull()) {
                mod.setIconUrl(modJson.get("icon_url").getAsString());
            }

            // Downloads
            if (modJson.has("downloads")) {
                mod.setDownloadCount(modJson.get("downloads").getAsLong());
            }

            // Get latest version info
            if (modJson.has("latest_version")) {
                mod.setVersion(modJson.get("latest_version").getAsString());
            }

            // Get game versions
            if (modJson.has("versions") && modJson.getAsJsonArray("versions").size() > 0) {
                mod.setMinecraftVersion(modJson.getAsJsonArray("versions").get(0).getAsString());
            }

            // Get categories
            if (modJson.has("categories")) {
                List<String> cats = new ArrayList<>();
                for (JsonElement el : modJson.getAsJsonArray("categories")) {
                    cats.add(el.getAsString());
                }
                mod.setCategories(cats);
                // Also detect mod loader from categories
                for (String cat : cats) {
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

    // ── Cache serialization ───────────────────────────────────────────────────

    private String serializeModList(List<ModInfo> mods) {
        return gson.toJson(mods);
    }

    @SuppressWarnings("unchecked")
    private List<ModInfo> deserializeModList(String json) {
        try {
            return gson.fromJson(json,
                    new com.google.gson.reflect.TypeToken<List<ModInfo>>(){}.getType());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
