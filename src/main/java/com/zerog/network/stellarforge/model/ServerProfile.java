package com.zerog.network.stellarforge.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Server profile wrapper that adds metadata to ServerConfig
 * Enables multi-server profile management
 */
public class ServerProfile {
    private String profileId;
    private String profileName;
    private ServerConfig config;
    private LocalDateTime created;
    private LocalDateTime lastUsed;
    private boolean favorite;
    private String description;
    private String colorCode; // For UI visualization (e.g., "#FF5733")

    public ServerProfile() {
        this.profileId = UUID.randomUUID().toString();
        this.created = LocalDateTime.now();
        this.lastUsed = LocalDateTime.now();
        this.favorite = false;
        this.colorCode = "#0078D4"; // Default blue
    }

    public ServerProfile(String profileName, ServerConfig config) {
        this();
        this.profileName = profileName;
        this.config = config;
    }

    // Getters and Setters

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public ServerConfig getConfig() {
        return config;
    }

    public void setConfig(ServerConfig config) {
        this.config = config;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    /**
     * Update the lastUsed timestamp to now
     */
    public void markAsUsed() {
        this.lastUsed = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return profileName + (favorite ? " ⭐" : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ServerProfile that = (ServerProfile) obj;
        return profileId.equals(that.profileId);
    }

    @Override
    public int hashCode() {
        return profileId.hashCode();
    }
}

