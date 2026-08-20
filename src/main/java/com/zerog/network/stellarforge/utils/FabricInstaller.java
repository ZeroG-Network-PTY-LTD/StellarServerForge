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
 * Fabric Mod Loader installer for Stellar Server Forge
 * Downloads and installs Fabric for Minecraft servers
 */
public class FabricInstaller {
    private static final Logger logger = LoggerFactory.getLogger(FabricInstaller.class);
    
    private static final String FABRIC_META_URL = "https://meta.fabricmc.net/v2/versions";
    private static final String FABRIC_LOADER_URL = "https://meta.fabricmc.net/v2/versions/loader";
    private static final String FABRIC_INSTALLER_URL = "https://meta.fabricmc.net/v2/versions/installer";
    private static final String FABRIC_SERVER_DOWNLOAD = "https://meta.fabricmc.net/v2/versions/loader/%s/%s/%s/server/jar";
    private static final String USER_AGENT = "StellarServerForge/1.0.0 (ZeroG Network)";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Path serverPath;
    
    public FabricInstaller(Path serverPath) {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.serverPath = serverPath;
    }
    
    /**
     * Fabric version information
     */
    public static class FabricVersion {
        private final String mcVersion;
        private final String loaderVersion;
        private final String installerVersion;
        
        public FabricVersion(String mcVersion, String loaderVersion, String installerVersion) {
            this.mcVersion = mcVersion;
            this.loaderVersion = loaderVersion;
            this.installerVersion = installerVersion;
        }
        
        public String getMcVersion() { return mcVersion; }
        public String getLoaderVersion() { return loaderVersion; }
        public String getInstallerVersion() { return installerVersion; }
        
        @Override
        public String toString() {
            return "Fabric Loader " + loaderVersion + " for MC " + mcVersion;
        }
    }
    
    /**
     * Get latest Fabric loader version
     */
    public String getLatestLoaderVersion() throws IOException {
        logger.info("Fetching latest Fabric loader version");
        
        Request request = new Request.Builder()
                .url(FABRIC_LOADER_URL)
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch Fabric loader versions: " + response.code());
            }
            
            if (response.body() == null) {
                throw new IOException("Empty response from Fabric API");
            }
            
            String body = response.body().string();
            JsonArray versions = gson.fromJson(body, JsonArray.class);
            
            if (versions.size() == 0) {
                throw new IOException("No Fabric loader versions found");
            }
            
            // First entry is the latest
            JsonObject latest = versions.get(0).getAsJsonObject();
            String version = latest.get("version").getAsString();
            
