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
 * Forge Mod Loader installer for Stellar Server Forge
 * Downloads and installs Forge for Minecraft servers
 */
public class ForgeInstaller {
    private static final Logger logger = LoggerFactory.getLogger(ForgeInstaller.class);
    
    private static final String FORGE_MAVEN_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/";
    private static final String FORGE_PROMOTIONS_URL = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json";
    private static final String FORGE_INSTALLER_BASE = "https://maven.minecraftforge.net/net/minecraftforge/forge/%s/forge-%s-installer.jar";
    private static final String USER_AGENT = "StellarServerForge/1.0.0 (ZeroG Network)";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Path serverPath;
    
    public ForgeInstaller(Path serverPath) {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.serverPath = serverPath;
    }
    
    /**
     * Forge version information
     */
    public static class ForgeVersion {
        private final String mcVersion;
        private final String forgeVersion;
        private final String fullVersion;
        
        public ForgeVersion(String mcVersion, String forgeVersion) {
            this.mcVersion = mcVersion;
            this.forgeVersion = forgeVersion;
            this.fullVersion = mcVersion + "-" + forgeVersion;
        }
        
        public String getMcVersion() { return mcVersion; }
        public String getForgeVersion() { return forgeVersion; }
        public String getFullVersion() { return fullVersion; }
        
        @Override
        public String toString() {
            return "Forge " + forgeVersion + " for MC " + mcVersion;
        }
    }
    
    /**
     * Get recommended Forge version for a Minecraft version
     */
    public ForgeVersion getRecommendedVersion(String mcVersion) throws IOException {
        logger.info("Fetching recommended Forge version for Minecraft {}", mcVersion);
        
        Request request = new Request.Builder()
                .url(FORGE_PROMOTIONS_URL)
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch Forge promotions: " + response.code());
            }
            
            if (response.body() == null) {
                throw new IOException("Empty response from Forge API");
            }
            
            String body = response.body().string();
            JsonObject promotions = gson.fromJson(body, JsonObject.class);
            
            // Look for recommended version
            if (promotions.has("promos")) {
                JsonObject promos = promotions.getAsJsonObject("promos");
                
                // Try recommended first
                String recommendedKey = mcVersion + "-recommended";
                if (promos.has(recommendedKey)) {
                    String forgeVersion = promos.get(recommendedKey).getAsString();
                    logger.info("Found recommended Forge version: {}", forgeVersion);
                    return new ForgeVersion(mcVersion, forgeVersion);
                }
                
                // Fall back to latest
                String latestKey = mcVersion + "-latest";
                if (promos.has(latestKey)) {
                    String forgeVersion = promos.get(latestKey).getAsString();
                    logger.info("Using latest Forge version: {}", forgeVersion);
                    return new ForgeVersion(mcVersion, forgeVersion);
                }
            }
            
