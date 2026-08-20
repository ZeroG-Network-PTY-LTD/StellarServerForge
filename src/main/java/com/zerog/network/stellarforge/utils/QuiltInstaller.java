package com.zerog.network.stellarforge.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Quilt Mod Loader installer for Stellar Server Forge
 * Downloads and installs Quilt for Minecraft servers
 * Quilt is a fork of Fabric with additional features
 */
public class QuiltInstaller {
    private static final Logger logger = LoggerFactory.getLogger(QuiltInstaller.class);
    
    private static final String QUILT_META_URL = "https://meta.quiltmc.org/v3";
    private static final String QUILT_VERSIONS_URL = "https://meta.quiltmc.org/v3/versions";
    private static final String QUILT_INSTALLER_URL = "https://meta.quiltmc.org/v3/versions/installer";
    private static final String QUILT_SERVER_DOWNLOAD = "https://meta.quiltmc.org/v3/versions/loader/%s/%s/%s/server/jar";
    private static final String USER_AGENT = "StellarServerForge/1.0.0 (ZeroG Network)";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Path serverPath;
    
    public QuiltInstaller(Path serverPath) {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.serverPath = serverPath;
    }
    
    /**
     * Quilt version information
     */
    public static class QuiltVersion {
        private final String mcVersion;
        private final String loaderVersion;
        private final String installerVersion;
        
        public QuiltVersion(String mcVersion, String loaderVersion, String installerVersion) {
            this.mcVersion = mcVersion;
            this.loaderVersion = loaderVersion;
            this.installerVersion = installerVersion;
        }
        
        public String getMcVersion() { return mcVersion; }
        public String getLoaderVersion() { return loaderVersion; }
        public String getInstallerVersion() { return installerVersion; }
        
        @Override
        public String toString() {
            return "Quilt Loader " + loaderVersion + " for MC " + mcVersion;
        }
    }
    
    /**
     * Get latest Quilt loader version
     */
    public String getLatestLoaderVersion() throws IOException {
        logger.info("Fetching latest Quilt loader version");
        
        Request request = new Request.Builder()
                .url(QUILT_VERSIONS_URL + "/loader")
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch Quilt loader versions: " + response.code());
            }
            
            if (response.body() == null) {
                throw new IOException("Empty response from Quilt API");
            }
            
            String body = response.body().string();
            JsonArray versions = gson.fromJson(body, JsonArray.class);
            
            if (versions.size() == 0) {
                throw new IOException("No Quilt loader versions found");
            }
            
            // First entry is the latest
            JsonObject latest = versions.get(0).getAsJsonObject();
            String version = latest.get("version").getAsString();
            
