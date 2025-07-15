package com.zerog.network.stellarforge.modpack;

/**
 * Configuration for imported modpack
 */
public class ModpackConfig {
    private String minecraftVersion;
    private String modLoader;
    private String modLoaderVersion;
    private String serverPath;
    private String serverName;
    
    public ModpackConfig() {}
    
    public ModpackConfig(String minecraftVersion, String modLoader, String modLoaderVersion, 
                        String serverPath, String serverName) {
        this.minecraftVersion = minecraftVersion;
        this.modLoader = modLoader;
        this.modLoaderVersion = modLoaderVersion;
        this.serverPath = serverPath;
        this.serverName = serverName;
    }
    
    // Getters and setters
    public String getMinecraftVersion() { return minecraftVersion; }
    public void setMinecraftVersion(String minecraftVersion) { this.minecraftVersion = minecraftVersion; }
    
    public String getModLoader() { return modLoader; }
    public void setModLoader(String modLoader) { this.modLoader = modLoader; }
    
    public String getModLoaderVersion() { return modLoaderVersion; }
    public void setModLoaderVersion(String modLoaderVersion) { this.modLoaderVersion = modLoaderVersion; }
    
    public String getServerPath() { return serverPath; }
    public void setServerPath(String serverPath) { this.serverPath = serverPath; }
    
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
}
