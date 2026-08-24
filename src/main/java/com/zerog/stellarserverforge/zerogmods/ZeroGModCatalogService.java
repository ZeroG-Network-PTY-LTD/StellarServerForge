package com.zerog.stellarserverforge.zerogmods;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerog.stellarserverforge.net.HttpFetcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Fetches the user-maintained "mods ZeroG Network has made" catalog from a raw GitHub URL (or any
 * plain-JSON URL) they supply, and keeps a local last-known-good copy so the list is still usable
 * if the URL is briefly unreachable.
 */
public class ZeroGModCatalogService {

    private final HttpFetcher http = new HttpFetcher();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path cacheFile;

    public ZeroGModCatalogService(Path cacheDir) {
        this.cacheFile = cacheDir.resolve("zerog-mods-catalog.json");
    }

    public List<ZeroGModEntry> fetch(String url) throws IOException, InterruptedException {
        if (url == null || url.isBlank()) {
            throw new IOException("No catalog URL is set — add the raw GitHub URL to your mods catalog first.");
        }
        String json = http.getString(url);
        List<ZeroGModEntry> entries = mapper.readValue(json, mapper.getTypeFactory()
                .constructCollectionType(List.class, ZeroGModEntry.class));
        Files.createDirectories(cacheFile.getParent());
        Files.writeString(cacheFile, json);
        return entries;
    }

    public boolean hasCachedCopy() {
        return Files.isRegularFile(cacheFile);
    }

    public List<ZeroGModEntry> loadCached() throws IOException {
        String json = Files.readString(cacheFile);
        return mapper.readValue(json, mapper.getTypeFactory()
                .constructCollectionType(List.class, ZeroGModEntry.class));
    }
}
