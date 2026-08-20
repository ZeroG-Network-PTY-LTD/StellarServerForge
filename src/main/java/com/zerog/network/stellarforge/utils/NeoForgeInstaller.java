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
 * NeoForge Mod Loader installer for Stellar Server Forge
 * Downloads and installs NeoForge for Minecraft servers (1.20.1+)
 * NeoForge is a modern fork of Forge with improved features
 */
public class NeoForgeInstaller {
    private static final Logger logger = LoggerFactory.getLogger(NeoForgeInstaller.class);
    
    private static final String NEOFORGE_MAVEN_URL = "https://maven.neoforged.net/releases/net/neoforged/neoforge/";
    private static final String NEOFORGE_MAVEN_METADATA = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml";
    private static final String NEOFORGE_INSTALLER_BASE = "https://maven.neoforged.net/releases/net/neoforged/neoforge/%s/neoforge-%s-installer.jar";
    private static final String USER_AGENT = "StellarServerForge/1.0.0 (ZeroG Network)";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Path serverPath;
    
    public NeoForgeInstaller(Path serverPath) {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.serverPath = serverPath;
    }
    
    /**
     * NeoForge version information
     */
    public static class NeoForgeVersion {
        private final String mcVersion;
        private final String neoforgeVersion;
        private final String fullVersion;
        
        public NeoForgeVersion(String mcVersion, String neoforgeVersion) {
            this.mcVersion = mcVersion;
            this.neoforgeVersion = neoforgeVersion;
            this.fullVersion = neoforgeVersion;
        }
        
        public String getMcVersion() { return mcVersion; }
        public String getNeoForgeVersion() { return neoforgeVersion; }
        public String getFullVersion() { return fullVersion; }
        
        @Override
        public String toString() {
            return "NeoForge " + neoforgeVersion + " for MC " + mcVersion;
        }
    }
    
    /**
     * Get latest NeoForge version for a Minecraft version
     * NeoForge versioning: 1.20.1 → 20.1.x, 1.20.4 → 20.4.x, 1.21 → 21.0.x
     */
    public NeoForgeVersion getLatestVersion(String mcVersion) throws IOException {
        logger.info("Fetching latest NeoForge version for Minecraft {}", mcVersion);
        
        // NeoForge only supports MC 1.20.1+
        if (!isMcVersionSupported(mcVersion)) {
            throw new IOException("NeoForge only supports Minecraft 1.20.1 and newer. For older versions, use Forge.");
        }
        
        Request request = new Request.Builder()
                .url(NEOFORGE_MAVEN_METADATA)
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch NeoForge versions: " + response.code());
            }
            
            if (response.body() == null) {
                throw new IOException("Empty response from NeoForge Maven");
            }
            
            String body = response.body().string();
            
            // Parse Maven metadata XML to find versions matching MC version
            List<String> matchingVersions = parseNeoForgeVersionsForMC(body, mcVersion);
            
            if (matchingVersions.isEmpty()) {
                throw new IOException("No NeoForge version found for Minecraft " + mcVersion);
            }
            
            // Get the latest matching version
            String latestVersion = matchingVersions.get(0);
            logger.info("Found NeoForge version: {}", latestVersion);
            
