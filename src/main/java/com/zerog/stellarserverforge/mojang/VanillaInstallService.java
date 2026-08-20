package com.zerog.stellarserverforge.mojang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerog.stellarserverforge.net.ChecksumUtil;
import com.zerog.stellarserverforge.net.HttpFetcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Downloads/verifies the vanilla server jar for a specific Minecraft version (spec §6.6).
 * Per-version metadata is cached indefinitely once fetched (unlike the top-level manifest, which
 * refreshes daily), since a given version's metadata never changes once published.
 */
public class VanillaInstallService {

    private final HttpFetcher http = new HttpFetcher();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path cacheDir;
    private final Path serverDir;

    public VanillaInstallService(Path cacheDir, Path serverDir) {
        this.cacheDir = cacheDir;
        this.serverDir = serverDir;
    }

    public Path serverJarPath(String minecraftVersion) {
        return serverDir.resolve("minecraft_server." + minecraftVersion + ".jar");
    }

    private Path perVersionJsonPath(String minecraftVersion) {
        return cacheDir.resolve("versions").resolve(minecraftVersion + ".json");
    }

    /**
     * Ensures the vanilla server jar for the given version exists and is checksum-valid,
     * downloading (and re-downloading on checksum mismatch, once) if necessary.
     */
    public Path ensureInstalled(MojangManifestService.VersionEntry version) throws IOException, InterruptedException {
        Path jarPath = serverJarPath(version.id());
        if (Files.exists(jarPath)) {
            return jarPath;
        }

        Path perVersionJson = perVersionJsonPath(version.id());
        Files.createDirectories(perVersionJson.getParent());
        if (!Files.exists(perVersionJson)) {
            String body = http.getString(version.url());
            Files.writeString(perVersionJson, body, StandardCharsets.UTF_8);
        }

        JsonNode root = mapper.readTree(perVersionJson.toFile());
        JsonNode server = root.path("downloads").path("server");
        String downloadUrl = server.path("url").asText();
        String expectedSha1 = server.path("sha1").asText();

        for (int attempt = 0; attempt < 2; attempt++) {
            http.downloadToFile(downloadUrl, jarPath);
            if (ChecksumUtil.matches(jarPath, expectedSha1, "SHA-1")) {
                return jarPath;
            }
            Files.deleteIfExists(jarPath);
        }
        throw new IOException("Vanilla server jar for " + version.id() + " failed SHA1 verification after retry");
    }
}
