package com.zerog.network.stellarforge.model;

import com.zerog.network.stellarforge.model.ServerConfig;

/**
 * A named preset that pre-fills a ServerConfig with recommended settings.
 *
 * Presets available:
 *   VANILLA      – plain server, minimal resources
 *   PERFORMANCE  – optimized JVM args, reduced view distance
 *   CREATIVE     – Creative mode, flight, no damage
 *   SURVIVAL     – Standard survival, normal difficulty
 *   LARGE_SERVER – High RAM, high player limit
 *   MODDED       – Pre-configured for heavy mod packs
 */
public class ConfigTemplate {

    public enum TemplateType {
        VANILLA     ("Vanilla Server",       "Minimal plain server — great starting point"),
        PERFORMANCE ("Performance Modded",   "Aikar JVM flags, lower view distance"),
        CREATIVE    ("Creative World",       "Creative mode, flight enabled, no damage"),
        SURVIVAL    ("Survival Hardcore",    "Hard difficulty, PvP on"),
        LARGE_SERVER("Large Community",      "High RAM, 100 player limit, PvP on"),
        MODDED      ("Heavy Modded",         "Reserved for large mod packs (8 GB+ RAM)");

        public final String label;
        public final String description;

        TemplateType(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Apply the given template to the provided ServerConfig in-place.
     */
    public static void apply(TemplateType type, ServerConfig cfg) {
        switch (type) {
            case VANILLA:
                cfg.setMaxRamGb(2);
                cfg.setViewDistance(10);
                cfg.setGameMode("survival");
                cfg.setDifficulty("normal");
                cfg.setMaxPlayers(20);
                cfg.setPvpEnabled(true);
                cfg.setJvmArgs("");
                break;

            case PERFORMANCE:
                cfg.setMaxRamGb(4);
                cfg.setViewDistance(8);
                cfg.setGameMode("survival");
                cfg.setDifficulty("normal");
                cfg.setMaxPlayers(40);
                cfg.setPvpEnabled(true);
                cfg.setJvmArgs(AIKAR_FLAGS_4G);
                break;

            case CREATIVE:
                cfg.setMaxRamGb(2);
                cfg.setViewDistance(12);
                cfg.setGameMode("creative");
                cfg.setDifficulty("peaceful");
                cfg.setMaxPlayers(20);
                cfg.setPvpEnabled(false);
                cfg.setJvmArgs("");
                break;

            case SURVIVAL:
                cfg.setMaxRamGb(3);
                cfg.setViewDistance(10);
                cfg.setGameMode("survival");
                cfg.setDifficulty("hard");
                cfg.setMaxPlayers(20);
                cfg.setPvpEnabled(true);
                cfg.setJvmArgs("");
                break;

            case LARGE_SERVER:
                cfg.setMaxRamGb(12);
                cfg.setViewDistance(12);
                cfg.setGameMode("survival");
                cfg.setDifficulty("normal");
                cfg.setMaxPlayers(100);
                cfg.setPvpEnabled(true);
                cfg.setJvmArgs(AIKAR_FLAGS_12G);
                break;

            case MODDED:
                cfg.setMaxRamGb(8);
                cfg.setViewDistance(8);
                cfg.setGameMode("survival");
                cfg.setDifficulty("normal");
                cfg.setMaxPlayers(30);
                cfg.setPvpEnabled(true);
                cfg.setJvmArgs(AIKAR_FLAGS_8G);
                break;
        }
    }

    // ── Aikar JVM flags (the community-standard Minecraft server JVM args) ────

    private static final String AIKAR_FLAGS_4G =
            "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 " +
            "-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch " +
            "-XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:G1MixedGCLiveThresholdPercent=90 " +
            "-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem " +
            "-XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs " +
            "-Daikars.new.flags=true -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 " +
            "-XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:InitiatingHeapOccupancyPercent=15";

    private static final String AIKAR_FLAGS_8G =
            AIKAR_FLAGS_4G.replace("-XX:G1NewSizePercent=30", "-XX:G1NewSizePercent=40")
                          .replace("-XX:G1MaxNewSizePercent=40", "-XX:G1MaxNewSizePercent=50")
                          .replace("-XX:G1HeapRegionSize=8M", "-XX:G1HeapRegionSize=16M")
                          .replace("-XX:G1ReservePercent=20", "-XX:G1ReservePercent=15")
                          .replace("-XX:InitiatingHeapOccupancyPercent=15", "-XX:InitiatingHeapOccupancyPercent=20");

    private static final String AIKAR_FLAGS_12G = AIKAR_FLAGS_8G;
}

