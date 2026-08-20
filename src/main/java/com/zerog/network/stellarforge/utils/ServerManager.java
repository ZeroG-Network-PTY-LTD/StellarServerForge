package com.zerog.network.stellarforge.utils;

import com.zerog.network.stellarforge.model.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Server manager for Stellar Server Forge
 * Handles Minecraft server installation and configuration
 */
public class ServerManager {
    private static final Logger logger = LoggerFactory.getLogger(ServerManager.class);
    
    private static final String MOJANG_VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String USER_AGENT = "StellarServerForge/1.0.0 (ZeroG Network)";
    
    private final ServerConfig config;
    private final Path serverPath;
    
    public ServerManager(ServerConfig config) {
        this.config = config;
        this.serverPath = config.getServerPath() != null && !config.getServerPath().isEmpty()
                ? Paths.get(config.getServerPath())
                : Paths.get("server");
    }
    
    /**
     * Initialize server directory structure
     */
    public boolean initializeServer() {
        try {
            // Create server directory
            if (!Files.exists(serverPath)) {
                Files.createDirectories(serverPath);
                logger.info("Created server directory: {}", serverPath.toAbsolutePath());
            }
            
            // Create subdirectories
            Files.createDirectories(serverPath.resolve("mods"));
            Files.createDirectories(serverPath.resolve("config"));
            Files.createDirectories(serverPath.resolve("world"));
            Files.createDirectories(serverPath.resolve("logs"));
            
            logger.info("Server directory structure initialized");
            return true;
        } catch (IOException e) {
            logger.error("Error initializing server directory", e);
            return false;
        }
    }
    
