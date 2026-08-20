package com.zerog.network.stellarforge.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.zerog.network.stellarforge.model.ServerConfig;
import com.zerog.network.stellarforge.model.ServerProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages server profiles - save, load, switch, import, export
 */
public class ProfileManager {
    private static final Logger logger = LoggerFactory.getLogger(ProfileManager.class);
    private static final Path PROFILES_DIR = Paths.get("config", "profiles");
    private static final Path ACTIVE_PROFILE_FILE = Paths.get("config", "active-profile.txt");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private static ProfileManager instance;
    private final Gson gson;
    private ServerProfile activeProfile;
    
    private ProfileManager() {
        // Configure Gson with custom serializers for LocalDateTime
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, 
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> 
                        context.serialize(src.format(DATE_FORMATTER)))
                .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                        LocalDateTime.parse(json.getAsString(), DATE_FORMATTER))
                .create();
        
        ensureProfilesDirectory();
    }
    
    public static synchronized ProfileManager getInstance() {
        if (instance == null) {
            instance = new ProfileManager();
        }
        return instance;
    }
    
    /**
     * Ensure profiles directory exists
     */
    private void ensureProfilesDirectory() {
        try {
            if (!Files.exists(PROFILES_DIR)) {
                Files.createDirectories(PROFILES_DIR);
                logger.info("Created profiles directory: {}", PROFILES_DIR);
            }
        } catch (IOException e) {
            logger.error("Error creating profiles directory", e);
        }
    }
    
    /**
     * Save a profile to disk
     */
    public boolean saveProfile(ServerProfile profile) {
        try {
            profile.markAsUsed();
            Path profileFile = PROFILES_DIR.resolve(profile.getProfileId() + ".json");
            
            try (FileWriter writer = new FileWriter(profileFile.toFile())) {
                gson.toJson(profile, writer);
                logger.info("Saved profile: {} ({})", profile.getProfileName(), profile.getProfileId());
                return true;
            }
        } catch (IOException e) {
            logger.error("Error saving profile: {}", profile.getProfileName(), e);
            return false;
        }
    }
    
    /**
     * Load a profile from disk
     */
    public ServerProfile loadProfile(String profileId) {
        try {
            Path profileFile = PROFILES_DIR.resolve(profileId + ".json");
            
            if (!Files.exists(profileFile)) {
                logger.warn("Profile file not found: {}", profileId);
                return null;
            }
            
            try (FileReader reader = new FileReader(profileFile.toFile())) {
                ServerProfile profile = gson.fromJson(reader, ServerProfile.class);
                logger.info("Loaded profile: {} ({})", profile.getProfileName(), profileId);
                return profile;
            }
        } catch (IOException e) {
            logger.error("Error loading profile: {}", profileId, e);
            return null;
        }
    }
    
    /**
     * Get all available profiles
     */
    public List<ServerProfile> getAllProfiles() {
        List<ServerProfile> profiles = new ArrayList<>();
        
        try {
            File profilesFolder = PROFILES_DIR.toFile();
            if (!profilesFolder.exists()) {
                return profiles;
            }
            
            File[] files = profilesFolder.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try (FileReader reader = new FileReader(file)) {
                        ServerProfile profile = gson.fromJson(reader, ServerProfile.class);
                        if (profile != null) {
                            profiles.add(profile);
                        }
                    } catch (Exception e) {
                        logger.error("Error loading profile from file: {}", file.getName(), e);
                    }
                }
            }
            
            // Sort by last used (most recent first); profiles never used go to the end
            profiles.sort(Comparator.comparing(ServerProfile::getLastUsed,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            
        } catch (Exception e) {
            logger.error("Error getting all profiles", e);
        }
        
        return profiles;
    }
    
    /**
     * Get recently used profiles (last N)
     */
    public List<ServerProfile> getRecentProfiles(int count) {
        return getAllProfiles().stream()
                .limit(count)
                .collect(Collectors.toList());
    }
    
    /**
     * Get favorite profiles
     */
    public List<ServerProfile> getFavoriteProfiles() {
        return getAllProfiles().stream()
                .filter(ServerProfile::isFavorite)
                .collect(Collectors.toList());
    }
    
    /**
     * Delete a profile
     */
    public boolean deleteProfile(String profileId) {
        try {
            Path profileFile = PROFILES_DIR.resolve(profileId + ".json");
            
            if (Files.exists(profileFile)) {
                Files.delete(profileFile);
                logger.info("Deleted profile: {}", profileId);
                
                // If this was the active profile, clear it
                if (activeProfile != null && activeProfile.getProfileId().equals(profileId)) {
                    activeProfile = null;
                    saveActiveProfileId(null);
                }
                
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error("Error deleting profile: {}", profileId, e);
            return false;
        }
    }
    
    /**
     * Duplicate a profile with a new name
     */
    public ServerProfile duplicateProfile(ServerProfile original, String newName) {
        ServerProfile duplicate = new ServerProfile(newName, original.getConfig());
        duplicate.setDescription(original.getDescription());
        duplicate.setColorCode(original.getColorCode());
        duplicate.setFavorite(false); // Don't copy favorite status
        
        if (saveProfile(duplicate)) {
            logger.info("Duplicated profile: {} -> {}", original.getProfileName(), newName);
            return duplicate;
        }
        return null;
    }
    
    /**
     * Export profile to a file (for sharing)
     */
    public boolean exportProfile(ServerProfile profile, Path destination) {
        try {
            try (FileWriter writer = new FileWriter(destination.toFile())) {
                gson.toJson(profile, writer);
                logger.info("Exported profile: {} to {}", profile.getProfileName(), destination);
                return true;
            }
        } catch (IOException e) {
            logger.error("Error exporting profile", e);
            return false;
        }
    }
    
    /**
     * Import profile from a file
     */
    public ServerProfile importProfile(Path source) {
        try {
            try (FileReader reader = new FileReader(source.toFile())) {
                ServerProfile profile = gson.fromJson(reader, ServerProfile.class);
                
                // Generate new ID to avoid conflicts
                profile.setProfileId(java.util.UUID.randomUUID().toString());
                profile.setLastUsed(LocalDateTime.now());
                
                if (saveProfile(profile)) {
                    logger.info("Imported profile: {}", profile.getProfileName());
                    return profile;
                }
            }
        } catch (IOException e) {
            logger.error("Error importing profile", e);
        }
        return null;
    }
    
    /**
     * Set the active profile
     */
    public void setActiveProfile(ServerProfile profile) {
        this.activeProfile = profile;
        if (profile != null) {
            profile.markAsUsed();
            saveProfile(profile);
            saveActiveProfileId(profile.getProfileId());
        } else {
            saveActiveProfileId(null);
        }
    }
    
    /**
     * Get the active profile
     */
    public ServerProfile getActiveProfile() {
        if (activeProfile == null) {
            // Try to load from saved active profile ID
            String activeId = loadActiveProfileId();
            if (activeId != null) {
                activeProfile = loadProfile(activeId);
            }
            
            // If still null, try to get the most recent profile
            if (activeProfile == null) {
                List<ServerProfile> profiles = getAllProfiles();
                if (!profiles.isEmpty()) {
                    activeProfile = profiles.get(0);
                }
            }
        }
        return activeProfile;
    }
    
    /**
     * Save active profile ID to file
     */
    private void saveActiveProfileId(String profileId) {
        try {
            if (profileId == null) {
                Files.deleteIfExists(ACTIVE_PROFILE_FILE);
            } else {
                Files.writeString(ACTIVE_PROFILE_FILE, profileId);
            }
        } catch (IOException e) {
            logger.error("Error saving active profile ID", e);
        }
    }
    
    /**
     * Load active profile ID from file
     */
    private String loadActiveProfileId() {
        try {
            if (Files.exists(ACTIVE_PROFILE_FILE)) {
                return Files.readString(ACTIVE_PROFILE_FILE).trim();
            }
        } catch (IOException e) {
            logger.error("Error loading active profile ID", e);
        }
        return null;
    }
    
    /**
     * Create a default profile if none exist
     */
    public ServerProfile createDefaultProfile() {
        ServerConfig defaultConfig = new ServerConfig();
        defaultConfig.setServerName("My Server");
        defaultConfig.setServerPath("server");
        defaultConfig.setMinecraftVersion("1.20.1");
        defaultConfig.setModLoader(ServerConfig.ModLoader.FORGE);
        defaultConfig.setMaxRamGb(4);
        defaultConfig.setPort(25565);
        
        ServerProfile profile = new ServerProfile("Default Server", defaultConfig);
        profile.setDescription("Default server configuration");
        profile.setFavorite(true);
        
        if (saveProfile(profile)) {
            logger.info("Created default profile");
            return profile;
        }
        return null;
    }
}

