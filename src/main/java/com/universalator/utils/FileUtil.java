package com.universalator.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.universalator.model.ServerConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Utility class for file operations
 */
public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Save server configuration to file
     */
    public static void saveServerConfig(ServerConfig config, String filePath) {
        try {
            String jsonString = gson.toJson(config);
            Files.write(Paths.get(filePath), jsonString.getBytes());
            logger.info("Server configuration saved to: {}", filePath);
        } catch (IOException e) {
            logger.error("Error saving server configuration", e);
        }
    }
    
    /**
     * Load server configuration from file
     */
    public static ServerConfig loadServerConfig(String filePath) {
        try {
            if (Files.exists(Paths.get(filePath))) {
                String jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
                return gson.fromJson(jsonString, ServerConfig.class);
            }
        } catch (IOException e) {
            logger.error("Error loading server configuration", e);
        }
        return new ServerConfig();
    }
    
    /**
     * Create server directory structure
     */
    public static boolean createServerDirectories(String serverPath) {
        try {
            Path serverDir = Paths.get(serverPath);
            Files.createDirectories(serverDir);
            Files.createDirectories(serverDir.resolve("mods"));
            Files.createDirectories(serverDir.resolve("config"));
            Files.createDirectories(serverDir.resolve("world"));
            Files.createDirectories(serverDir.resolve("logs"));
            return true;
        } catch (IOException e) {
            logger.error("Error creating server directories", e);
            return false;
        }
    }
    
    /**
     * Download file from URL
     */
    public static boolean downloadFile(String url, String destinationPath) {
        try {
            FileUtils.copyURLToFile(new java.net.URL(url), new File(destinationPath));
            logger.info("Downloaded file from {} to {}", url, destinationPath);
            return true;
        } catch (IOException e) {
            logger.error("Error downloading file from {} to {}", url, destinationPath, e);
            return false;
        }
    }
    
    /**
     * Create server.properties file
     */
    public static void createServerProperties(String serverPath, ServerConfig config) {
        try {
            Properties props = new Properties();
            props.setProperty("server-port", String.valueOf(config.getPort()));
            props.setProperty("max-players", "20");
            props.setProperty("level-name", "world");
            props.setProperty("gamemode", "survival");
            props.setProperty("difficulty", "easy");
            props.setProperty("spawn-protection", "16");
            props.setProperty("white-list", "false");
            props.setProperty("enable-rcon", "false");
            props.setProperty("motd", config.getServerName());
            props.setProperty("online-mode", "true");
            props.setProperty("allow-nether", "true");
            props.setProperty("allow-flight", "false");
            props.setProperty("resource-pack", "");
            props.setProperty("pvp", "true");
            props.setProperty("snooper-enabled", "false");
            props.setProperty("view-distance", "10");
            
            File propsFile = new File(serverPath, "server.properties");
            try (FileOutputStream fos = new FileOutputStream(propsFile)) {
                props.store(fos, "Minecraft server properties");
            }
            
            logger.info("Created server.properties file");
        } catch (IOException e) {
            logger.error("Error creating server.properties file", e);
        }
    }
    
    /**
     * Create EULA file
     */
    public static void createEulaFile(String serverPath) {
        try {
            Properties props = new Properties();
            props.setProperty("eula", "true");
            
            File eulaFile = new File(serverPath, "eula.txt");
            try (FileOutputStream fos = new FileOutputStream(eulaFile)) {
                props.store(fos, "By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).");
            }
            
            logger.info("Created eula.txt file");
        } catch (IOException e) {
            logger.error("Error creating eula.txt file", e);
        }
    }
    
    /**
     * Create start script
     */
    public static void createStartScript(String serverPath, ServerConfig config) {
        try {
            StringBuilder scriptContent = new StringBuilder();
            scriptContent.append("@echo off\n");
            scriptContent.append("title ").append(config.getServerName()).append("\n");
            scriptContent.append("echo Starting ").append(config.getServerName()).append("...\n");
            scriptContent.append("echo.\n");
            
            // Java command
            String javaCommand = config.getCustomJavaPath() != null ? 
                "\"" + config.getCustomJavaPath() + "\"" : "java";
            
            scriptContent.append(javaCommand);
            scriptContent.append(" -Xmx").append(config.getMaxRamGb()).append("G");
            scriptContent.append(" -Xms1G");
            scriptContent.append(" ").append(config.getJvmArgs());
            scriptContent.append(" -jar server.jar nogui\n");
            scriptContent.append("pause\n");
            
            File scriptFile = new File(serverPath, "start.bat");
            Files.write(scriptFile.toPath(), scriptContent.toString().getBytes());
            
            logger.info("Created start.bat script");
        } catch (IOException e) {
            logger.error("Error creating start script", e);
        }
    }
    
    /**
     * Check if directory exists and is writable
     */
    public static boolean isDirectoryWritable(String path) {
        try {
            Path dirPath = Paths.get(path);
            return Files.exists(dirPath) && Files.isDirectory(dirPath) && Files.isWritable(dirPath);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get file size in human readable format
     */
    public static String getHumanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