    /**
     * Download Minecraft server JAR
     */
    public boolean downloadMinecraftServer(ProgressCallback callback) {
        try {
            logger.info("Downloading Minecraft server version {}", config.getMinecraftVersion());
            
            if (callback != null) {
                callback.onProgress(0, "Fetching version manifest...");
            }
            
            // Use Mojang manifest service
            MojangManifestService mojangService = new MojangManifestService();
            
            if (callback != null) {
                callback.onProgress(10, "Looking up version information...");
            }
            
            // Get server download info
            MojangManifestService.ServerDownload serverDownload = 
                    mojangService.getServerDownload(config.getMinecraftVersion());
            
            if (serverDownload == null) {
                logger.error("Server not available for version {}", config.getMinecraftVersion());
                if (callback != null) {
                    callback.onProgress(0, "Server not available for this version");
                }
                return false;
            }
            
            if (callback != null) {
                callback.onProgress(20, "Downloading server.jar (" + formatFileSize(serverDownload.getSize()) + ")...");
            }
            
            Path serverJar = serverPath.resolve("server.jar");
            downloadFile(serverDownload.getUrl(), serverJar, callback);
            
            // Verify SHA1 checksum
            if (callback != null) {
                callback.onProgress(95, "Verifying download...");
            }
            
            String actualSha1 = calculateSha1(serverJar);
            if (!actualSha1.equalsIgnoreCase(serverDownload.getSha1())) {
                logger.error("SHA1 checksum mismatch! Expected: {}, Got: {}", 
                        serverDownload.getSha1(), actualSha1);
                Files.deleteIfExists(serverJar);
                if (callback != null) {
                    callback.onProgress(0, "Download verification failed");
                }
                return false;
            }
            
            if (callback != null) {
                callback.onProgress(100, "Download complete and verified");
            }
            
            logger.info("Minecraft server {} downloaded and verified successfully", config.getMinecraftVersion());
            return true;
            
        } catch (Exception e) {
            logger.error("Error downloading Minecraft server", e);
            if (callback != null) {
                callback.onProgress(0, "Error: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Accept Minecraft EULA
     */
    public boolean acceptEula() {
        try {
            Path eulaFile = serverPath.resolve("eula.txt");
            
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(eulaFile))) {
                writer.println("# Automatically accepted by Stellar Server Forge");
                writer.println("# " + java.time.LocalDateTime.now());
                writer.println("eula=true");
            }
            
            logger.info("EULA accepted and saved to {}", eulaFile);
            return true;
        } catch (IOException e) {
            logger.error("Error accepting EULA", e);
            return false;
        }
    }
    
    /**
     * Generate server.properties file
     */
    public boolean generateServerProperties() {
        try {
            Path propertiesFile = serverPath.resolve("server.properties");
            Properties props = new Properties();
            
            // Basic properties
            props.setProperty("server-name", config.getServerName());
            props.setProperty("server-port", String.valueOf(config.getPort()));
            props.setProperty("max-players", "20");
            props.setProperty("level-name", "world");
            props.setProperty("gamemode", "survival");
            props.setProperty("difficulty", "normal");
            props.setProperty("enable-command-block", "true");
            props.setProperty("spawn-protection", "16");
            props.setProperty("max-world-size", "29999984");
            props.setProperty("motd", "A ZeroG Network Server - Powered by Stellar Server Forge");
            props.setProperty("online-mode", "true");
            props.setProperty("pvp", "true");
            props.setProperty("allow-flight", "false");
            props.setProperty("enable-rcon", "false");
            
            // Save properties
            try (OutputStream out = Files.newOutputStream(propertiesFile)) {
                props.store(out, "Generated by Stellar Server Forge");
            }
            
            logger.info("Server properties generated: {}", propertiesFile);
            return true;
        } catch (IOException e) {
            logger.error("Error generating server properties", e);
            return false;
        }
    }
    
    /**
     * Create server startup script
     */
    public boolean createStartScript() {
        try {
            Path startScript = serverPath.resolve("start.bat");
            
            // Calculate RAM parameters
            int minRam = Math.max(1, config.getMaxRamGb() / 2);
            int maxRam = config.getMaxRamGb();
            
            StringBuilder script = new StringBuilder();
            script.append("@echo off\n");
            script.append("title ").append(config.getServerName()).append("\n");
            script.append("echo ========================================\n");
            script.append("echo  ").append(config.getServerName()).append("\n");
            script.append("echo  Minecraft ").append(config.getMinecraftVersion()).append("\n");
            script.append("echo  Mod Loader: ").append(config.getModLoader().getDisplayName()).append("\n");
            script.append("echo ========================================\n");
            script.append("echo.\n");
            script.append("echo Starting server...\n");
            script.append("echo.\n\n");
            
            // Java command
            String javaPath = config.getCustomJavaPath() != null && !config.getCustomJavaPath().isEmpty()
                    ? config.getCustomJavaPath()
                    : "java";
            
            script.append(javaPath).append(" ");
            script.append("-Xms").append(minRam).append("G ");
            script.append("-Xmx").append(maxRam).append("G ");
            
            // Add JVM arguments
            if (config.getJvmArgs() != null && !config.getJvmArgs().isEmpty()) {
                script.append(config.getJvmArgs()).append(" ");
            }
            
            script.append("-jar server.jar nogui\n\n");
            
            // Auto-restart if enabled
            if (config.isAutoRestart()) {
                script.append("echo.\n");
                script.append("echo Server closed. Restarting in 5 seconds...\n");
                script.append("timeout /t 5 /nobreak >nul\n");
                script.append("goto :start\n");
            } else {
                script.append("pause\n");
            }
            
            // Write script
            Files.writeString(startScript, script.toString());
            
            logger.info("Start script created: {}", startScript);
            return true;
        } catch (IOException e) {
            logger.error("Error creating start script", e);
            return false;
        }
    }
    
    /**
     * Install mod loader (placeholder)
     */
    public boolean installModLoader(ProgressCallback callback) {
        try {
            logger.info("Installing {} mod loader", config.getModLoader().getDisplayName());
            
            if (callback != null) {
                callback.onProgress(0, "Preparing mod loader installation...");
            }
            
            // This is a placeholder
            // Full implementation would download and run the installer for each mod loader
            switch (config.getModLoader()) {
                case FORGE:
                    return installForge(callback);
                case FABRIC:
                    return installFabric(callback);
                case QUILT:
                    return installQuilt(callback);
                case NEOFORGE:
                    return installNeoForge(callback);
                default:
                    logger.warn("Unknown mod loader: {}", config.getModLoader());
                    return false;
            }
        } catch (Exception e) {
            logger.error("Error installing mod loader", e);
            return false;
        }
    }
    
    private boolean installForge(ProgressCallback callback) {
        try {
            logger.info("Installing Forge for Minecraft {}", config.getMinecraftVersion());
            
            if (callback != null) {
                callback.onProgress(0, "Preparing Forge installation...");
            }
            
            ForgeInstaller forgeInstaller = new ForgeInstaller(serverPath);
            
            // Check if already installed
            if (forgeInstaller.isForgeInstalled()) {
                logger.info("Forge is already installed");
                if (callback != null) {
                    callback.onProgress(100, "Forge is already installed");
                }
                return true;
            }
            
            // Quick install with auto-detection
            boolean success = forgeInstaller.quickInstall(config.getMinecraftVersion(), callback);
            
            if (success) {
                logger.info("Forge installation completed successfully");
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error installing Forge", e);
            if (callback != null) {
                callback.onProgress(0, "Forge installation failed: " + e.getMessage());
            }
            return false;
        }
    }
    
    private boolean installFabric(ProgressCallback callback) {
        try {
            logger.info("Installing Fabric for Minecraft {}", config.getMinecraftVersion());
            
            if (callback != null) {
                callback.onProgress(0, "Preparing Fabric installation...");
            }
            
            FabricInstaller fabricInstaller = new FabricInstaller(serverPath);
            
            // Check if already installed
            if (fabricInstaller.isFabricInstalled()) {
                logger.info("Fabric is already installed");
                if (callback != null) {
                    callback.onProgress(100, "Fabric is already installed");
                }
                return true;
            }
            
            // Quick install with auto-detection
            boolean success = fabricInstaller.quickInstall(config.getMinecraftVersion(), callback);
            
            if (success) {
                logger.info("Fabric installation completed successfully");
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error installing Fabric", e);
            if (callback != null) {
                callback.onProgress(0, "Fabric installation failed: " + e.getMessage());
            }
            return false;
        }
    }
    
    private boolean installQuilt(ProgressCallback callback) {
        try {
            logger.info("Installing Quilt for Minecraft {}", config.getMinecraftVersion());
            
            if (callback != null) {
                callback.onProgress(0, "Preparing Quilt installation...");
            }
            
            QuiltInstaller quiltInstaller = new QuiltInstaller(serverPath);
            
            // Check if already installed
            if (quiltInstaller.isQuiltInstalled()) {
                logger.info("Quilt is already installed");
                if (callback != null) {
                    callback.onProgress(100, "Quilt is already installed");
                }
                return true;
            }
            
            // Quick install with auto-detection
            boolean success = quiltInstaller.quickInstall(config.getMinecraftVersion(), callback);
            
            if (success) {
                logger.info("Quilt installation completed successfully");
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error installing Quilt", e);
            if (callback != null) {
                callback.onProgress(0, "Quilt installation failed: " + e.getMessage());
            }
            return false;
        }
    }
    
    private boolean installNeoForge(ProgressCallback callback) {
        try {
            logger.info("Installing NeoForge for Minecraft {}", config.getMinecraftVersion());
            
            if (callback != null) {
                callback.onProgress(0, "Preparing NeoForge installation...");
            }
            
            NeoForgeInstaller neoforgeInstaller = new NeoForgeInstaller(serverPath);
            
            // Check if already installed
            if (neoforgeInstaller.isNeoForgeInstalled()) {
                logger.info("NeoForge is already installed");
                if (callback != null) {
                    callback.onProgress(100, "NeoForge is already installed");
                }
                return true;
            }
            
            // Quick install with auto-detection
            boolean success = neoforgeInstaller.quickInstall(config.getMinecraftVersion(), callback);
            
            if (success) {
                logger.info("NeoForge installation completed successfully");
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error installing NeoForge", e);
            if (callback != null) {
                callback.onProgress(0, "NeoForge installation failed: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Check if server is already installed
     */
    public boolean isServerInstalled() {
        Path serverJar = serverPath.resolve("server.jar");
        return Files.exists(serverJar);
    }
    
    /**
     * Download a file with progress tracking
     */
    private void downloadFile(String urlString, Path targetFile, ProgressCallback callback) throws IOException {
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        
        long fileSize = connection.getContentLengthLong();
        long downloaded = 0;
        
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(targetFile.toFile())) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            int lastProgress = -1;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                downloaded += bytesRead;
                
                if (callback != null && fileSize > 0) {
                    int progress = 20 + (int) ((downloaded * 75) / fileSize); // 20-95% range
                    if (progress != lastProgress) {
                        callback.onProgress(progress, 
                                String.format("Downloaded %s / %s", 
                                        formatFileSize(downloaded), 
                                        formatFileSize(fileSize)));
                        lastProgress = progress;
                    }
                }
            }
        }
        
        logger.info("Downloaded file: {}", targetFile.getFileName());
    }
    
    /**
     * Calculate SHA1 checksum of a file
     */
    private String calculateSha1(Path file) throws IOException {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
            
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 algorithm not available", e);
        }
    }
    
    /**
     * Format file size for display
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }
    
    /**
     * Get server path
     */
    public Path getServerPath() {
        return serverPath;
    }
    
    /**
     * Progress callback interface
     */
    public interface ProgressCallback {
        void onProgress(int percent, String message);
    }
}

