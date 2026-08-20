package com.zerog.stellarserverforge.mods;

import com.zerog.stellarserverforge.net.HttpFetcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Fetches/caches the community-curated {@code clientonlymods.txt} list (spec §10.1), refreshed
 * hourly with graceful degradation to a stale cached copy on fetch failure.
 */
public class ClientOnlyModListService {

    private static final String URL = "https://raw.githubusercontent.com/nanonestor/utilities/main/clientonlymods.txt";
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(1);

    private final HttpFetcher http = new HttpFetcher();
    private final Path cacheDir;

    public ClientOnlyModListService(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    public Set<String> fetch() throws IOException, InterruptedException {
        Files.createDirectories(cacheDir);
        Path path = cacheDir.resolve("clientonlymods.txt");

        boolean stale = true;
        if (Files.exists(path)) {
            Instant modified = Files.getLastModifiedTime(path).toInstant();
            stale = Instant.now().isAfter(modified.plus(REFRESH_INTERVAL));
        }
        if (stale) {
            try {
                Files.writeString(path, http.getString(URL), StandardCharsets.UTF_8);
            } catch (IOException e) {
                if (!Files.exists(path)) {
                    throw e;
                }
                // Degrade gracefully to the stale cached copy.
            }
        }

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        return lines.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toSet());
    }
}
