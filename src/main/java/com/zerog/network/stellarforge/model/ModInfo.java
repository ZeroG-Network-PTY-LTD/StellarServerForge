package com.zerog.network.stellarforge.model;

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
    
    // Server-side support fields
    private String slug;
    private String platform;
    private boolean serverSide;
    private boolean serverCompatible;
    private String serverSideStatus; // "required", "optional", "unsupported"
    
    public ModInfo() {}
    
    public ModInfo(String name, String version, String description, String fileName, ModSource source) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.fileName = fileName;
        this.source = source;
    }
    
    // Getters and setters
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
    
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public boolean isServerSide() { return serverSide; }
    public void setServerSide(boolean serverSide) { this.serverSide = serverSide; }
    
    public boolean isServerCompatible() { return serverCompatible; }
    public void setServerCompatible(boolean serverCompatible) { this.serverCompatible = serverCompatible; }
    
    public String getServerSideStatus() { return serverSideStatus; }
    public void setServerSideStatus(String serverSideStatus) { this.serverSideStatus = serverSideStatus; }
    
    public enum ModSource {
        CURSEFORGE("CurseForge"),
        MODRINTH("Modrinth"),
        LOCAL("Local");
        
        private final String displayName;
        
        ModSource(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
        
        @Override
        public String toString() { return displayName; }
    }
    
    @Override
    public String toString() {
        return name + " " + version + " (" + source.getDisplayName() + ")";
    }
}
