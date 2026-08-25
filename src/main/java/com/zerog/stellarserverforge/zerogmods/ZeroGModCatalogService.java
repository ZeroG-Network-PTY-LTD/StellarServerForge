package com.zerog.stellarserverforge.zerogmods;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zerog.stellarserverforge.net.HttpFetcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches the user-maintained "mods ZeroG Network has made" catalog from a raw GitHub URL (or any
 * plain-JSON URL) they supply, and keeps a local last-known-good copy so the list is still usable
 * if the URL is briefly unreachable.
 * <p>
 * Since this file is meant to be hand-edited, parsing is per-entry tolerant: one bad entry (a
 * typo'd {@code source} value, a missing field) is skipped with a reason rather than failing the
 * entire catalog — see {@link FetchResult#skipped()}.
 */
public class ZeroGModCatalogService {

    public record FetchResult(List<ZeroGModEntry> entries, List<String> skipped) {
    }

    private final HttpFetcher http = new HttpFetcher();
    private final JsonMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();
    private final Path cacheFile;

    public ZeroGModCatalogService(Path cacheDir) {
        this.cacheFile = cacheDir.resolve("zerog-mods-catalog.json");
    }

    public FetchResult fetch(String url) throws IOException, InterruptedException {
        if (url == null || url.isBlank()) {
            throw new IOException("No catalog URL is set — add the raw GitHub URL to your mods catalog first.");
        }
        String json = http.getString(url);
        FetchResult result = parse(json);
        Files.createDirectories(cacheFile.getParent());
        Files.writeString(cacheFile, json);
        return result;
    }

    public boolean hasCachedCopy() {
        return Files.isRegularFile(cacheFile);
    }

    public FetchResult loadCached() throws IOException {
        return parse(Files.readString(cacheFile));
    }

    private FetchResult parse(String json) throws IOException {
        JsonNode root = mapper.readTree(json);
        if (!root.isArray()) {
            throw new IOException("Catalog JSON must be an array of mod entries.");
        }

        List<ZeroGModEntry> entries = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        int index = 0;
        for (JsonNode node : root) {
            index++;
            try {
                ZeroGModEntry entry = mapper.treeToValue(node, ZeroGModEntry.class);
                if (entry.getName() == null || entry.getName().isBlank()) {
                    skipped.add("Entry #" + index + ": missing \"name\"");
                    continue;
                }
                if (entry.getSource() == null) {
                    skipped.add("Entry #" + index + " (\"" + entry.getName() + "\"): missing/invalid \"source\"");
                    continue;
                }
                if (entry.getProjectId() == null || entry.getProjectId().isBlank()) {
                    skipped.add("Entry #" + index + " (\"" + entry.getName() + "\"): missing \"projectId\"");
                    continue;
                }
                entries.add(entry);
            } catch (Exception e) {
                skipped.add("Entry #" + index + ": " + rootMessage(e));
            }
        }
        return new FetchResult(entries, skipped);
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.toString();
    }
}
