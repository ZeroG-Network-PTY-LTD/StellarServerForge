package com.zerog.stellarserverforge.utility;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds a distributable server-pack ZIP (spec §3.4/§13.2) using {@code ZipOutputStream} —
 * replacing PowerShell's {@code Compress-Archive}.
 */
public class ServerPackZipService {

    private static final List<String> WHITELIST_CANDIDATES = List.of(
            "config", "defaultconfigs", "kubejs", "mods", "scripts",
            "server-icon.png", "server.properties", "settings.json");

    private static final Set<String> EXCLUDED_NAMES = Set.of(".stellarforge-cache", ".fabric", "libraries", "logs");

    public List<String> defaultCandidates(Path serverDir) {
        List<String> found = new ArrayList<>();
        for (String candidate : WHITELIST_CANDIDATES) {
            if (Files.exists(serverDir.resolve(candidate))) {
                found.add(candidate);
            }
        }
        return found;
    }

    public boolean isExcluded(String name) {
        return EXCLUDED_NAMES.contains(name) || name.endsWith(".jar");
    }

    public Path createZip(Path serverDir, List<String> entryNames, String zipBaseName) throws IOException {
        Path serverDirNormalized = serverDir.normalize();
        Path zipPath = serverDirNormalized.resolve(sanitizeFileName(zipBaseName) + ".zip").normalize();
        if (!zipPath.startsWith(serverDirNormalized)) {
            // Defense in depth on top of the sanitizer below — a free-text UI field should never
            // be able to make this write outside the server directory.
            throw new IOException("Invalid file name.");
        }
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            String readme = "This is a StellarServerForge server pack.\n\n"
                    + "To run it: install StellarServerForge and point it at this folder, or run the "
                    + "server jar directly with the settings recorded in settings.json.\n";
            zos.putNextEntry(new ZipEntry("readme-server.txt"));
            zos.write(readme.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            for (String name : entryNames) {
                Path source = serverDir.resolve(name);
                if (!Files.exists(source)) {
                    continue;
                }
                if (Files.isDirectory(source)) {
                    addDirectory(zos, serverDir, source);
                } else {
                    addFile(zos, serverDir, source);
                }
            }
        }
        return zipPath;
    }

    /** Reduces a free-text UI field to a safe single filename component — no path separators, no
     * {@code ..} traversal, no characters Windows/most filesystems reject in a filename. */
    private static String sanitizeFileName(String name) {
        if (name == null) {
            return "server-pack";
        }
        String base = name.trim().replace('\\', '/');
        int lastSlash = base.lastIndexOf('/');
        if (lastSlash >= 0) {
            base = base.substring(lastSlash + 1);
        }
        base = base.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("^\\.+", "");
        return base.isBlank() ? "server-pack" : base;
    }

    private void addDirectory(ZipOutputStream zos, Path root, Path dir) throws IOException {
        try (var walk = Files.walk(dir)) {
            for (Path path : (Iterable<Path>) walk::iterator) {
                if (Files.isDirectory(path)) {
                    continue;
                }
                addFile(zos, root, path);
            }
        }
    }

    private void addFile(ZipOutputStream zos, Path root, Path file) throws IOException {
        String entryName = root.relativize(file).toString().replace('\\', '/');
        zos.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, zos);
        zos.closeEntry();
    }
}
