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

        // ── Cache check ───────────────────────────────────────────────────────
        String cacheKey = "cf-search-" + query + "-" + minecraftVersion + "-" + modLoader + "-" + limit;
        String cached = CacheManager.getInstance().get(cacheKey);
        if (cached != null) {
            logger.debug("CurseForge search cache hit for: {}", query);
            return deserializeModList(cached);
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
                    // Cache result for 1 hour
                    CacheManager.getInstance().putSeconds(cacheKey, serializeModList(mods), 3600);
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
     * Get the latest compatible file for a CurseForge mod project.
     * Returns null if no compatible file is found or the API is unavailable.
     *
     * @param modId           CurseForge mod ID
     * @param minecraftVersion MC version to filter by (may be null)
     * @param loaderType      Mod loader name, e.g. "forge" (may be null)
     */
    public ProjectVersion getLatestVersion(String modId, String minecraftVersion, String loaderType) {
        if (!isAvailable() || modId == null || modId.isEmpty()) return null;

        String cacheKey = "cf-latest-" + modId + "-" + minecraftVersion + "-" + loaderType;
        String cached = CacheManager.getInstance().get(cacheKey);
        if (cached != null) {
            // Cached as "fileId||displayName||downloadUrl" (downloadUrl may be empty)
            String[] parts = cached.split("\\|\\|", 3);
            if (parts.length == 3) {
                return new ProjectVersion(parts[0], parts[1],
                        parts[2].isEmpty() ? null : parts[2]);
            }
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(
                    API_BASE_URL + "/mods/" + modId + "/files?pageSize=1&sortField=1&sortOrder=desc")
                    .append("&gameId=").append(MINECRAFT_GAME_ID);

            if (minecraftVersion != null && !minecraftVersion.isEmpty()) {
                urlBuilder.append("&gameVersion=")
                          .append(URLEncoder.encode(minecraftVersion, StandardCharsets.UTF_8));
            }
            if (loaderType != null && !loaderType.isEmpty()) {
                urlBuilder.append("&modLoaderType=").append(getModLoaderTypeId(loaderType));
            }

            Request request = new Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("Accept", "application/json")
                .addHeader("x-api-key", apiKey)
                .addHeader("User-Agent", USER_AGENT)
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
                    if (json.has("data") && json.getAsJsonArray("data").size() > 0) {
                        JsonObject file = json.getAsJsonArray("data").get(0).getAsJsonObject();
                        String fileId      = file.has("id")          ? file.get("id").getAsString()          : null;
                        String displayName = file.has("displayName") ? file.get("displayName").getAsString() : null;
                        String dlUrl       = (file.has("downloadUrl") && !file.get("downloadUrl").isJsonNull())
                                             ? file.get("downloadUrl").getAsString() : null;

                        if (fileId != null) {
                            // Cache for 6 hours
                            String cacheVal = fileId + "||" + (displayName != null ? displayName : "")
                                            + "||" + (dlUrl != null ? dlUrl : "");
                            CacheManager.getInstance().putSeconds(cacheKey, cacheVal, 21600);
                            return new ProjectVersion(fileId, displayName, dlUrl);
                        }
                    }
                } else {
                    logger.warn("CurseForge latest-version API returned status {} for mod {}",
                            response.code(), modId);
                }
            }
        } catch (IOException e) {
            logger.error("Error getting latest version from CurseForge for mod {}", modId, e);
        }
        return null;
    }

    /** Holds the latest file ID, display name, and download URL for a CurseForge mod. */
    public static class ProjectVersion {
        public final String fileId;
        public final String displayName;
        public final String downloadUrl;

        public ProjectVersion(String fileId, String displayName, String downloadUrl) {
            this.fileId      = fileId;
            this.displayName = displayName;
            this.downloadUrl = downloadUrl;
        }
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
    
    private ModInfo parseModFromJson(JsonObject modJson) {
        try {
            ModInfo mod = new ModInfo();
            mod.setName(modJson.get("name").getAsString());
            mod.setDescription(modJson.get("summary").getAsString());
            mod.setProjectId(modJson.get("id").getAsString());
            mod.setSource(ModInfo.ModSource.CURSEFORGE);

            // Author
            if (modJson.has("authors") && modJson.getAsJsonArray("authors").size() > 0) {
                mod.setAuthor(modJson.getAsJsonArray("authors").get(0)
                        .getAsJsonObject().get("name").getAsString());
            }

            // Download count
            if (modJson.has("downloadCount")) {
                mod.setDownloadCount(modJson.get("downloadCount").getAsLong());
            }

            // Icon / thumbnail
            if (modJson.has("logo") && !modJson.get("logo").isJsonNull()) {
                JsonObject logo = modJson.getAsJsonObject("logo");
                if (logo.has("thumbnailUrl")) {
                    mod.setIconUrl(logo.get("thumbnailUrl").getAsString());
                }
            }

            // Categories
            if (modJson.has("categories")) {
                List<String> cats = new java.util.ArrayList<>();
                for (com.google.gson.JsonElement el : modJson.getAsJsonArray("categories")) {
                    if (el.getAsJsonObject().has("name"))
                        cats.add(el.getAsJsonObject().get("name").getAsString());
                }
                mod.setCategories(cats);
            }

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

                if (latestFile.has("downloadUrl") && !latestFile.get("downloadUrl").isJsonNull()) {
                    mod.setUrl(latestFile.get("downloadUrl").getAsString());
                }

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
            return new java.util.ArrayList<>();
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
