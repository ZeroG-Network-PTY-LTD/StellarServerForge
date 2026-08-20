package com.zerog.stellarserverforge.mods;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Searches jar contents for MCreator's class package signatures (spec §10.2). Informational
 * only — MCreator mods are commonly poorly optimized and a frequent source of server issues.
 */
public final class McreatorScanner {

    private McreatorScanner() {
    }

    public static List<String> scan(Path modsDir) throws IOException {
        List<String> flagged = new ArrayList<>();
        if (!Files.isDirectory(modsDir)) {
            return flagged;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar")) {
            for (Path jar : stream) {
                try (ZipFile zip = new ZipFile(jar.toFile())) {
                    boolean isMcreator = zip.stream().anyMatch(McreatorScanner::matchesSignature);
                    if (isMcreator) {
                        flagged.add(jar.getFileName().toString());
                    }
                } catch (IOException ignored) {
                    // Unreadable/corrupt jar — skip it.
                }
            }
        }
        flagged.sort(String::compareTo);
        return flagged;
    }

    private static boolean matchesSignature(ZipEntry entry) {
        String name = entry.getName();
        return name.contains("net/mcreator") || name.contains("/procedures/");
    }
}
