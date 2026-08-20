package com.zerog.network.stellarforge.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Snapshot of server performance metrics at a point in time.
 */
public class ServerMetrics {

    private final Instant timestamp;
    private double tps1m;            // ticks-per-second (1-min avg)
    private double tps5m;
    private double tps15m;
    private long memUsedMb;
    private long memMaxMb;
    private double cpuPercent;
    private int playerCount;
    private int maxPlayers;
    private int entities;
    private int chunksLoaded;
    private String dimension;

    // --- History buffer (kept by monitor) ---
    public static final int HISTORY_SIZE = 120; // 2 minutes @ 1-second polls

    private ServerMetrics(Instant ts) {
        this.timestamp = ts;
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static ServerMetrics empty() {
        return new ServerMetrics(Instant.now());
    }

    /** Mutable builder returned from empty() for convenient set-chain use */
    public ServerMetrics tps(double t1, double t5, double t15) {
        this.tps1m = t1; this.tps5m = t5; this.tps15m = t15; return this;
    }

    public ServerMetrics mem(long usedMb, long maxMb) {
        this.memUsedMb = usedMb; this.memMaxMb = maxMb; return this;
    }

    public ServerMetrics cpu(double pct) {
        this.cpuPercent = pct; return this;
    }

    public ServerMetrics players(int count, int max) {
        this.playerCount = count; this.maxPlayers = max; return this;
    }

    public ServerMetrics world(int entities, int chunks, String dimension) {
        this.entities = entities; this.chunksLoaded = chunks;
        this.dimension = dimension; return this;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Instant getTimestamp()    { return timestamp; }
    public double getTps1m()         { return tps1m; }
    public double getTps5m()         { return tps5m; }
    public double getTps15m()        { return tps15m; }
    public long getMemUsedMb()       { return memUsedMb; }
    public long getMemMaxMb()        { return memMaxMb; }
    public double getCpuPercent()    { return cpuPercent; }
    public int getPlayerCount()      { return playerCount; }
    public int getMaxPlayers()       { return maxPlayers; }
    public int getEntities()         { return entities; }
    public int getChunksLoaded()     { return chunksLoaded; }
    public String getDimension()     { return dimension; }

    /** % memory used, 0-100 */
    public int memPercent() {
        if (memMaxMb <= 0) return 0;
        return (int) (memUsedMb * 100 / memMaxMb);
    }

    // ── Thread-safe history list ───────────────────────────────────────────────

    public static List<ServerMetrics> trimmedHistory(List<ServerMetrics> history) {
        if (history.size() <= HISTORY_SIZE) return history;
        return new ArrayList<>(history.subList(history.size() - HISTORY_SIZE, history.size()));
    }
}