            return new NeoForgeVersion(mcVersion, latestVersion);
        }
    }
    
    /**
     * Check if Minecraft version is supported by NeoForge
     * NeoForge started with 1.20.1
     */
    private boolean isMcVersionSupported(String mcVersion) {
        try {
            String[] parts = mcVersion.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            
            // NeoForge requires 1.20.1+
            if (major > 1) return true;
            if (major == 1 && minor > 20) return true;
            if (major == 1 && minor == 20 && patch >= 1) return true;
            
            return false;
        } catch (Exception e) {
            logger.error("Error parsing MC version: {}", mcVersion, e);
            return false;
        }
    }
    
    /**
     * Parse NeoForge versions from Maven metadata XML
     * NeoForge versioning: MC 1.20.1 → 20.1.x, MC 1.20.4 → 20.4.x, MC 1.21 → 21.0.x
     */
    private List<String> parseNeoForgeVersionsForMC(String xmlContent, String mcVersion) {
        List<String> versions = new ArrayList<>();
        
        try {
            // Determine NeoForge version prefix from MC version
            // 1.20.1 → 20.1, 1.20.4 → 20.4, 1.21 → 21.0, 1.21.1 → 21.1
            String[] parts = mcVersion.split("\\.");
            String versionPrefix;
            
            if (parts.length >= 3) {
                // e.g., 1.20.1 → 20.1
                versionPrefix = parts[1] + "." + parts[2];
            } else if (parts.length == 2) {
                // e.g., 1.21 → 21.0
                versionPrefix = parts[1] + ".0";
            } else {
                return versions;
            }
            
            // Extract versions from XML
            String[] lines = xmlContent.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith("<version>")) {
                    String version = line.trim()
                            .replace("<version>", "")
                            .replace("</version>", "")
                            .trim();
                    
                    // Check if version matches MC version pattern
                    if (version.startsWith(versionPrefix + ".")) {
                        versions.add(version);
                    }
                }
            }
            
            // Sort versions in descending order (latest first)
            versions.sort((a, b) -> compareVersions(b, a));
            
        } catch (Exception e) {
            logger.error("Error parsing NeoForge versions", e);
        }
        
        return versions;
    }
    
    /**
     * Compare version strings (e.g., "20.1.15" vs "20.1.5")
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLen = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLen; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        
        return 0;
    }
    
    /**
     * Download NeoForge installer
     */
    public Path downloadInstaller(NeoForgeVersion version, ServerManager.ProgressCallback callback) throws IOException {
        String installerUrl = String.format(NEOFORGE_INSTALLER_BASE, version.getFullVersion(), version.getFullVersion());
        Path installerPath = serverPath.resolve("neoforge-installer.jar");
        
        logger.info("Downloading NeoForge installer from: {}", installerUrl);
        
        if (callback != null) {
            callback.onProgress(10, "Downloading NeoForge installer...");
        }
        
        Request request = new Request.Builder()
                .url(installerUrl)
                .addHeader("User-Agent", USER_AGENT)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download NeoForge installer: " + response.code());
            }
            
            if (response.body() == null) {
                throw new IOException("Empty response when downloading installer");
            }
            
            // Download with progress tracking
            long contentLength = response.body().contentLength();
            long downloaded = 0;
            
            try (InputStream in = response.body().byteStream();
                 OutputStream out = Files.newOutputStream(installerPath)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    
                    if (callback != null && contentLength > 0) {
                        int progress = 10 + (int) ((downloaded * 20) / contentLength); // 10-30%
                        callback.onProgress(progress, "Downloading installer...");
                    }
                }
            }
            
            logger.info("NeoForge installer downloaded: {}", installerPath);
            return installerPath;
        }
    }
    
    /**
     * Run NeoForge installer
     */
    public boolean runInstaller(Path installerPath, ServerManager.ProgressCallback callback) {
        try {
            logger.info("Running NeoForge installer...");
            
            if (callback != null) {
                callback.onProgress(30, "Running NeoForge installer...");
            }
            
            // Run installer: java -jar neoforge-installer.jar --installServer
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-jar",
                    installerPath.getFileName().toString(),
                    "--installServer"
            );
            
            pb.directory(serverPath.toFile());
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Read installer output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int progress = 35;
                
                while ((line = reader.readLine()) != null) {
                    logger.info("Installer: {}", line);
                    
                    if (callback != null) {
                        // Update progress based on installer output
                        if (line.contains("Downloading") || line.contains("downloading")) {
                            progress = Math.min(70, progress + 2);
                            callback.onProgress(progress, "Downloading libraries...");
                        } else if (line.contains("Installing") || line.contains("installing")) {
                            progress = Math.min(85, progress + 2);
                            callback.onProgress(progress, "Installing components...");
                        }
                    }
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                logger.info("NeoForge installer completed successfully");
                
                if (callback != null) {
                    callback.onProgress(95, "Finalizing installation...");
                }
                
                // Clean up installer
                Files.deleteIfExists(installerPath);
                
                if (callback != null) {
                    callback.onProgress(100, "NeoForge installed successfully!");
                }
                
                return true;
            } else {
                logger.error("NeoForge installer failed with exit code: {}", exitCode);
                if (callback != null) {
                    callback.onProgress(0, "Installation failed (exit code: " + exitCode + ")");
                }
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error running NeoForge installer", e);
            if (callback != null) {
                callback.onProgress(0, "Error: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Check if NeoForge is already installed
     */
    public boolean isNeoForgeInstalled() {
        // Check for NeoForge server files
        Path[] neoforgeFiles = {
                serverPath.resolve("libraries/net/neoforged/neoforge"),
                serverPath.resolve("run.bat"),
                serverPath.resolve("run.sh"),
                serverPath.resolve("user_jvm_args.txt")
        };
        
        for (Path file : neoforgeFiles) {
            if (Files.exists(file)) {
                logger.info("NeoForge installation detected: {}", file.getFileName());
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Quick install with auto-detection
     * This is the main entry point for NeoForge installation
     */
    public boolean quickInstall(String mcVersion, ServerManager.ProgressCallback callback) {
        try {
            logger.info("Starting NeoForge quick install for Minecraft {}", mcVersion);
            
            if (callback != null) {
                callback.onProgress(0, "Detecting NeoForge version...");
            }
            
            // Get latest version
            NeoForgeVersion version = getLatestVersion(mcVersion);
            logger.info("Detected version: {}", version);
            
            if (callback != null) {
                callback.onProgress(5, "Found " + version);
            }
            
            // Download installer
            Path installerPath = downloadInstaller(version, callback);
            
            // Run installer
            boolean success = runInstaller(installerPath, callback);
            
            if (success) {
                logger.info("NeoForge {} installed successfully", version);
            }
            
            return success;
            
        } catch (IOException e) {
            logger.error("Error during NeoForge installation", e);
            if (callback != null) {
                callback.onProgress(0, "Installation failed: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Get installed NeoForge version
     */
    public String getInstalledVersion() {
        try {
            // Check run.bat or run.sh for version info
            Path runScript = serverPath.resolve("run.bat");
            if (!Files.exists(runScript)) {
                runScript = serverPath.resolve("run.sh");
            }
            
            if (Files.exists(runScript)) {
                String content = Files.readString(runScript);
                // Extract version from script (neoforge-XX.X.XX.jar pattern)
                if (content.contains("neoforge-")) {
                    int start = content.indexOf("neoforge-") + 9;
                    int end = content.indexOf(".jar", start);
                    if (end > start) {
                        return content.substring(start, end);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error detecting installed NeoForge version", e);
        }
        
        return "unknown";
    }
}