            logger.info("Latest Fabric loader: {}", version);
            return version;
        }
    }
    
    /**
     * Get latest Fabric installer version
     */
    public String getLatestInstallerVersion() throws IOException {
        logger.info("Fetching latest Fabric installer version");
        
        Request request = new Request.Builder()
                .url(FABRIC_INSTALLER_URL)
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch Fabric installer versions: " + response.code());
            }
            
            if (response.body() == null) {
                throw new IOException("Empty response from Fabric API");
            }
            
            String body = response.body().string();
            JsonArray versions = gson.fromJson(body, JsonArray.class);
            
            if (versions.size() == 0) {
                throw new IOException("No Fabric installer versions found");
            }
            
            // First entry is the latest stable
            JsonObject latest = versions.get(0).getAsJsonObject();
            String version = latest.get("version").getAsString();
            
            logger.info("Latest Fabric installer: {}", version);
            return version;
        }
    }
    
    /**
     * Verify Minecraft version is supported by Fabric
     */
    public boolean isMcVersionSupported(String mcVersion) throws IOException {
        Request request = new Request.Builder()
                .url(FABRIC_META_URL + "/game")
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
     * Get recommended Fabric version for a Minecraft version
     */
    public FabricVersion getRecommendedVersion(String mcVersion) throws IOException {
        logger.info("Getting recommended Fabric version for Minecraft {}", mcVersion);
        
        // Check if MC version is supported
        if (!isMcVersionSupported(mcVersion)) {
            throw new IOException("Minecraft version " + mcVersion + " is not supported by Fabric");
        }
        
        String loaderVersion = getLatestLoaderVersion();
        String installerVersion = getLatestInstallerVersion();
        
        return new FabricVersion(mcVersion, loaderVersion, installerVersion);
    }
    
    /**
     * Download Fabric server JAR directly
     * Fabric provides a pre-built server JAR that includes everything
     */
    public Path downloadFabricServer(FabricVersion version, ServerManager.ProgressCallback callback) throws IOException {
        String downloadUrl = String.format(FABRIC_SERVER_DOWNLOAD, 
                version.getMcVersion(), 
                version.getLoaderVersion(), 
                version.getInstallerVersion());
        
        Path fabricServerJar = serverPath.resolve("fabric-server-launch.jar");
        
        logger.info("Downloading Fabric server from: {}", downloadUrl);
        
        if (callback != null) {
            callback.onProgress(10, "Downloading Fabric server...");
        }
        
        Request request = new Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download Fabric server: " + response.code());
            }
            
            if (response.body() == null) {
                throw new IOException("Empty Fabric server download");
            }
            
            long totalSize = response.body().contentLength();
            
            try (InputStream in = response.body().byteStream();
                 OutputStream out = new FileOutputStream(fabricServerJar.toFile())) {
                
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
                            callback.onProgress(progress, "Downloading Fabric server...");
                            lastProgress = progress;
                        }
                    }
                }
            }
        }
        
        logger.info("Fabric server downloaded to: {}", fabricServerJar);
        return fabricServerJar;
    }
    
    /**
     * Install Fabric for the server
     */
    public boolean installFabric(FabricVersion version, ServerManager.ProgressCallback callback) throws IOException {
        logger.info("Installing Fabric {} to server", version);
        
        if (callback != null) {
            callback.onProgress(5, "Preparing Fabric installation...");
        }
        
        // Download Fabric server JAR
        Path fabricServerJar = downloadFabricServer(version, callback);
        
        if (callback != null) {
            callback.onProgress(92, "Finalizing Fabric installation...");
        }
        
        // Create server.jar symlink/copy
        Path serverJar = serverPath.resolve("server.jar");
        if (Files.exists(serverJar)) {
            // Backup existing server.jar
            Files.move(serverJar, serverPath.resolve("server-vanilla.jar"), StandardCopyOption.REPLACE_EXISTING);
        }
        
        // Copy Fabric jar to server.jar
        Files.copy(fabricServerJar, serverJar, StandardCopyOption.REPLACE_EXISTING);
        
        if (callback != null) {
            callback.onProgress(95, "Creating Fabric launcher...");
        }
        
        // Fabric needs the fabric-server-launch.jar to stay
        // It also downloads server JAR on first run
        logger.info("Fabric server JAR created successfully");
        
        if (callback != null) {
            callback.onProgress(100, "Fabric installation complete!");
        }
        
        logger.info("Fabric installation complete");
        return true;
    }
    
    /**
     * Check if Fabric is already installed
     */
    public boolean isFabricInstalled() {
        Path fabricJar = serverPath.resolve("fabric-server-launch.jar");
        return Files.exists(fabricJar);
    }
    
    /**
     * Get list of available Fabric loader versions
     */
    public List<String> getAvailableLoaderVersions() throws IOException {
        List<String> versions = new ArrayList<>();
        
        Request request = new Request.Builder()
                .url(FABRIC_LOADER_URL)
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch Fabric versions");
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
                .url(FABRIC_META_URL + "/game")
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
            callback.onProgress(0, "Detecting Fabric version...");
        }
        
        FabricVersion version = getRecommendedVersion(mcVersion);
        
        if (callback != null) {
            callback.onProgress(5, "Found Fabric Loader " + version.getLoaderVersion());
        }
        
        return installFabric(version, callback);
    }
}

