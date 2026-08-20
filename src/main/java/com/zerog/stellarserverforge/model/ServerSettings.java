package com.zerog.stellarserverforge.model;

/**
 * Persisted per-server settings, equivalent to the original {@code settings-universalator.txt}
 * but stored as plain JSON key/value data rather than an executable batch fragment.
 */
public class ServerSettings {

    public static final String DEFAULT_ARGS =
            "-XX:+UseG1GC -Dsun.rmi.dgc.server.gcInterval=2147483646 -XX:+UnlockExperimentalVMOptions "
                    + "-XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M";

    /** Always appended, never persisted as user-editable — mirrors the bat script's OTHERARGS constant. */
    public static final String OTHER_ARGS = "-XX:+IgnoreUnrecognizedVMOptions -Dlog4j2.formatMsgNoLookups=true";

    private String minecraftVersion;
    private ModLoader modLoader = ModLoader.VANILLA;
    private String modLoaderVersion = "";
    private int javaVersion;
    private int maxRamGigs = 4;
    private String args = DEFAULT_ARGS;
    private boolean askModsCheck = true;
    private int port = 25565;
    private int portUdp = 24454;
    private String protocol = "TCP";
    private boolean usePortForwarded = false;
    private JavaOverrideMode javaOverrideMode = JavaOverrideMode.AUTOMATIC;

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    public ModLoader getModLoader() {
        return modLoader;
    }

    public void setModLoader(ModLoader modLoader) {
        this.modLoader = modLoader;
    }

    public String getModLoaderVersion() {
        return modLoaderVersion;
    }

    public void setModLoaderVersion(String modLoaderVersion) {
        this.modLoaderVersion = modLoaderVersion;
    }

    public int getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(int javaVersion) {
        this.javaVersion = javaVersion;
    }

    public int getMaxRamGigs() {
        return maxRamGigs;
    }

    public void setMaxRamGigs(int maxRamGigs) {
        this.maxRamGigs = maxRamGigs;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public boolean isAskModsCheck() {
        return askModsCheck;
    }

    public void setAskModsCheck(boolean askModsCheck) {
        this.askModsCheck = askModsCheck;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPortUdp() {
        return portUdp;
    }

    public void setPortUdp(int portUdp) {
        this.portUdp = portUdp;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isUsePortForwarded() {
        return usePortForwarded;
    }

    public void setUsePortForwarded(boolean usePortForwarded) {
        this.usePortForwarded = usePortForwarded;
    }

    public JavaOverrideMode getJavaOverrideMode() {
        return javaOverrideMode;
    }

    public void setJavaOverrideMode(JavaOverrideMode javaOverrideMode) {
        this.javaOverrideMode = javaOverrideMode;
    }

    /** Whether {@link #args} is still exactly the unmodified default string. */
    public boolean hasDefaultArgs() {
        return DEFAULT_ARGS.equals(args);
    }
}