            logger.info("Latest Quilt loader: {}", version);
            return version;
        }
    }
    
    /**
     * Get latest Quilt installer version
     */
    public String getLatestInstallerVersion() throws IOException {
        logger.info("Fetching latest Quilt installer version");
        
        Request request = new Request.Builder()
                .url(QUILT_INSTALLER_URL)
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch Quilt installer versions: " + response.code());
            }
            
            if (response.body() == null) {
                throw new IOException("Empty response from Quilt API");
            }
            
            String body = response.body().string();
            JsonArray versions = gson.fromJson(body, JsonArray.class);
            
            if (versions.size() == 0) {
                throw new IOException("No Quilt installer versions found");
            }
            
            // First entry is the latest stable
            JsonObject latest = versions.get(0).getAsJsonObject();
            String version = latest.get("version").getAsString();
            
            logger.info("Latest Quilt installer: {}", version);
            return version;
        }
    }
    
    /**
     * Verify Minecraft version is supported by Quilt
     */
    public boolean isMcVersionSupported(String mcVersion) throws IOException {
        Request request = new Request.Builder()
                .url(QUILT_VERSIONS_URL + "/game")
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return false;
            }
            
            if (response.body() == null) {
                return false;
            }
            
            String body = response.body().string();
            JsonArray versions = gson.fromJson(body, JsonArray.class);
            
            for (int i = 0; i < versions.size(); i++) {
                JsonObject version = versions.get(i).getAsJsonObject();
                if (version.get("version").getAsString().equals(mcVersion)) {
                    return true;
                }
            }
            
            return false;
        }
    }
    
    /**
     * Get recommended Quilt version for a Minecraft version
     */
    public QuiltVersion getRecommendedVersion(String mcVersion) throws IOException {
        logger.info("Getting recommended Quilt version for Minecraft {}", mcVersion);
        
        // Check if MC version is supported
        if (!isMcVersionSupported(mcVersion)) {
            throw new IOException("Minecraft version " + mcVersion + " is not supported by Quilt");
        }
        
        String loaderVersion = getLatestLoaderVersion();
        String installerVersion = getLatestInstallerVersion();
        
        return new QuiltVersion(mcVersion, loaderVersion, installerVersion);
    }
    
    /**
     * Download Quilt server JAR directly
     * Similar to Fabric, Quilt provides a pre-built server JAR
     */
    public Path downloadQuiltServer(QuiltVersion version, ServerManager.ProgressCallback callback) throws IOException {
        String downloadUrl = String.format(QUILT_SERVER_DOWNLOAD, 
                version.getMcVersion(), 
                version.getLoaderVersion(), 
                version.getInstallerVersion());
        
        Path quiltServerJar = serverPath.resolve("quilt-server-launch.jar");
        
        logger.info("Downloading Quilt server from: {}", downloadUrl);
        
        if (callback != null) {
            callback.onProgress(10, "Downloading Quilt server...");
        }
        
        Request request = new Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download Quilt server: " + response.code());
            }
            
            if (response.body() == null) {
                throw new IOException("Empty Quilt server download");
            }
            
            long totalSize = response.body().contentLength();
            
            try (InputStream in = response.body().byteStream();
                 OutputStream out = new FileOutputStream(quiltServerJar.toFile())) {
                
                byte[] buffer = new byte[8192];
                long downloaded = 0;
                int bytesRead;
                int lastProgress = 10;
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    
                    if (callback != null && totalSize > 0) {
                        int progress = 10 + (int) ((downloaded * 80) / totalSize); // 10-90%
                        if (progress != lastProgress) {
                            callback.onProgress(progress, "Downloading Quilt server...");
                            lastProgress = progress;
                        }
                    }
                }
            }
        }
        
        logger.info("Quilt server downloaded to: {}", quiltServerJar);
        return quiltServerJar;
    }
    
    /**
     * Install Quilt for the server
     */
    public boolean installQuilt(QuiltVersion version, ServerManager.ProgressCallback callback) throws IOException {
        logger.info("Installing Quilt {} to server", version);
        
        if (callback != null) {
            callback.onProgress(5, "Preparing Quilt installation...");
        }
        
        // Download Quilt server JAR
        Path quiltServerJar = downloadQuiltServer(version, callback);
        
        if (callback != null) {
            callback.onProgress(92, "Finalizing Quilt installation...");
        }
        
        // Create server.jar symlink/copy
        Path serverJar = serverPath.resolve("server.jar");
        if (Files.exists(serverJar)) {
            // Backup existing server.jar
            Files.move(serverJar, serverPath.resolve("server-vanilla.jar"), StandardCopyOption.REPLACE_EXISTING);
        }
        
        // Copy Quilt jar to server.jar
        Files.copy(quiltServerJar, serverJar, StandardCopyOption.REPLACE_EXISTING);
        
        if (callback != null) {
            callback.onProgress(95, "Creating Quilt launcher...");
        }
        
        // Quilt needs the quilt-server-launch.jar to stay
        // It also downloads server JAR on first run (like Fabric)
        logger.info("Quilt server JAR created successfully");
        
        if (callback != null) {
            callback.onProgress(100, "Quilt installation complete!");
        }
        
        logger.info("Quilt installation complete");
        return true;
    }
    
    /**
     * Check if Quilt is already installed
     */
    public boolean isQuiltInstalled() {
        Path quiltJar = serverPath.resolve("quilt-server-launch.jar");
        return Files.exists(quiltJar);
    }
    
    /**
     * Get list of available Quilt loader versions
     */
    public List<String> getAvailableLoaderVersions() throws IOException {
        List<String> versions = new ArrayList<>();
        
        Request request = new Request.Builder()
                .url(QUILT_VERSIONS_URL + "/loader")
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch Quilt versions");
            }
            
            if (response.body() == null) {
                throw new IOException("Empty response");
            }
            
            String body = response.body().string();
            JsonArray loaders = gson.fromJson(body, JsonArray.class);
            
            for (int i = 0; i < Math.min(10, loaders.size()); i++) {
                JsonObject loader = loaders.get(i).getAsJsonObject();
                versions.add(loader.get("version").getAsString());
            }
        }
        
        return versions;
    }
    
    /**
     * Get list of supported Minecraft versions
     */
    public List<String> getSupportedMcVersions() throws IOException {
        List<String> versions = new ArrayList<>();
        
        Request request = new Request.Builder()
                .url(QUILT_VERSIONS_URL + "/game")
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch Minecraft versions");
            }
            
            if (response.body() == null) {
                throw new IOException("Empty response");
            }
            
            String body = response.body().string();
            JsonArray mcVersions = gson.fromJson(body, JsonArray.class);
            
            for (int i = 0; i < Math.min(20, mcVersions.size()); i++) {
                JsonObject version = mcVersions.get(i).getAsJsonObject();
                if (version.get("stable").getAsBoolean()) {
                    versions.add(version.get("version").getAsString());
                }
            }
        }
        
        return versions;
    }
    
    /**
     * Quick install with auto-detection
     */
    public boolean quickInstall(String mcVersion, ServerManager.ProgressCallback callback) throws IOException {
        if (callback != null) {
            callback.onProgress(0, "Detecting Quilt version...");
        }
        
        QuiltVersion version = getRecommendedVersion(mcVersion);
        
        if (callback != null) {
            callback.onProgress(5, "Found Quilt Loader " + version.getLoaderVersion());
        }
        
        return installQuilt(version, callback);
    }
}

