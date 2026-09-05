package com.zerog.stellarserverforge.modrinth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.net.ChecksumUtil;
import com.zerog.stellarserverforge.net.HttpFetcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Imports a Modrinth modpack export ({@code .mrpack} — a zip containing {@code
 * modrinth.index.json} plus an {@code overrides/} folder), mirroring what {@link
 * com.zerog.stellarserverforge.curseforge.CurseForgeImportService} does for a local CurseForge
 * app instance, but for the portable single-file format Modrinth actually publishes — there's no
 * local CurseForge-style "installed instance" to scan for Modrinth, so a downloaded {@code
 * .mrpack} file is the natural equivalent entry point.
 */
public class ModrinthModpackImportService {

    /** {@code modrinth.index.json}'s {@code dependencies} object uses one of these keys to name
     * the modloader; order matters only in that a manifest should have exactly one of them. */
    private static final Map<String, ModLoader> LOADER_DEPENDENCY_KEYS = new LinkedHashMap<>();

    static {
        LOADER_DEPENDENCY_KEYS.put("forge", ModLoader.FORGE);
        LOADER_DEPENDENCY_KEYS.put("neoforge", ModLoader.NEOFORGE);
        LOADER_DEPENDENCY_KEYS.put("fabric-loader", ModLoader.FABRIC);
        LOADER_DEPENDENCY_KEYS.put("quilt-loader", ModLoader.QUILT);
    }

    public record ManifestFile(String path, String downloadUrl, String sha1) {
    }

    public record ParsedModpack(String name, String minecraftVersion, ModLoader modLoader,
                                 String modLoaderVersion, List<ManifestFile> serverFiles) {
    }

    private final HttpFetcher http = new HttpFetcher();
    private final ObjectMapper mapper = new ObjectMapper();

    /** Reads {@code modrinth.index.json} out of the {@code .mrpack} and validates it — does not
     * download or write anything yet, so a bad file can be rejected before anything happens. */
    public ParsedModpack parse(Path mrpackFile) throws IOException {
        try (ZipFile zip = new ZipFile(mrpackFile.toFile())) {
            ZipEntry indexEntry = zip.getEntry("modrinth.index.json");
            if (indexEntry == null) {
                throw new IOException(mrpackFile.getFileName() + " is not a valid .mrpack file — "
                        + "missing modrinth.index.json.");
            }
            JsonNode root;
            try (InputStream in = zip.getInputStream(indexEntry)) {
                root = mapper.readTree(in);
            }

            String name = root.path("name").asText(null);
            JsonNode dependencies = root.path("dependencies");
            String minecraftVersion = dependencies.path("minecraft").asText(null);

            ModLoader modLoader = null;
            String modLoaderVersion = null;
            for (Map.Entry<String, ModLoader> candidate : LOADER_DEPENDENCY_KEYS.entrySet()) {
                JsonNode value = dependencies.path(candidate.getKey());
                if (!value.isMissingNode() && !value.asText("").isBlank()) {
                    modLoader = candidate.getValue();
                    modLoaderVersion = value.asText();
                    break;
                }
            }

            if (name == null || name.isBlank() || minecraftVersion == null || minecraftVersion.isBlank()
                    || modLoader == null || modLoaderVersion == null) {
                throw new IOException("This .mrpack is missing required fields (name/minecraft version/modloader) "
                        + "in modrinth.index.json — it may be corrupted or from an unsupported format version.");
            }

            List<ManifestFile> serverFiles = new ArrayList<>();
            for (JsonNode fileNode : root.path("files")) {
                // A file explicitly marked "unsupported" for the server environment is client-only
                // (e.g. a resource pack or a client-side-only mod) — skip it for a dedicated server.
                String serverEnv = fileNode.path("env").path("server").asText("required");
                if ("unsupported".equalsIgnoreCase(serverEnv)) {
                    continue;
                }
                String path = fileNode.path("path").asText(null);
                String downloadUrl = firstDownloadUrl(fileNode.path("downloads"));
                if (path == null || path.isBlank() || downloadUrl == null) {
                    continue;
                }
                String sha1 = fileNode.path("hashes").path("sha1").asText(null);
                serverFiles.add(new ManifestFile(path, downloadUrl, sha1));
            }

            return new ParsedModpack(name, minecraftVersion, modLoader, modLoaderVersion, serverFiles);
        }
    }

    private static String firstDownloadUrl(JsonNode downloads) {
        return downloads.isArray() && !downloads.isEmpty() ? downloads.get(0).asText(null) : null;
    }

    /** Downloads every server-relevant file the manifest lists, then extracts {@code overrides/}
     * and (taking precedence) {@code server-overrides/} into the server directory. Reports
     * progress via {@code onProgress}, called after each file with (done, total). */
    public void importInto(Path mrpackFile, ParsedModpack parsed, Path serverDir,
                            java.util.function.BiConsumer<Integer, Integer> onProgress)
            throws IOException, InterruptedException {
        Files.createDirectories(serverDir);
        int total = parsed.serverFiles().size();
        int done = 0;
        for (ManifestFile file : parsed.serverFiles()) {
            Path dest = resolveWithinServerDir(serverDir, file.path());
            Files.createDirectories(dest.getParent());
            http.downloadToFile(file.downloadUrl(), dest);
            if (file.sha1() != null && !file.sha1().isBlank() && !ChecksumUtil.matches(dest, file.sha1(), "SHA-1")) {
                Files.deleteIfExists(dest);
                throw new IOException("Checksum mismatch downloading " + file.path() + " — aborted.");
            }
            done++;
            onProgress.accept(done, total);
        }

        try (ZipFile zip = new ZipFile(mrpackFile.toFile())) {
            extractPrefixed(zip, serverDir, "overrides/");
            extractPrefixed(zip, serverDir, "server-overrides/");
        }
    }

    private void extractPrefixed(ZipFile zip, Path serverDir, String prefix) throws IOException {
        var entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().startsWith(prefix)) {
                continue;
            }
            String relative = entry.getName().substring(prefix.length());
            if (relative.isBlank()) {
                continue;
            }
            Path dest = resolveWithinServerDir(serverDir, relative);
            Files.createDirectories(dest.getParent());
            try (InputStream in = zip.getInputStream(entry)) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /** Defense against a malicious/corrupt manifest or zip entry using {@code ../} to escape the
     * server directory (zip-slip) — every write target is checked before use. */
    private Path resolveWithinServerDir(Path serverDir, String relativePath) throws IOException {
        Path normalizedRoot = serverDir.normalize();
        Path dest = normalizedRoot.resolve(relativePath).normalize();
        if (!dest.startsWith(normalizedRoot)) {
            throw new IOException("Modpack referenced a path outside the server directory: " + relativePath);
        }
        return dest;
    }
}
