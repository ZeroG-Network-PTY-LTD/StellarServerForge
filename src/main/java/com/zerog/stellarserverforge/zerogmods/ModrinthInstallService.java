package com.zerog.stellarserverforge.zerogmods;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.net.HttpFetcher;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
        JsonNode chosen = versions.get(0);
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
        String filename = file.path("filename").asText();
        Files.createDirectories(modsDir);
        Path dest = modsDir.resolve(filename);
        http.downloadToFile(downloadUrl, dest);
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
}
