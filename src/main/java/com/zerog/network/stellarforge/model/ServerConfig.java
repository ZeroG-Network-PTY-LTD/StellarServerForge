package com.zerog.network.stellarforge.model;

import com.zerog.network.stellarforge.config.SecureConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Server configuration model - Stellar Server Forge
 */
public class ServerConfig {
    private String minecraftVersion;
    private ModLoader modLoader;
    private String modLoaderVersion;
    private String javaVersion;
    private String customJavaPath;
    private int maxRamGb;
    private int port;
    private String serverName;
    private String serverPath;
    private List<ModInfo> installedMods;
    private boolean autoRestart;
    private boolean upnpEnabled;
    private String jvmArgs;
    
    public ServerConfig() {
        this.installedMods = new ArrayList<>();
        this.minecraftVersion = "1.20.1";
        this.modLoader = ModLoader.FORGE;
        this.modLoaderVersion = "Latest";
        this.javaVersion = "17";
        this.maxRamGb = Integer.parseInt(SecureConfig.getInstance().getProperty("default.ram.gb", "4"));
        this.port = Integer.parseInt(SecureConfig.getInstance().getProperty("default.port", "25565"));
        this.serverName = SecureConfig.getInstance().getProperty("default.server.name", "ZeroG Server");
        this.autoRestart = false;
        this.upnpEnabled = false;
        this.jvmArgs = "-XX:+UseG1GC -Dsun.rmi.dgc.server.gcInterval=2147483646 -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M";
    }
    
    // Getters and setters
    public String getMinecraftVersion() { return minecraftVersion; }
    public void setMinecraftVersion(String minecraftVersion) { this.minecraftVersion = minecraftVersion; }
    
    public ModLoader getModLoader() { return modLoader; }
    public void setModLoader(ModLoader modLoader) { this.modLoader = modLoader; }
    
    public String getModLoaderVersion() { return modLoaderVersion; }
    public void setModLoaderVersion(String modLoaderVersion) { this.modLoaderVersion = modLoaderVersion; }
    
    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }
    
    public String getCustomJavaPath() { return customJavaPath; }
    public void setCustomJavaPath(String customJavaPath) { this.customJavaPath = customJavaPath; }
    
    public int getMaxRamGb() { return maxRamGb; }
    public void setMaxRamGb(int maxRamGb) { this.maxRamGb = maxRamGb; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    
    public String getServerPath() { return serverPath; }
    public void setServerPath(String serverPath) { this.serverPath = serverPath; }
    
    public List<ModInfo> getInstalledMods() { return installedMods; }
    public void setInstalledMods(List<ModInfo> installedMods) { this.installedMods = installedMods; }
    
    public boolean isAutoRestart() { return autoRestart; }
    public void setAutoRestart(boolean autoRestart) { this.autoRestart = autoRestart; }
    
    public boolean isUpnpEnabled() { return upnpEnabled; }
    public void setUpnpEnabled(boolean upnpEnabled) { this.upnpEnabled = upnpEnabled; }
    
    public String getJvmArgs() { return jvmArgs; }
    public void setJvmArgs(String jvmArgs) { this.jvmArgs = jvmArgs; }
    
    public enum ModLoader {
        FORGE("Forge"),
        FABRIC("Fabric"),
        QUILT("Quilt"),
        NEOFORGE("NeoForge");
        
        private final String displayName;
        
        ModLoader(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
        
        @Override
        public String toString() { return displayName; }
    }
}
