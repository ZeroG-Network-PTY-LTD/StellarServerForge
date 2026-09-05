package com.zerog.stellarserverforge.mojang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerog.stellarserverforge.net.HttpFetcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches/caches the Mojang master version manifest (spec §5.1) and validates that an entered
 * Minecraft version string is a real {@code release}-typed version (spec §5.2 — snapshots, betas,
 * and alphas are rejected).
 */
public class MojangManifestService {

    private static final String MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
    private static final Duration REFRESH_INTERVAL = Duration.ofDays(1);

    private final HttpFetcher http = new HttpFetcher();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path cacheDir;

    public MojangManifestService(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    private Path manifestPath() {
        return cacheDir.resolve("version_manifest_v2.json");
    }

    /** Result entry for a single manifest version. */
    public record VersionEntry(String id, String type, String url) {
    }

    /** Ensures a reasonably fresh manifest is cached locally, then returns all release versions. */
    public List<VersionEntry> fetchReleaseVersions() throws IOException, InterruptedException {
        ensureFresh();
        JsonNode root = mapper.readTree(manifestPath().toFile());
        List<VersionEntry> releases = new ArrayList<>();
        for (JsonNode v : root.get("versions")) {
            if ("release".equals(v.path("type").asText())) {
                releases.add(new VersionEntry(v.path("id").asText(), v.path("type").asText(), v.path("url").asText()));
            }
        }
        return releases;
    }

    /** The manifest's own "latest.release" pointer — lets the setup wizard offer a one-click
     * "use the latest release" shortcut instead of requiring the version to be typed by hand. */
    public String latestReleaseVersion() throws IOException, InterruptedException {
        ensureFresh();
        JsonNode root = mapper.readTree(manifestPath().toFile());
        String id = root.path("latest").path("release").asText(null);
        return (id == null || id.isBlank()) ? null : id;
    }

    public boolean isValidReleaseVersion(String minecraftVersion) throws IOException, InterruptedException {
        for (VersionEntry entry : fetchReleaseVersions()) {
            if (entry.id().equals(minecraftVersion)) {
                return true;
            }
        }
        return false;
    }

    public VersionEntry findVersion(String minecraftVersion) throws IOException, InterruptedException {
        for (VersionEntry entry : fetchReleaseVersions()) {
            if (entry.id().equals(minecraftVersion)) {
                return entry;
            }
        }
        return null;
    }

    private void ensureFresh() throws IOException, InterruptedException {
        Files.createDirectories(cacheDir);
        Path path = manifestPath();
        boolean stale = true;
        if (Files.exists(path)) {
            Instant modified = Files.getLastModifiedTime(path).toInstant();
            stale = Instant.now().isAfter(modified.plus(REFRESH_INTERVAL));
        }
        if (!stale) {
            return;
        }
        try {
            String body = http.getString(MANIFEST_URL);
            Files.writeString(path, body, StandardCharsets.UTF_8);
        } catch (IOException e) {
            if (!Files.exists(path)) {
                throw e;
            }
            // Degrade gracefully: keep using the stale cached copy if the network fetch fails.
        }
    }
}
