package com.zerog.stellarserverforge.zerogmods;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.net.ChecksumUtil;
import com.zerog.stellarserverforge.net.HttpFetcher;
import com.zerog.stellarserverforge.net.RateLimiter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

/**
 * Resolves and downloads a ZeroG Network mod hosted on CurseForge. By default this goes through
 * ZeroG's own hosted proxy ({@link #DEFAULT_PROXY_BASE_URL}, backed by {@code proxy-php/}), which
 * holds the real CurseForge API key server-side — so installing a CurseForge-sourced mod works
 * with zero setup for anyone using the app, without a shared secret ever shipping inside the
 * distributed jar or being asked of the user. A fork of this app that runs its own proxy just
 * changes the endpoint (Settings → ZeroG mods connection endpoint) rather than needing a personal
 * API key. A personal key is still accepted as a secondary override for anyone who wants to bypass
 * either proxy and talk to CurseForge directly.
 * <p>
 * Either way, usage is self-throttled to {@link #MAX_CALLS_PER_WINDOW} calls per rolling
 * {@link #WINDOW}, independent of whatever limits CurseForge or the proxy itself enforce, so a bug
 * or accidental loop here can't hammer either into getting throttled.
 */
public class CurseForgeInstallService {

    private static final int MAX_CALLS_PER_WINDOW = 50;
    private static final Duration WINDOW = Duration.ofHours(1);

    /** ZeroG Network's own hosted proxy (see proxy-php/README.md — a small PHP script on shared
     * cPanel hosting). Used whenever settings don't specify a different endpoint. */
    public static final String DEFAULT_PROXY_BASE_URL = "https://sfs.zerognetwork.co.za";

    private final HttpFetcher http = new HttpFetcher();
    private final ObjectMapper mapper = new ObjectMapper();
    private final RateLimiter rateLimiter;

    public CurseForgeInstallService(Path cacheDir) {
        this.rateLimiter = new RateLimiter(cacheDir.resolve("curseforge-api-usage.txt"), MAX_CALLS_PER_WINDOW, WINDOW);
    }

    /**
     * @param personalApiKey optional — when blank, requests go through the proxy instead of directly to CurseForge.
     * @param proxyBaseUrl   optional — when blank, falls back to {@link #DEFAULT_PROXY_BASE_URL}. Lets a fork point
     *                       at its own deployed proxy without needing a personal key.
     */
    public Path install(ZeroGModEntry entry, ModLoader loader, McVersion mc, Path modsDir, String personalApiKey,
                         String proxyBaseUrl) throws IOException, InterruptedException {
        rateLimiter.checkAndRecord();

        String query = "gameVersion=" + urlEncode(mc.raw()) + "&modLoaderType=" + curseForgeLoaderType(loader) + "&pageSize=50";
        String json;
        if (personalApiKey != null && !personalApiKey.isBlank()) {
            String url = "https://api.curseforge.com/v1/mods/" + urlEncode(entry.getProjectId()) + "/files?" + query;
            json = http.getString(url, Map.of("x-api-key", personalApiKey, "Accept", "application/json"));
        } else {
            String base = (proxyBaseUrl == null || proxyBaseUrl.isBlank()) ? DEFAULT_PROXY_BASE_URL : proxyBaseUrl;
            String url = base + "/curseforge.php?modId=" + urlEncode(entry.getProjectId()) + "&" + query;
            json = http.getString(url);
        }

        JsonNode data = mapper.readTree(json).path("data");
        if (!data.isArray() || data.isEmpty()) {
            throw new IOException("No CurseForge file of \"" + entry.getName() + "\" was found for " + mc.raw()
                    + " (" + loader + ").");
        }

        // Don't rely on the API returning newest-first — sort explicitly by file date.
        JsonNode chosen = StreamSupport.stream(Spliterators.spliteratorUnknownSize(data.iterator(), 0), false)
                .max(Comparator.comparing(f -> {
                    try {
                        return Instant.parse(f.path("fileDate").asText());
                    } catch (Exception e) {
                        return Instant.EPOCH;
                    }
                }))
                .orElse(data.get(0));

        String downloadUrl = chosen.path("downloadUrl").asText(null);
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IOException("CurseForge didn't provide a direct download URL for \"" + entry.getName()
                    + "\" (the author may have disabled third-party downloads) — use \"Open page\" instead.");
        }
        String filename = sanitizeFileName(chosen.path("fileName").asText(), entry.getName());

        // downloadUrl is a public CurseForge CDN link — no API key needed for the file itself,
        // whether the metadata above came from the proxy or a personal key.
        Files.createDirectories(modsDir);
        Path dest = modsDir.resolve(filename);
        http.downloadToFile(downloadUrl, dest);

        String expectedHash = null;
        String hashAlgo = null;
        for (JsonNode h : chosen.path("hashes")) {
            int algo = h.path("algo").asInt(-1);
            if (algo == 1) { // CurseForge algo 1 = SHA1
                expectedHash = h.path("value").asText(null);
                hashAlgo = "SHA-1";
                break;
            }
            if (algo == 2 && expectedHash == null) { // algo 2 = MD5, used only if no SHA1 present
                expectedHash = h.path("value").asText(null);
                hashAlgo = "MD5";
            }
        }
        if (expectedHash != null && !expectedHash.isBlank()) {
            if (!ChecksumUtil.matches(dest, expectedHash, hashAlgo)) {
                Files.deleteIfExists(dest);
                throw new IOException("Downloaded file for \"" + entry.getName()
                        + "\" failed " + hashAlgo + " verification against CurseForge's published hash — deleted, not installed.");
            }
        }
        return dest;
    }

    private static int curseForgeLoaderType(ModLoader loader) {
        return switch (loader) {
            case FORGE -> 1;
            case FABRIC -> 4;
            case QUILT -> 5;
            case NEOFORGE -> 6;
            case VANILLA -> throw new IllegalArgumentException("Vanilla servers can't install mods");
        };
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** CurseForge's reported file name ends up straight in a {@code Path.resolve()} call — reduce
     * it to a bare file name so a compromised proxy/MITM response can't escape {@code modsDir} via
     * {@code ../} segments or an absolute path. */
    private static String sanitizeFileName(String rawFilename, String modName) throws IOException {
        String name;
        try {
            name = Path.of(rawFilename).getFileName().toString();
        } catch (java.nio.file.InvalidPathException e) {
            throw new IOException("CurseForge returned an invalid file name for \"" + modName + "\".", e);
        }
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            throw new IOException("CurseForge returned an invalid file name for \"" + modName + "\".");
        }
        return name;
    }
}
