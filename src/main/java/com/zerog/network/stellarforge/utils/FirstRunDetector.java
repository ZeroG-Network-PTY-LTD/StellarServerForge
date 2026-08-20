package com.zerog.network.stellarforge.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Detects if this is the first run of the application
 * and manages the first-run flag
 */
public class FirstRunDetector {
    private static final Logger logger = LoggerFactory.getLogger(FirstRunDetector.class);
    private static final Path FIRST_RUN_FLAG = Paths.get("config", ".first-run-complete");
    
    /**
     * Check if this is the first run of the application
     */
    public static boolean isFirstRun() {
        boolean firstRun = !Files.exists(FIRST_RUN_FLAG);
        
        if (firstRun) {
            logger.info("First run detected");
        }
        
        return firstRun;
    }
    
    /**
     * Mark the first run as complete
     */
    public static void markFirstRunComplete() {
        try {
            // Ensure config directory exists
            Path configDir = FIRST_RUN_FLAG.getParent();
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            
            // Create the flag file
            Files.writeString(FIRST_RUN_FLAG, "First run completed on: " + java.time.LocalDateTime.now());
            logger.info("First run marked as complete");
        } catch (IOException e) {
            logger.error("Error marking first run as complete", e);
        }
    }
    
    /**
     * Reset the first run flag (for testing)
     */
    public static void resetFirstRun() {
        try {
            if (Files.exists(FIRST_RUN_FLAG)) {
                Files.delete(FIRST_RUN_FLAG);
                logger.info("First run flag reset");
            }
        } catch (IOException e) {
            logger.error("Error resetting first run flag", e);
        }
    }
    
    /**
     * Check if essential configurations are present
     */
    public static boolean hasEssentialConfiguration() {
        // Check for profiles
        ProfileManager profileManager = ProfileManager.getInstance();
        return !profileManager.getAllProfiles().isEmpty();
    }
}

