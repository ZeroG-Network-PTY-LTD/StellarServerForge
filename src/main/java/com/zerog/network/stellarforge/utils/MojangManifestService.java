package com.zerog.network.stellarforge.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for interacting with Mojang's version manifest API
 * Fetches Minecraft versions and server download URLs
 */
public class MojangManifestService {
    private static final Logger logger = LoggerFactory.getLogger(MojangManifestService.class);
    
    private static final String VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String USER_AGENT = "StellarServerForge/1.0.0 (ZeroG Network)";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    // Cache for version manifest
    private JsonObject cachedManifest;
    private long cacheTime = 0;
    private static final long CACHE_DURATION = 3600000; // 1 hour
    
    public MojangManifestService() {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * Minecraft version information
     */
    public static class MinecraftVersion {
        private final String id;
        private final String type;
        private final String url;
        private final String releaseTime;
        
        public MinecraftVersion(String id, String type, String url, String releaseTime) {
            this.id = id;
            this.type = type;
            this.url = url;
            this.releaseTime = releaseTime;
        }
        
        public String getId() { return id; }
        public String getType() { return type; }
        public String getUrl() { return url; }
        public String getReleaseTime() { return releaseTime; }
        
        public boolean isRelease() { return "release".equals(type); }
        public boolean isSnapshot() { return "snapshot".equals(type); }
        
        @Override
        public String toString() {
            return id + (isRelease() ? "" : " (" + type + ")");
        }
    }
    
    /**
     * Server download information
     */
    public static class ServerDownload {
        private final String version;
        private final String url;
        private final String sha1;
        private final long size;
        
        public ServerDownload(String version, String url, String sha1, long size) {
            this.version = version;
            this.url = url;
            this.sha1 = sha1;
            this.size = size;
        }
        
        public String getVersion() { return version; }
        public String getUrl() { return url; }
        public String getSha1() { return sha1; }
        public long getSize() { return size; }
    }
    
    /**
     * Fetch the version manifest from Mojang
     */
    private JsonObject fetchManifest() throws IOException {
        // Check cache
        long now = System.currentTimeMillis();
        if (cachedManifest != null && (now - cacheTime) < CACHE_DURATION) {
            logger.debug("Using cached version manifest");
            return cachedManifest;
        }
        
        logger.info("Fetching Minecraft version manifest from Mojang");
        
        Request request = new Request.Builder()
                .url(VERSION_MANIFEST_URL)
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch version manifest: " + response.code());
            }
            
            if (response.body() == null) {
                throw new IOException("Empty response from Mojang API");
            }
            
            String body = response.body().string();
            cachedManifest = gson.fromJson(body, JsonObject.class);
            cacheTime = now;
            
            logger.info("Successfully fetched version manifest");
            return cachedManifest;
        }
    }
    
    /**
     * Get all available Minecraft versions
     */
    public List<MinecraftVersion> getAllVersions() throws IOException {
        JsonObject manifest = fetchManifest();
        List<MinecraftVersion> versions = new ArrayList<>();
        
        if (manifest.has("versions")) {
            JsonArray versionsArray = manifest.getAsJsonArray("versions");
            
            for (int i = 0; i < versionsArray.size(); i++) {
                JsonObject versionObj = versionsArray.get(i).getAsJsonObject();
                
                String id = versionObj.get("id").getAsString();
                String type = versionObj.get("type").getAsString();
                String url = versionObj.get("url").getAsString();
                String releaseTime = versionObj.get("releaseTime").getAsString();
                
                versions.add(new MinecraftVersion(id, type, url, releaseTime));
            }
        }
        
        logger.info("Found {} Minecraft versions", versions.size());
        return versions;
    }
    
    /**
     * Get only release versions (no snapshots)
     */
    public List<MinecraftVersion> getReleaseVersions() throws IOException {
        List<MinecraftVersion> allVersions = getAllVersions();
        List<MinecraftVersion> releases = new ArrayList<>();
        
        for (MinecraftVersion version : allVersions) {
            if (version.isRelease()) {
                releases.add(version);
            }
        }
        
        logger.info("Found {} release versions", releases.size());
        return releases;
    }
    
    /**
     * Get the latest release version
     */
    public MinecraftVersion getLatestRelease() throws IOException {
        JsonObject manifest = fetchManifest();
        
        if (manifest.has("latest")) {
            JsonObject latest = manifest.getAsJsonObject("latest");
            String latestRelease = latest.get("release").getAsString();
            
            // Find the version in the list
            List<MinecraftVersion> versions = getAllVersions();
            for (MinecraftVersion version : versions) {
                if (version.getId().equals(latestRelease)) {
                    logger.info("Latest release: {}", latestRelease);
                    return version;
                }
            }
        }
        
        throw new IOException("Could not determine latest release");
    }
    
    /**
     * Get the latest snapshot version
     */
    public MinecraftVersion getLatestSnapshot() throws IOException {
        JsonObject manifest = fetchManifest();
        
        if (manifest.has("latest")) {
            JsonObject latest = manifest.getAsJsonObject("latest");
            String latestSnapshot = latest.get("snapshot").getAsString();
            
            // Find the version in the list
            List<MinecraftVersion> versions = getAllVersions();
            for (MinecraftVersion version : versions) {
                if (version.getId().equals(latestSnapshot)) {
                    logger.info("Latest snapshot: {}", latestSnapshot);
                    return version;
                }
            }
        }
        
        throw new IOException("Could not determine latest snapshot");
    }
    
    /**
     * Find a specific version by ID
     */
    public MinecraftVersion findVersion(String versionId) throws IOException {
        List<MinecraftVersion> versions = getAllVersions();
        
        for (MinecraftVersion version : versions) {
            if (version.getId().equals(versionId)) {
                logger.info("Found version: {}", versionId);
                return version;
            }
        }
        
        logger.warn("Version not found: {}", versionId);
        return null;
    }
    
    /**
     * Get server download information for a specific version
     */
    public ServerDownload getServerDownload(String versionId) throws IOException {
        MinecraftVersion version = findVersion(versionId);
        
        if (version == null) {
            throw new IOException("Version not found: " + versionId);
        }
        
        return getServerDownload(version);
    }
    
    /**
     * Get server download information for a version
     */
    public ServerDownload getServerDownload(MinecraftVersion version) throws IOException {
        logger.info("Fetching server download info for {}", version.getId());
        
        // Fetch the version-specific manifest
        Request request = new Request.Builder()
                .url(version.getUrl())
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch version details: " + response.code());
            }
            
            if (response.body() == null) {
                throw new IOException("Empty response for version details");
            }
            
            String body = response.body().string();
            JsonObject versionData = gson.fromJson(body, JsonObject.class);
            
            // Check if server download exists
            if (!versionData.has("downloads")) {
                throw new IOException("No downloads available for version " + version.getId());
            }
            
            JsonObject downloads = versionData.getAsJsonObject("downloads");
            
            if (!downloads.has("server")) {
                throw new IOException("Server download not available for version " + version.getId());
            }
            
            JsonObject server = downloads.getAsJsonObject("server");
            
            String url = server.get("url").getAsString();
            String sha1 = server.get("sha1").getAsString();
            long size = server.get("size").getAsLong();
            
            logger.info("Server download found - URL: {}, Size: {} bytes", url, size);
            
            return new ServerDownload(version.getId(), url, sha1, size);
        }
    }
    
    /**
     * Check if server download is available for a version
     */
    public boolean isServerAvailable(String versionId) {
        try {
            ServerDownload download = getServerDownload(versionId);
            return download != null;
        } catch (IOException e) {
            logger.debug("Server not available for {}: {}", versionId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get popular/recommended Minecraft versions
     */
    public List<String> getRecommendedVersions() throws IOException {
        List<String> recommended = new ArrayList<>();
        
        // Always include latest
        try {
            MinecraftVersion latest = getLatestRelease();
            recommended.add(latest.getId());
        } catch (IOException e) {
            logger.warn("Could not get latest version", e);
        }
        
        // Add some popular stable versions
        String[] popular = {
            "1.20.4", "1.20.2", "1.20.1",
            "1.19.4", "1.19.2",
            "1.18.2",
            "1.17.1",
            "1.16.5",
            "1.12.2"
        };
        
        List<MinecraftVersion> allVersions = getAllVersions();
        for (String popVer : popular) {
            for (MinecraftVersion version : allVersions) {
                if (version.getId().equals(popVer) && !recommended.contains(popVer)) {
                    recommended.add(popVer);
                    break;
                }
            }
        }
        
        return recommended;
    }
    
    /**
     * Validate if a version string is a valid Minecraft version
     */
    public boolean isValidVersion(String versionId) {
        try {
            MinecraftVersion version = findVersion(versionId);
            return version != null;
        } catch (IOException e) {
            logger.debug("Error validating version {}: {}", versionId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Clear the manifest cache
     */
    public void clearCache() {
        cachedManifest = null;
        cacheTime = 0;
        logger.debug("Version manifest cache cleared");
    }
}

