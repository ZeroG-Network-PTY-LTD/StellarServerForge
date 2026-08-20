package com.zerog.stellarserverforge.utility;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Deletes cached/downloaded modloader and utility files (spec §3.10) — never touches the user's
 * custom server files (mods, config, world saves, settings).
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
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
            for (Path p : stream) {
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
