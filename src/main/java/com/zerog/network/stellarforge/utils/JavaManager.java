package com.zerog.network.stellarforge.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java installation manager for Stellar Server Forge
 * Detects and validates Java installations
 */
public class JavaManager {
    private static final Logger logger = LoggerFactory.getLogger(JavaManager.class);
    
    private static final Pattern VERSION_PATTERN = Pattern.compile("version \"(\\d+)\\.(\\d+)\\.(\\d+)");
    private static final Pattern SIMPLE_VERSION_PATTERN = Pattern.compile("version \"(\\d+)");
    
    /**
     * Java installation information
     */
    public static class JavaInstallation {
        private final String path;
        private final int majorVersion;
        private final String fullVersion;
        private final boolean is64Bit;
        
        public JavaInstallation(String path, int majorVersion, String fullVersion, boolean is64Bit) {
            this.path = path;
            this.majorVersion = majorVersion;
            this.fullVersion = fullVersion;
            this.is64Bit = is64Bit;
        }
        
        public String getPath() { return path; }
        public int getMajorVersion() { return majorVersion; }
        public String getFullVersion() { return fullVersion; }
        public boolean is64Bit() { return is64Bit; }
        
        @Override
        public String toString() {
            return String.format("Java %d (%s) - %s", majorVersion, is64Bit ? "64-bit" : "32-bit", path);
        }
    }
    
    /**
     * Detect all Java installations on the system
     */
    public static List<JavaInstallation> detectJavaInstallations() {
        List<JavaInstallation> installations = new ArrayList<>();
        
        // Check system PATH
        JavaInstallation systemJava = detectSystemJava();
        if (systemJava != null) {
            installations.add(systemJava);
            logger.info("Found system Java: {}", systemJava);
        }
        
        // Check JAVA_HOME
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            JavaInstallation javaHomeInstall = detectJavaAt(javaHome);
            if (javaHomeInstall != null && !installations.contains(javaHomeInstall)) {
                installations.add(javaHomeInstall);
                logger.info("Found JAVA_HOME: {}", javaHomeInstall);
            }
        }
        
        // Check common installation directories (Windows)
        if (isWindows()) {
            checkWindowsJavaLocations(installations);
        }
        
        // Check common installation directories (Linux/Mac)
        if (!isWindows()) {
            checkUnixJavaLocations(installations);
        }
        
