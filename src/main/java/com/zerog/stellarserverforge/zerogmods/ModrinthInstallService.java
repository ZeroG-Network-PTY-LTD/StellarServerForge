package com.zerog.stellarserverforge.zerogmods;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.net.ChecksumUtil;
import com.zerog.stellarserverforge.net.HttpFetcher;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

/** Resolves and downloads a ZeroG Network mod from Modrinth's public API (no auth required). */
public class ModrinthInstallService {

    private final HttpFetcher http = new HttpFetcher();
    private final ObjectMapper mapper = new ObjectMapper();

    public Path install(ZeroGModEntry entry, ModLoader loader, McVersion mc, Path modsDir)
            throws IOException, InterruptedException {
        String url = "https://api.modrinth.com/v2/project/" + urlEncode(entry.getProjectId())
                + "/version?loaders=" + urlEncode("[\"" + modrinthLoaderName(loader) + "\"]")
                + "&game_versions=" + urlEncode("[\"" + mc.raw() + "\"]");
        String json = http.getString(url);
        JsonNode versions = mapper.readTree(json);
        if (!versions.isArray() || versions.isEmpty()) {
            throw new IOException("No Modrinth version of \"" + entry.getName() + "\" is published for "
                    + mc.raw() + " (" + loader + ").");
        }

        // Don't rely on the API returning newest-first — sort explicitly by publish date.
        JsonNode chosen = StreamSupport.stream(Spliterators.spliteratorUnknownSize(versions.iterator(), 0), false)
                .max(Comparator.comparing(v -> {
                    try {
                        return Instant.parse(v.path("date_published").asText());
                    } catch (Exception e) {
                        return Instant.EPOCH;
                    }
                }))
                .orElse(versions.get(0));

        JsonNode files = chosen.path("files");
        JsonNode file = null;
        for (JsonNode f : files) {
            if (f.path("primary").asBoolean(false)) {
                file = f;
                break;
            }
        }
        if (file == null && !files.isEmpty()) {
            file = files.get(0);
        }
        if (file == null) {
            throw new IOException("Modrinth returned a version of \"" + entry.getName() + "\" with no downloadable files.");
        }

        String downloadUrl = file.path("url").asText();
        String filename = sanitizeFileName(file.path("filename").asText(), entry.getName());
        String expectedSha1 = file.path("hashes").path("sha1").asText(null);

        Files.createDirectories(modsDir);
        Path dest = modsDir.resolve(filename);
        http.downloadToFile(downloadUrl, dest);

        if (expectedSha1 != null && !expectedSha1.isBlank()) {
            if (!ChecksumUtil.matches(dest, expectedSha1, "SHA-1")) {
                Files.deleteIfExists(dest);
                throw new IOException("Downloaded file for \"" + entry.getName()
                        + "\" failed SHA-1 verification against Modrinth's published hash — deleted, not installed.");
            }
        }
        return dest;
    }

    private static String modrinthLoaderName(ModLoader loader) {
        return switch (loader) {
            case FORGE -> "forge";
            case NEOFORGE -> "neoforge";
            case FABRIC -> "fabric";
            case QUILT -> "quilt";
            case VANILLA -> throw new IllegalArgumentException("Vanilla servers can't install mods");
        };
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Modrinth's reported filename ends up straight in a {@code Path.resolve()} call — reduce it
     * to a bare file name so a malicious/compromised response can't escape {@code modsDir} via
     * {@code ../} segments or an absolute path. */
    private static String sanitizeFileName(String rawFilename, String modName) throws IOException {
        String name;
        try {
            name = Path.of(rawFilename).getFileName().toString();
        } catch (java.nio.file.InvalidPathException e) {
            throw new IOException("Modrinth returned an invalid file name for \"" + modName + "\".", e);
        }
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            throw new IOException("Modrinth returned an invalid file name for \"" + modName + "\".");
        }
        return name;
    }
}
