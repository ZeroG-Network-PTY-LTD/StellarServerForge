package com.zerog.network.stellarforge.util;

import com.zerog.network.stellarforge.model.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for server management operations
 */
public class ServerManager {
    private static final Logger logger = LoggerFactory.getLogger(ServerManager.class);
    
    /**
     * Get available Minecraft versions
     */
    public static String[] getAvailableMinecraftVersions() {
        return new String[] {
            "1.21.1", "1.21", "1.20.6", "1.20.5", "1.20.4", "1.20.3", "1.20.2", "1.20.1", 
            "1.19.4", "1.19.3", "1.19.2", "1.19.1", "1.19", "1.18.2", "1.18.1", "1.18", 
            "1.17.1", "1.17", "1.16.5", "1.16.4", "1.16.3", "1.16.2", "1.16.1", "1.16", 
            "1.15.2", "1.15.1", "1.15", "1.14.4", "1.13.2", "1.12.2", "1.11.2", "1.10.2", "1.8.9"
        };
    }
    
    /**
     * Check if server is already downloaded
     */
    public static boolean isServerDownloaded(ServerConfig config) {
        String serverPath = config.getServerPath();
        if (serverPath == null || serverPath.trim().isEmpty()) {
            return false;
        }
        
        File serverDir = new File(serverPath);
        if (!serverDir.exists()) {
            return false;
        }
        
        // Check for server jar file
        String serverJarName = getServerJarName(config);
        File serverJar = new File(serverDir, serverJarName);
        
        return serverJar.exists();
    }
    
    /**
     * Download server files
     */
    public static boolean downloadServer(ServerConfig config) {
        try {
            String downloadUrl = getServerDownloadUrl(config);
            if (downloadUrl == null) {
                logger.error("Unable to determine download URL for server configuration");
                return false;
            }
            
            String serverPath = config.getServerPath();
            String serverJarName = getServerJarName(config);
            String destinationPath = serverPath + File.separator + serverJarName;
            
            logger.info("Downloading server from {} to {}", downloadUrl, destinationPath);
            
            // Create directory if it doesn't exist
            FileUtil.createDirectory(serverPath);
            
            // Download server jar
            return FileUtil.downloadFile(downloadUrl, destinationPath);
            
        } catch (Exception e) {
            logger.error("Error downloading server", e);
            return false;
        }
    }
    
    /**
     * Launch server process
     */
    public static Process launchServer(ServerConfig config) {
        try {
            String serverPath = config.getServerPath();
            String serverJarName = getServerJarName(config);
            String javaPath = config.getCustomJavaPath();
            
            if (javaPath == null || javaPath.trim().isEmpty()) {
                javaPath = "java";
            }
            
            List<String> command = new ArrayList<>();
            command.add(javaPath);
            
            // Add JVM arguments
            String jvmArgs = config.getJvmArgs();
            if (jvmArgs != null && !jvmArgs.trim().isEmpty()) {
                String[] args = jvmArgs.split("\\s+");
                for (String arg : args) {
                    if (!arg.trim().isEmpty()) {
                        command.add(arg.trim());
                    }
                }
            }
            
            // Add memory settings
            command.add("-Xmx" + config.getMaxRamGb() + "G");
            command.add("-Xms1G");
            
            // Add server jar
            command.add("-jar");
            command.add(serverJarName);
            
            // Add nogui flag
            command.add("nogui");
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(serverPath));
            pb.redirectErrorStream(true);
            
            logger.info("Starting server with command: {}", String.join(" ", command));
            
            return pb.start();
            
        } catch (IOException e) {
            logger.error("Error launching server", e);
            return null;
        }
    }
    
    /**
     * Validate server configuration
     */
    public static boolean validateServerConfig(ServerConfig config) {
        if (config.getServerPath() == null || config.getServerPath().trim().isEmpty()) {
            return false;
        }
        
        if (config.getMinecraftVersion() == null || config.getMinecraftVersion().trim().isEmpty()) {
            return false;
        }
        
        if (config.getMaxRamGb() < 1 || config.getMaxRamGb() > 64) {
            return false;
        }
        
        if (config.getPort() < 1024 || config.getPort() > 65535) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get server download URL based on configuration
     */
    private static String getServerDownloadUrl(ServerConfig config) {
        String minecraftVersion = config.getMinecraftVersion();
        ServerConfig.ModLoader modLoader = config.getModLoader();
        String modLoaderVersion = config.getModLoaderVersion();
        
        switch (modLoader) {
            case FORGE:
                return String.format("https://maven.minecraftforge.net/net/minecraftforge/forge/%s-%s/forge-%s-%s-installer.jar",
                    minecraftVersion, modLoaderVersion, minecraftVersion, modLoaderVersion);
                
            case FABRIC:
                return String.format("https://meta.fabricmc.net/v2/versions/loader/%s/%s/1.0.0/server/jar",
                    minecraftVersion, modLoaderVersion);
                
            case QUILT:
                return String.format("https://meta.quiltmc.org/v3/versions/loader/%s/%s/0.19.2/server/jar",
                    minecraftVersion, modLoaderVersion);
                
            case NEOFORGE:
                return String.format("https://maven.neoforged.net/releases/net/neoforged/neoforge/%s/neoforge-%s-installer.jar",
                    modLoaderVersion, modLoaderVersion);
                
            default:
                return null;
        }
    }
    
    /**
     * Get server jar name based on configuration
     */
    private static String getServerJarName(ServerConfig config) {
        String minecraftVersion = config.getMinecraftVersion();
        ServerConfig.ModLoader modLoader = config.getModLoader();
        String modLoaderVersion = config.getModLoaderVersion();
        
        switch (modLoader) {
            case FORGE:
                return String.format("forge-%s-%s-installer.jar", minecraftVersion, modLoaderVersion);
            case FABRIC:
                return String.format("fabric-server-%s-%s.jar", minecraftVersion, modLoaderVersion);
            case QUILT:
                return String.format("quilt-server-%s-%s.jar", minecraftVersion, modLoaderVersion);
            case NEOFORGE:
                return String.format("neoforge-%s-installer.jar", modLoaderVersion);
            default:
                return "server.jar";
        }
    }
}
