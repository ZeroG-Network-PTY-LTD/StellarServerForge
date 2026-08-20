package com.zerog.network.stellarforge.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-level caching system:
 *   1. In-memory ConcurrentHashMap with TTL eviction
 *   2. Disk cache in config/cache/ (JSON strings)
 *
 * Usage:
 *   CacheManager cm = CacheManager.getInstance();
 *   cm.put("version-manifest", jsonString, Duration.ofHours(24));
 *   Optional<String> val = cm.get("version-manifest");
 */
public class CacheManager {

    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    private static final Path CACHE_DIR = Paths.get("config", "cache");
    private static CacheManager INSTANCE;

    // ── In-memory store ───────────────────────────────────────────────────────

    private static class Entry {
        final String value;
        final Instant expiry;

        Entry(String value, long ttlMillis) {
            this.value  = value;
            this.expiry = Instant.now().plusMillis(ttlMillis);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }

    private final Map<String, Entry> memCache = new ConcurrentHashMap<>();

    // ── Singleton ─────────────────────────────────────────────────────────────

    public static synchronized CacheManager getInstance() {
        if (INSTANCE == null) INSTANCE = new CacheManager();
        return INSTANCE;
    }

    private CacheManager() {
        try {
            Files.createDirectories(CACHE_DIR);
        } catch (IOException e) {
            logger.warn("Cannot create cache directory: {}", e.getMessage());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Store a value with a TTL in milliseconds.
     */
    public void put(String key, String value, long ttlMillis) {
        memCache.put(key, new Entry(value, ttlMillis));
        writeToDisk(key, value, ttlMillis);
    }

    /** Convenience: put with seconds */
    public void putSeconds(String key, String value, int ttlSeconds) {
        put(key, value, ttlSeconds * 1000L);
    }

    /**
     * Retrieve a cached value.  Returns null if absent or expired.
     */
    public String get(String key) {
        // Try memory first
        Entry entry = memCache.get(key);
        if (entry != null) {
            if (!entry.isExpired()) return entry.value;
            memCache.remove(key);
        }

        // Try disk
        String diskVal = readFromDisk(key);
        if (diskVal != null) {
            // Re-load into memory with a short TTL so re-reads are fast
            memCache.put(key, new Entry(diskVal, 60_000)); // 1-minute in-memory rehydration
            return diskVal;
        }

        return null;
    }

    /** Remove entry from both memory and disk */
    public void invalidate(String key) {
        memCache.remove(key);
        deleteFromDisk(key);
    }

    /** Remove all expired entries from memory */
    public void evictExpired() {
        memCache.entrySet().removeIf(e -> e.getValue().isExpired());
        logger.debug("Cache eviction complete. Remaining: {}", memCache.size());
    }

    /** Clear everything */
    public void clearAll() {
        memCache.clear();
        try {
            if (Files.exists(CACHE_DIR)) {
                try (var stream = Files.list(CACHE_DIR)) {
                    stream.filter(p -> p.toString().endsWith(".cache"))
                          .forEach(p -> {
                              try { Files.delete(p); } catch (IOException ignored) {}
                          });
                }
            }
        } catch (IOException e) {
            logger.warn("Error clearing disk cache: {}", e.getMessage());
        }
    }

    // ── Disk helpers ──────────────────────────────────────────────────────────

    private Path cacheFile(String key) {
        // Sanitize key to valid filename
        String safe = key.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return CACHE_DIR.resolve(safe + ".cache");
    }

    private void writeToDisk(String key, String value, long ttlMillis) {
        try {
            Path f = cacheFile(key);
            long expiry = System.currentTimeMillis() + ttlMillis;
            // Format: first line = expiry epoch millis, rest = value
            String content = expiry + "\n" + value;
            Files.writeString(f, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            logger.debug("Disk cache write failed for '{}': {}", key, e.getMessage());
        }
    }

    private String readFromDisk(String key) {
        try {
            Path f = cacheFile(key);
            if (!Files.exists(f)) return null;
            String content = Files.readString(f, StandardCharsets.UTF_8);
            int nl = content.indexOf('\n');
            if (nl < 0) return null;
            long expiry = Long.parseLong(content.substring(0, nl).trim());
            if (System.currentTimeMillis() > expiry) {
                Files.deleteIfExists(f);
                return null;
            }
            return content.substring(nl + 1);
        } catch (Exception e) {
            logger.debug("Disk cache read failed for '{}': {}", key, e.getMessage());
            return null;
        }
    }

    private void deleteFromDisk(String key) {
        try {
            Files.deleteIfExists(cacheFile(key));
        } catch (IOException ignored) {}
    }
}