            throw new IOException("No Forge version found for Minecraft " + mcVersion);
        }
    }
    
    /**
     * Download Forge installer
     */
    public Path downloadInstaller(ForgeVersion version, ServerManager.ProgressCallback callback) throws IOException {
        String installerUrl = String.format(FORGE_INSTALLER_BASE, version.getFullVersion(), version.getFullVersion());
        Path installerPath = serverPath.resolve("forge-installer.jar");
        
        logger.info("Downloading Forge installer from: {}", installerUrl);
        
        if (callback != null) {
            callback.onProgress(10, "Downloading Forge installer...");
        }
        
        Request request = new Request.Builder()
                .url(installerUrl)
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download Forge installer: " + response.code());
            }
            
            if (response.body() == null) {
                throw new IOException("Empty installer download");
            }
            
            long totalSize = response.body().contentLength();
            
            try (InputStream in = response.body().byteStream();
                 OutputStream out = new FileOutputStream(installerPath.toFile())) {
                
                byte[] buffer = new byte[8192];
                long downloaded = 0;
                int bytesRead;
                int lastProgress = 10;
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    
                    if (callback != null && totalSize > 0) {
                        int progress = 10 + (int) ((downloaded * 30) / totalSize); // 10-40%
                        if (progress != lastProgress) {
                            callback.onProgress(progress, "Downloading installer...");
                            lastProgress = progress;
                        }
                    }
                }
            }
        }
        
        logger.info("Forge installer downloaded to: {}", installerPath);
        return installerPath;
    }
    
    /**
     * Install Forge using the installer
     */
    public boolean installForge(ForgeVersion version, ServerManager.ProgressCallback callback) throws IOException {
        logger.info("Installing Forge {} to server", version);
        
        // Download installer
        if (callback != null) {
            callback.onProgress(5, "Preparing Forge installation...");
        }
        
        Path installerPath = downloadInstaller(version, callback);
        
        if (callback != null) {
            callback.onProgress(45, "Running Forge installer...");
        }
        
        // Find Java
        String javaPath = findJava();
        
        // Run installer
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(serverPath.toFile());
        pb.command(javaPath, "-jar", installerPath.getFileName().toString(), "--installServer");
        pb.redirectErrorStream(true);
        
        logger.info("Executing: {} -jar {} --installServer", javaPath, installerPath.getFileName());
        
        Process process = pb.start();
        
        // Capture output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.debug("Forge installer: {}", line);
                
                // Update progress based on output
                if (callback != null) {
                    if (line.contains("Downloading")) {
                        callback.onProgress(55, "Downloading Forge libraries...");
                    } else if (line.contains("Installing")) {
                        callback.onProgress(75, "Installing Forge...");
                    } else if (line.contains("Success")) {
                        callback.onProgress(90, "Forge installed successfully");
                    }
                }
            }
        }
        
        // Wait for completion
        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Forge installation interrupted", e);
        }
        
        if (exitCode != 0) {
            logger.error("Forge installer failed with exit code: {}", exitCode);
            logger.error("Output: {}", output);
            throw new IOException("Forge installation failed. Exit code: " + exitCode);
        }
        
        // Clean up installer
        try {
            Files.deleteIfExists(installerPath);
            Files.deleteIfExists(serverPath.resolve("forge-installer.jar.log"));
        } catch (IOException e) {
            logger.warn("Could not delete installer files", e);
        }
        
        if (callback != null) {
            callback.onProgress(95, "Finalizing Forge installation...");
        }
        
        // Find the installed Forge jar
        Path forgeJar = findForgeJar(version);
        if (forgeJar == null) {
            throw new IOException("Forge installation completed but server JAR not found");
        }
        
        logger.info("Forge installed successfully: {}", forgeJar.getFileName());
        
        // Create a symlink or copy to server.jar for easy launching
        Path serverJar = serverPath.resolve("server.jar");
        if (Files.exists(serverJar)) {
            // Backup existing server.jar
            Files.move(serverJar, serverPath.resolve("server-vanilla.jar"), StandardCopyOption.REPLACE_EXISTING);
        }
        
        // Copy Forge jar to server.jar
        Files.copy(forgeJar, serverJar, StandardCopyOption.REPLACE_EXISTING);
        
        if (callback != null) {
            callback.onProgress(100, "Forge installation complete!");
        }
        
        logger.info("Forge installation complete");
        return true;
    }
    
    /**
     * Find the installed Forge JAR file
     */
    private Path findForgeJar(ForgeVersion version) throws IOException {
        // Forge creates files like: forge-1.20.1-47.2.0.jar or forge-1.20.1-47.2.0-shim.jar
        File[] files = serverPath.toFile().listFiles((dir, name) -> 
                name.startsWith("forge-") && name.endsWith(".jar") && !name.contains("installer"));
        
        if (files != null && files.length > 0) {
            // Return the first match (usually there's only one)
            return files[0].toPath();
        }
        
        // Also check for universal jar
        Path universalJar = serverPath.resolve("forge-" + version.getFullVersion() + "-universal.jar");
        if (Files.exists(universalJar)) {
            return universalJar;
        }
        
        // Check for shim jar
        Path shimJar = serverPath.resolve("forge-" + version.getFullVersion() + "-shim.jar");
        if (Files.exists(shimJar)) {
            return shimJar;
        }
        
        return null;
    }
    
    /**
     * Find Java executable
     */
    private String findJava() {
        // Try to find Java
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            Path javaExe = Path.of(javaHome, "bin", 
                    System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
            if (Files.exists(javaExe)) {
                return javaExe.toString();
            }
        }
        
        // Fall back to system java
        return "java";
    }
    
    /**
     * Check if Forge is already installed
     */
    public boolean isForgeInstalled() {
        try {
            File[] forgeJars = serverPath.toFile().listFiles((dir, name) -> 
                    name.startsWith("forge-") && name.endsWith(".jar") && !name.contains("installer"));
            return forgeJars != null && forgeJars.length > 0;
        } catch (Exception e) {
            logger.debug("Error checking Forge installation", e);
            return false;
        }
    }
    
    /**
     * Get list of available Forge versions for a Minecraft version
     */
    public List<String> getAvailableVersions(String mcVersion) throws IOException {
        List<String> versions = new ArrayList<>();
        
        // This would require parsing the Forge Maven metadata
        // For now, we'll just use the recommended version
        try {
            ForgeVersion recommended = getRecommendedVersion(mcVersion);
            versions.add(recommended.getForgeVersion());
        } catch (IOException e) {
            logger.warn("Could not fetch Forge versions", e);
        }
        
        return versions;
    }
    
    /**
     * Quick install with auto-detection
     */
    public boolean quickInstall(String mcVersion, ServerManager.ProgressCallback callback) throws IOException {
        if (callback != null) {
            callback.onProgress(0, "Detecting Forge version...");
        }
        
        ForgeVersion version = getRecommendedVersion(mcVersion);
        
        if (callback != null) {
            callback.onProgress(5, "Found Forge " + version.getForgeVersion());
        }
        
        return installForge(version, callback);
    }
}

