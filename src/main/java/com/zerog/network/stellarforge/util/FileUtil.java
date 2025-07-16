package com.zerog.network.stellarforge.util;

import com.zerog.network.stellarforge.model.ServerConfig;
import com.zerog.network.stellarforge.config.SecureConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for file operations
 */
public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    
    /**
     * Load server configuration from file
     */
    public static ServerConfig loadServerConfig(String filename) {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                logger.info("Config file {} not found, creating new configuration", filename);
                return new ServerConfig();
            }
            
            String json = new String(Files.readAllBytes(file.toPath()));
            ServerConfig config = gson.fromJson(json, ServerConfig.class);
            
            logger.info("Loaded configuration from {}", filename);
            return config;
            
        } catch (Exception e) {
            logger.error("Error loading configuration from {}: {}", filename, e.getMessage());
            return new ServerConfig();
        }
    }
    
    /**
     * Save server configuration to file
     */
    public static void saveServerConfig(ServerConfig config, String filename) {
        try {
            String json = gson.toJson(config);
            Files.write(Paths.get(filename), json.getBytes());
            logger.debug("Saved configuration to {}", filename);
            
        } catch (Exception e) {
            logger.error("Error saving configuration to {}: {}", filename, e.getMessage());
        }
    }
    
    /**
     * Download file from URL
     */
    public static boolean downloadFile(String url, String destinationPath) {
        try {
            // This is a placeholder - implement actual download logic
            logger.info("Downloading file from {} to {}", url, destinationPath);
            
            // Create parent directories if they don't exist
            Path dest = Paths.get(destinationPath);
            Files.createDirectories(dest.getParent());
            
            // For now, just create an empty file as placeholder
            Files.createFile(dest);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error downloading file from {}: {}", url, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get human readable file size
     */
    public static String getHumanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Copy file asynchronously
     */
    public static CompletableFuture<Boolean> copyFileAsync(String source, String destination) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.copy(Paths.get(source), Paths.get(destination));
                return true;
            } catch (Exception e) {
                logger.error("Error copying file from {} to {}: {}", source, destination, e.getMessage());
                return false;
            }
        }, executor);
    }
    
    /**
     * Create directory if it doesn't exist
     */
    public static boolean createDirectory(String path) {
        try {
            Path dir = Paths.get(path);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                logger.debug("Created directory: {}", path);
            }
            return true;
        } catch (Exception e) {
            logger.error("Error creating directory {}: {}", path, e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete file or directory
     */
    public static boolean delete(String path) {
        try {
            Path p = Paths.get(path);
            if (Files.exists(p)) {
                Files.delete(p);
                logger.debug("Deleted: {}", path);
            }
            return true;
        } catch (Exception e) {
            logger.error("Error deleting {}: {}", path, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if file exists
     */
    public static boolean exists(String path) {
        return Files.exists(Paths.get(path));
    }
}
