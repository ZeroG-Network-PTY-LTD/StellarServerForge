package com.universalator.utils;

import com.universalator.model.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for server management operations
 */
public class ServerManager {
    private static final Logger logger = LoggerFactory.getLogger(ServerManager.class);
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * Download and install server jar file
     */
    public static CompletableFuture<Boolean> installServerJar(ServerConfig config, String serverPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String downloadUrl = getServerDownloadUrl(config);
                if (downloadUrl != null) {
                    String jarPath = Paths.get(serverPath, "server.jar").toString();
                    return FileUtil.downloadFile(downloadUrl, jarPath);
                }
                return false;
            } catch (Exception e) {
                logger.error("Error installing server jar", e);
                return false;
            }
        }, executorService);
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
                return String.format("https://meta.quiltmc.org/v3/versions/loader/%s/%s/1.0.0/server/jar",
                    minecraftVersion, modLoaderVersion);
                
            case NEOFORGE:
                return String.format("https://maven.neoforged.net/net/neoforged/neoforge/%s/neoforge-%s-installer.jar",
                    modLoaderVersion, modLoaderVersion);
                
            default:
                return null;
        }
    }
    
    /**
     * Check if Java is installed and get version
     */
    public static String getJavaVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && line.contains("version")) {
                    return line.split("\"")[1];
                }
            }
        } catch (IOException e) {
            logger.error("Error checking Java version", e);
        }
        return "Unknown";
    }
    
    /**
     * Check if port is available
     */
    public static boolean isPortAvailable(int port) {
        try {
            java.net.ServerSocket socket = new java.net.ServerSocket(port);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Start server process
     */
    public static Process startServer(String serverPath, ServerConfig config) {
        try {
            String javaCommand = config.getCustomJavaPath() != null ? 
                config.getCustomJavaPath() : "java";
            
            String[] command = {
                javaCommand,
                "-Xmx" + config.getMaxRamGb() + "G",
                "-Xms1G",
                "-jar",
                "server.jar",
                "nogui"
            };
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(serverPath));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            logger.info("Server started successfully");
            return process;
            
        } catch (IOException e) {
            logger.error("Error starting server", e);
            return null;
        }
    }
    
    /**
     * Stop server process
     */
    public static void stopServer(Process serverProcess) {
        if (serverProcess != null && serverProcess.isAlive()) {
            try {
                // Send stop command
                OutputStreamWriter writer = new OutputStreamWriter(serverProcess.getOutputStream());
                writer.write("stop\n");
                writer.flush();
                
                // Wait for graceful shutdown
                if (!serverProcess.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    serverProcess.destroyForcibly();
                }
                
                logger.info("Server stopped successfully");
            } catch (Exception e) {
                logger.error("Error stopping server", e);
                serverProcess.destroyForcibly();
            }
        }
    }
    
    /**
     * Check if server is running
     */
    public static boolean isServerRunning(Process serverProcess) {
        return serverProcess != null && serverProcess.isAlive();
    }
    
    /**
     * Get available Minecraft versions
     */
    public static String[] getAvailableMinecraftVersions() {
        // Common Minecraft versions - in a real implementation, this would be fetched from the API
        return new String[]{
            "1.21.1", "1.21", "1.20.6", "1.20.5", "1.20.4", "1.20.3", "1.20.2", "1.20.1", "1.20",
            "1.19.4", "1.19.3", "1.19.2", "1.19.1", "1.19", "1.18.2", "1.18.1", "1.18",
            "1.17.1", "1.17", "1.16.5", "1.16.4", "1.16.3", "1.16.2", "1.16.1", "1.16",
            "1.15.2", "1.15.1", "1.15", "1.14.4", "1.14.3", "1.14.2", "1.14.1", "1.14",
            "1.13.2", "1.13.1", "1.13", "1.12.2", "1.12.1", "1.12"
        };
    }
    
    /**
     * Get recommended RAM for server
     */
    public static int getRecommendedRam(int playerCount) {
        if (playerCount <= 5) return 2;
        if (playerCount <= 10) return 4;
        if (playerCount <= 20) return 6;
        if (playerCount <= 50) return 8;
        return 12;
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
}
