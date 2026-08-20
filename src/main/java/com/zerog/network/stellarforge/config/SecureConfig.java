package com.zerog.network.stellarforge.config;

import com.zerog.network.stellarforge.security.KeyVault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Secure configuration manager for Stellar Server Forge
 * Manages application settings and secure API keys
 */
public class SecureConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecureConfig.class);
    
    private static final String APP_NAME = "Stellar Server Forge";
    private static final String APP_VERSION = "1.0.0";
    private static final String ORGANIZATION = "ZeroG Network";
    
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE = "stellar-forge.properties";
    private static final String API_KEYS_FILE = "api-keys.properties";
    
    private static SecureConfig instance;
    
    private final Properties appConfig;
    private final Properties apiKeysConfig;
    private final Path configDir;
    
    private SecureConfig() {
        this.configDir = Paths.get(CONFIG_DIR);
        this.appConfig = new Properties();
        this.apiKeysConfig = new Properties();
        
        initialize();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized SecureConfig getInstance() {
        if (instance == null) {
            instance = new SecureConfig();
        }
        return instance;
    }
    
    /**
     * Initialize configuration
     */
    private void initialize() {
        try {
            // Create config directory if it doesn't exist
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                logger.info("Created configuration directory: {}", configDir.toAbsolutePath());
            }
            
            // Load or create configuration files
            loadAppConfig();
            loadApiKeysConfig();
            
            logger.info("{} v{} configuration initialized successfully", APP_NAME, APP_VERSION);
        } catch (Exception e) {
            logger.error("Failed to initialize configuration", e);
            throw new RuntimeException("Configuration initialization failed", e);
        }
    }
    
    /**
     * Load application configuration
     */
    private void loadAppConfig() throws IOException {
        Path configFile = configDir.resolve(CONFIG_FILE);
        
        if (Files.exists(configFile)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                appConfig.load(in);
                logger.info("Loaded application configuration from {}", configFile);
            }
        } else {
            // Create default configuration
            createDefaultAppConfig();
            saveAppConfig();
        }
    }
    
    /**
     * Create default application configuration
     */
    private void createDefaultAppConfig() {
        appConfig.setProperty("default.ram.gb", "4");
        appConfig.setProperty("default.port", "25565");
        appConfig.setProperty("default.server.name", "ZeroG Server");
        appConfig.setProperty("default.minecraft.version", "1.20.1");
        appConfig.setProperty("default.mod.loader", "FORGE");
        appConfig.setProperty("theme", "dark");
        appConfig.setProperty("auto.check.updates", "true");
        appConfig.setProperty("enable.logging", "true");
        
        logger.info("Created default application configuration");
    }
    
    /**
     * Save application configuration
     */
    private void saveAppConfig() throws IOException {
        Path configFile = configDir.resolve(CONFIG_FILE);
        try (OutputStream out = Files.newOutputStream(configFile)) {
            appConfig.store(out, APP_NAME + " Configuration");
            logger.info("Saved application configuration to {}", configFile);
        }
    }
    
    /**
     * Load API keys configuration
     */
    private void loadApiKeysConfig() throws IOException {
        Path apiKeysFile = configDir.resolve(API_KEYS_FILE);
        
        if (Files.exists(apiKeysFile)) {
            try (InputStream in = Files.newInputStream(apiKeysFile)) {
                apiKeysConfig.load(in);
                logger.info("Loaded API keys configuration from {}", apiKeysFile);
            }
        } else {
            // Create template API keys file
            createApiKeysTemplate();
        }
    }
    
    /**
     * Create API keys template file
     */
    private void createApiKeysTemplate() throws IOException {
        Path apiKeysFile = configDir.resolve(API_KEYS_FILE);
        
        // Create a template with instructions
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(apiKeysFile))) {
            writer.println("# " + APP_NAME + " API Keys Configuration");
            writer.println("# IMPORTANT: Keep this file secure and never commit it to version control");
            writer.println("#");
            writer.println("# Get your CurseForge API key from: https://console.curseforge.com/");
            writer.println("# Modrinth API key is optional (API works without authentication for basic operations)");
            writer.println();
            writer.println("# CurseForge API Key (REQUIRED for mod installation)");
            writer.println("curseforge.api.key=YOUR_CURSEFORGE_API_KEY_HERE");
            writer.println();
            writer.println("# Modrinth API Key (OPTIONAL)");
            writer.println("modrinth.api.key=");
            writer.println();
            writer.println("# Enable/disable API platforms");
            writer.println("curseforge.enabled=true");
            writer.println("modrinth.enabled=true");
        }
        
        logger.info("Created API keys template file at {}", apiKeysFile);
        logger.warn("Please configure your API keys in {}", apiKeysFile.toAbsolutePath());
    }
    
    /**
     * Get CurseForge API key
     */
    public String getCurseForgeApiKey() {
        // First check external config file
        String externalKey = apiKeysConfig.getProperty("curseforge.api.key");
        if (externalKey != null && !externalKey.isEmpty() && !externalKey.equals("YOUR_CURSEFORGE_API_KEY_HERE")) {
            return externalKey;
        }
        
        // Fall back to embedded key from KeyVault
        String embeddedKey = KeyVault.getCurseForgeApiKey();
        if (embeddedKey != null) {
            return embeddedKey;
        }
        
        logger.warn("CurseForge API key not configured");
        return null;
    }
    
    /**
     * Get Modrinth API key
     */
    public String getModrinthApiKey() {
        // Check external config first
        String externalKey = apiKeysConfig.getProperty("modrinth.api.key");
        if (externalKey != null && !externalKey.isEmpty()) {
            return externalKey;
        }
        
        // Fall back to embedded key
        return KeyVault.getModrinthApiKey();
    }
    
    /**
     * Get Modrinth Client ID
     */
    public String getModrinthClientId() {
        // Check external config first
        String externalId = apiKeysConfig.getProperty("modrinth.client.id");
        if (externalId != null && !externalId.isEmpty()) {
            return externalId;
        }
        
        // Fall back to embedded client ID
        return KeyVault.getModrinthClientId();
    }
    
    /**
     * Check if CurseForge is enabled and configured
     */
    public boolean isCurseForgeEnabled() {
        boolean enabled = Boolean.parseBoolean(apiKeysConfig.getProperty("curseforge.enabled", "true"));
        return enabled && getCurseForgeApiKey() != null;
    }
    
    /**
     * Check if Modrinth is enabled
     */
    public boolean isModrinthEnabled() {
        return Boolean.parseBoolean(apiKeysConfig.getProperty("modrinth.enabled", "true"));
    }
    
    /**
     * Get application property
     */
    public String getProperty(String key, String defaultValue) {
        return appConfig.getProperty(key, defaultValue);
    }
    
    /**
     * Set application property
     */
    public void setProperty(String key, String value) {
        appConfig.setProperty(key, value);
        try {
            saveAppConfig();
        } catch (IOException e) {
            logger.error("Failed to save configuration", e);
        }
    }
    
    /**
     * Get application name
     */
    public String getAppName() {
        return APP_NAME;
    }
    
    /**
     * Get application version
     */
    public String getAppVersion() {
        return APP_VERSION;
    }
    
    /**
     * Get organization name
     */
    public String getOrganization() {
        return ORGANIZATION;
    }
    
    /**
     * Get configuration directory path
     */
    public Path getConfigDir() {
        return configDir;
    }
}

