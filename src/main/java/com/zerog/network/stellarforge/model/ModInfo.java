package com.zerog.network.stellarforge.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for mod information - Stellar Server Forge
 */
public class ModInfo {
    private String name;
    private String version;
    private String description;
    private String fileName;
    private String url;
    private String projectId;
    private String fileId;
    private ModSource source;
    private String minecraftVersion;
    private String modLoaderType;
    private long fileSize;

    // ── Extended fields ───────────────────────────────────────────────────────
    private String slug;              // Modrinth/CurseForge slug for URL building
    private String iconUrl;           // Thumbnail/icon URL
    private long downloadCount;       // Total download count
    private double rating;            // Star rating 0-5 (where available)
    private List<String> categories = new ArrayList<>(); // e.g. "tech", "magic", "utility"
    private String author;
    private String changelog;
    private List<String> dependencies = new ArrayList<>(); // dependency project IDs
    private boolean installed;        // true if already in mods folder

    public ModInfo() {}

    public ModInfo(String name, String version, String description, String fileName, ModSource source) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.fileName = fileName;
        this.source = source;
    }

    // ── Basic getters/setters ─────────────────────────────────────────────────
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public ModSource getSource() { return source; }
    public void setSource(ModSource source) { this.source = source; }

    public String getMinecraftVersion() { return minecraftVersion; }
    public void setMinecraftVersion(String minecraftVersion) { this.minecraftVersion = minecraftVersion; }

    public String getModLoaderType() { return modLoaderType; }
    public void setModLoaderType(String modLoaderType) { this.modLoaderType = modLoaderType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    // ── Extended getters/setters ──────────────────────────────────────────────
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public long getDownloadCount() { return downloadCount; }
    public void setDownloadCount(long downloadCount) { this.downloadCount = downloadCount; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories != null ? categories : new ArrayList<>(); }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getChangelog() { return changelog; }
    public void setChangelog(String changelog) { this.changelog = changelog; }

    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies != null ? dependencies : new ArrayList<>(); }

    public boolean isInstalled() { return installed; }
    public void setInstalled(boolean installed) { this.installed = installed; }

    /** Format download count with K/M suffix */
    public String getFormattedDownloads() {
        if (downloadCount >= 1_000_000) return String.format("%.1fM", downloadCount / 1_000_000.0);
        if (downloadCount >= 1_000)     return String.format("%.1fK", downloadCount / 1_000.0);
        return String.valueOf(downloadCount);
    }

    /** Return a Unicode star rating string like ★★★☆☆ */
    public String getStarRating() {
        int stars = (int) Math.round(rating);
        return "★".repeat(Math.max(0, stars)) + "☆".repeat(Math.max(0, 5 - stars));
    }

    public enum ModSource {
        CURSEFORGE("CurseForge"),
        MODRINTH("Modrinth"),
        LOCAL("Local");

        private final String displayName;
        ModSource(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
        @Override public String toString() { return displayName; }
    }

    @Override
    public String toString() {
        return name + " " + version + " (" + (source != null ? source.getDisplayName() : "?") + ")";
    }
}
