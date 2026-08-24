package com.zerog.stellarserverforge.zerogmods;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.net.HttpFetcher;
import com.zerog.stellarserverforge.net.RateLimiter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Resolves and downloads a ZeroG Network mod hosted on CurseForge. By default this goes through
 * ZeroG's own proxy ({@code proxy/curseforge-proxy}), which holds the real CurseForge API key
 * server-side — so installing a CurseForge-sourced mod works with zero setup for anyone using the
 * app, without a shared secret ever shipping inside the distributed jar. Users who supply their
 * own personal API key (from console.curseforge.com) bypass the proxy and talk to CurseForge
 * directly with it instead — useful if the proxy is ever unreachable.
 * <p>
 * Either way, usage is self-throttled to {@link #MAX_CALLS_PER_WINDOW} calls per rolling
 * {@link #WINDOW}, independent of whatever limits CurseForge or the proxy itself enforce, so a bug
 * or accidental loop here can't hammer either into getting throttled.
 */
public class CurseForgeInstallService {

    private static final int MAX_CALLS_PER_WINDOW = 50;
    private static final Duration WINDOW = Duration.ofHours(1);

    /** ZeroG Network's proxy that holds the real CurseForge API key server-side (see
     * proxy-php/README.md — a small PHP script hosted on shared cPanel hosting). Update once
     * deployed; a Cloudflare Workers alternative also exists in proxy/curseforge-proxy/ if that
     * ever becomes the preferred host instead. */
    private static final String DEFAULT_PROXY_BASE_URL = "https://sfs.zerognetwork.co.za";

    private final HttpFetcher http = new HttpFetcher();
    private final ObjectMapper mapper = new ObjectMapper();
    private final RateLimiter rateLimiter;

    public CurseForgeInstallService(Path cacheDir) {
        this.rateLimiter = new RateLimiter(cacheDir.resolve("curseforge-api-usage.txt"), MAX_CALLS_PER_WINDOW, WINDOW);
    }

    /** @param personalApiKey optional — when blank, requests go through ZeroG's proxy instead of directly to CurseForge. */
    public Path install(ZeroGModEntry entry, ModLoader loader, McVersion mc, Path modsDir, String personalApiKey)
            throws IOException, InterruptedException {
        rateLimiter.checkAndRecord();

        String query = "gameVersion=" + urlEncode(mc.raw()) + "&modLoaderType=" + curseForgeLoaderType(loader) + "&pageSize=50";
        String json;
        if (personalApiKey != null && !personalApiKey.isBlank()) {
            String url = "https://api.curseforge.com/v1/mods/" + urlEncode(entry.getProjectId()) + "/files?" + query;
            json = http.getString(url, Map.of("x-api-key", personalApiKey, "Accept", "application/json"));
        } else {
            String url = DEFAULT_PROXY_BASE_URL + "/curseforge.php?modId=" + urlEncode(entry.getProjectId()) + "&" + query;
            json = http.getString(url);
        }

        JsonNode data = mapper.readTree(json).path("data");
        if (!data.isArray() || data.isEmpty()) {
            throw new IOException("No CurseForge file of \"" + entry.getName() + "\" was found for " + mc.raw()
                    + " (" + loader + ").");
        }

        JsonNode chosen = data.get(0);
        String downloadUrl = chosen.path("downloadUrl").asText(null);
        String filename = chosen.path("fileName").asText();
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IOException("CurseForge didn't provide a direct download URL for \"" + entry.getName()
                    + "\" (the author may have disabled third-party downloads) — use \"Open page\" instead.");
        }

        // downloadUrl is a public CurseForge CDN link — no API key needed for the file itself,
        // whether the metadata above came from the proxy or a personal key.
        Files.createDirectories(modsDir);
        Path dest = modsDir.resolve(filename);
        http.downloadToFile(downloadUrl, dest);
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
}
