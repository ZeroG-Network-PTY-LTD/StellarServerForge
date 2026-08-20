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

    // ── Server-properties fields ───────────────────────────────────────────────
    private String  gameMode            = "survival";
    private String  difficulty          = "normal";
    private int     maxPlayers          = 20;
    private int     viewDistance        = 10;
    private boolean pvpEnabled          = true;
    private boolean onlineModeEnabled   = true;
    private boolean allowFlight         = false;
    private boolean whitelistEnabled    = false;
    private boolean commandBlocksEnabled= true;
    private String  motd                = "A Minecraft Server";
    
    public ServerConfig() {
        this.installedMods = new ArrayList<>();
        this.minecraftVersion = "1.20.1";
        this.modLoader = ModLoader.FORGE;
        this.modLoaderVersion = "47.2.0";
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

    // ── Server-properties getters/setters ─────────────────────────────────────
    public String getGameMode() { return gameMode != null ? gameMode : "survival"; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }

    public String getDifficulty() { return difficulty != null ? difficulty : "normal"; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getMaxPlayers() { return maxPlayers > 0 ? maxPlayers : 20; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public int getViewDistance() { return viewDistance > 0 ? viewDistance : 10; }
    public void setViewDistance(int viewDistance) { this.viewDistance = viewDistance; }

    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }

    public boolean isOnlineModeEnabled() { return onlineModeEnabled; }
    public void setOnlineModeEnabled(boolean onlineModeEnabled) { this.onlineModeEnabled = onlineModeEnabled; }

    public boolean isAllowFlight() { return allowFlight; }
    public void setAllowFlight(boolean allowFlight) { this.allowFlight = allowFlight; }

    public boolean isWhitelistEnabled() { return whitelistEnabled; }
    public void setWhitelistEnabled(boolean whitelistEnabled) { this.whitelistEnabled = whitelistEnabled; }

    public boolean isCommandBlocksEnabled() { return commandBlocksEnabled; }
    public void setCommandBlocksEnabled(boolean commandBlocksEnabled) { this.commandBlocksEnabled = commandBlocksEnabled; }

    public String getMotd() { return motd != null ? motd : "A Minecraft Server"; }
    public void setMotd(String motd) { this.motd = motd; }

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