        logger.info("Detected {} Java installation(s)", installations.size());
        return installations;
    }
    
    /**
     * Detect Java in system PATH
     */
    private static JavaInstallation detectSystemJava() {
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    String versionOutput = output.toString();
                    int majorVersion = parseJavaVersion(versionOutput);
                    boolean is64Bit = versionOutput.contains("64-Bit");
                    
                    return new JavaInstallation("java", majorVersion, versionOutput.split("\n")[0], is64Bit);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not detect system Java", e);
        }
        return null;
    }
    
    /**
     * Detect Java at a specific path
     */
    private static JavaInstallation detectJavaAt(String basePath) {
        try {
            Path javaExePath = Paths.get(basePath, "bin", "java" + (isWindows() ? ".exe" : ""));
            
            if (!Files.exists(javaExePath)) {
                return null;
            }
            
            ProcessBuilder pb = new ProcessBuilder(javaExePath.toString(), "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    String versionOutput = output.toString();
                    int majorVersion = parseJavaVersion(versionOutput);
                    boolean is64Bit = versionOutput.contains("64-Bit");
                    
                    return new JavaInstallation(basePath, majorVersion, versionOutput.split("\n")[0], is64Bit);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not detect Java at {}", basePath, e);
        }
        return null;
    }
    
    /**
     * Check common Windows Java installation locations
     */
    private static void checkWindowsJavaLocations(List<JavaInstallation> installations) {
        String[] searchPaths = {
            "C:\\Program Files\\Java",
            "C:\\Program Files (x86)\\Java",
            "C:\\Program Files\\Eclipse Adoptium",
            "C:\\Program Files\\AdoptOpenJDK",
            "C:\\Program Files\\Zulu",
            System.getProperty("user.home") + "\\.jdks"
        };
        
        for (String searchPath : searchPaths) {
            File dir = new File(searchPath);
            if (dir.exists() && dir.isDirectory()) {
                File[] subdirs = dir.listFiles(File::isDirectory);
                if (subdirs != null) {
                    for (File subdir : subdirs) {
                        JavaInstallation install = detectJavaAt(subdir.getAbsolutePath());
                        if (install != null && !containsPath(installations, install.getPath())) {
                            installations.add(install);
                            logger.debug("Found Java at {}", install.getPath());
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Check common Unix/Linux Java installation locations
     */
    private static void checkUnixJavaLocations(List<JavaInstallation> installations) {
        String[] searchPaths = {
            "/usr/lib/jvm",
            "/usr/java",
            "/Library/Java/JavaVirtualMachines",
            System.getProperty("user.home") + "/.jdks"
        };
        
        for (String searchPath : searchPaths) {
            File dir = new File(searchPath);
            if (dir.exists() && dir.isDirectory()) {
                File[] subdirs = dir.listFiles(File::isDirectory);
                if (subdirs != null) {
                    for (File subdir : subdirs) {
                        // On Mac, Java is in Contents/Home
                        File macJavaHome = new File(subdir, "Contents/Home");
                        if (macJavaHome.exists()) {
                            JavaInstallation install = detectJavaAt(macJavaHome.getAbsolutePath());
                            if (install != null && !containsPath(installations, install.getPath())) {
                                installations.add(install);
                                logger.debug("Found Java at {}", install.getPath());
                            }
                        } else {
                            JavaInstallation install = detectJavaAt(subdir.getAbsolutePath());
                            if (install != null && !containsPath(installations, install.getPath())) {
                                installations.add(install);
                                logger.debug("Found Java at {}", install.getPath());
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Parse Java major version from version output
     */
    private static int parseJavaVersion(String versionOutput) {
        // Try new version format first (Java 9+): "version "11.0.1"
        Matcher simpleMatcher = SIMPLE_VERSION_PATTERN.matcher(versionOutput);
        if (simpleMatcher.find()) {
            try {
                int version = Integer.parseInt(simpleMatcher.group(1));
                if (version >= 9) {
                    return version;
                }
            } catch (NumberFormatException e) {
                logger.debug("Error parsing simple version", e);
            }
        }
        
        // Try old version format (Java 8 and earlier): "version "1.8.0_291"
        Matcher matcher = VERSION_PATTERN.matcher(versionOutput);
        if (matcher.find()) {
            try {
                int major = Integer.parseInt(matcher.group(1));
                int minor = Integer.parseInt(matcher.group(2));
                
                // Java 1.8 -> Java 8, etc.
                if (major == 1) {
                    return minor;
                }
                return major;
            } catch (NumberFormatException e) {
                logger.debug("Error parsing version", e);
            }
        }
        
        return 0;
    }
    
    /**
     * Get required Java version for a Minecraft version
     */
    public static int getRequiredJavaVersion(String minecraftVersion) {
        if (minecraftVersion == null) {
            return 17; // Default to Java 17
        }
        
        // Parse version (format: 1.20.1 or 1.20)
        String[] parts = minecraftVersion.split("\\.");
        if (parts.length < 2) {
            return 17;
        }
        
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            
            // Version 1.x
            if (major == 1) {
                if (minor <= 16) {
                    return 8;  // Java 8 for 1.16.5 and earlier
                } else if (minor == 17) {
                    return 16; // Java 16 for 1.17
                } else if (minor <= 20 || (minor == 20 && parts.length > 2 && Integer.parseInt(parts[2]) <= 4)) {
                    return 17; // Java 17 for 1.18-1.20.4
                } else {
                    return 21; // Java 21 for 1.20.5+
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("Could not parse Minecraft version: {}", minecraftVersion);
        }
        
        return 17; // Default
    }
    
    /**
     * Check if a Java installation meets version requirements
     */
    public static boolean isCompatible(JavaInstallation installation, String minecraftVersion) {
        int required = getRequiredJavaVersion(minecraftVersion);
        return installation.getMajorVersion() >= required;
    }
    
    /**
     * Find the best Java installation for a Minecraft version
     */
    public static JavaInstallation findBestJava(String minecraftVersion) {
        List<JavaInstallation> installations = detectJavaInstallations();
        int required = getRequiredJavaVersion(minecraftVersion);
        
        // Find exact match first
        for (JavaInstallation install : installations) {
            if (install.getMajorVersion() == required && install.is64Bit()) {
                return install;
            }
        }
        
        // Find any compatible 64-bit version
        for (JavaInstallation install : installations) {
            if (install.getMajorVersion() >= required && install.is64Bit()) {
                return install;
            }
        }
        
        // Find any compatible version (even 32-bit)
        for (JavaInstallation install : installations) {
            if (install.getMajorVersion() >= required) {
                return install;
            }
        }
        
        return null;
    }
    
    /**
     * Check if running on Windows
     */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
    
    /**
     * Check if list contains a path
     */
    private static boolean containsPath(List<JavaInstallation> installations, String path) {
        for (JavaInstallation install : installations) {
            if (install.getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }
}

