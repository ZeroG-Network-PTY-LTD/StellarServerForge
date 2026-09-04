package com.zerog.stellarserverforge.utility;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Deletes the installed server jar/modloader libraries plus cached/downloaded installer, Java,
 * and metadata files (spec §3.10) — forcing a full re-download/reinstall on the next launch. Never
 * touches the user's custom server files (mods, config, world saves, settings), but this is a real
 * "start over" operation, not a harmless cache clear — see the confirmation text in
 * {@code UtilitiesDialog} for exactly what gets removed.
 */
public class PurgeService {

    public void purge(Path serverDir, Path cacheDir) throws IOException {
        deleteMatching(serverDir, "*.jar");
        deleteRecursively(serverDir.resolve("libraries"));
        deleteRecursively(serverDir.resolve(".fabric"));

        deleteRecursively(cacheDir.resolve("installers"));
        deleteRecursively(cacheDir.resolve("java"));
        deleteRecursively(cacheDir.resolve("versions"));
        deleteMatching(cacheDir, "*.json");
        deleteMatching(cacheDir, "*.xml");
    }

    private void deleteMatching(Path dir, String glob) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        Path ownJar = com.zerog.stellarserverforge.util.OwnJar.path();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
            for (Path p : stream) {
                if (ownJar != null && p.toAbsolutePath().normalize().equals(ownJar)) {
                    continue;
                }
                Files.deleteIfExists(p);
            }
        }
    }

    private void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                            // Best-effort cleanup.
                        }
                    });
        }
    }
}
