package com.zerog.stellarserverforge.curseforge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * CurseForge profile detection and import (spec §11). Unlike the original bat script, field
 * validation after parsing {@code minecraftinstance.json} is actually enforced here — the bat
 * script's equivalent check was dead code, bypassed by an unconditional {@code goto}.
 */
public class CurseForgeImportService {

    private static final Set<String> STALE_ITEMS_TO_CLEAR = Set.of(
            "blueprints", "config", "crash-reports", "datapacks", "defaultconfigs", "kubejs", "local", "logs",
            "modernfix", "mods", "patchouli_books", "resourcepacks", "schematics", "scripts", "screenshots",
            "shaderpacks", "xaero", "usercache.json", "usernamecache.json", "server-icon.png");

    private static final Set<String> EXCLUDED_FROM_COPY = Set.of(
            ".curseclient", "minecraftinstance.json", "crash-reports", "logs", "fancymenu_data", "natives",
            "saves", "screenshots", "shaderpacks");

    public record ProfileInfo(String folderName, Path path) {
    }

    public record ParsedProfile(String displayName, String minecraftVersion, String modLoaderName, String modLoaderVersion) {
    }

    /** Windows-only: reads the CurseForge app's minecraft_root registry key. Returns null if unavailable. */
    public Path findCurseForgeRoot() {
        if (!isWindows()) {
            return null;
        }
        try {
            String value = Advapi32Util.registryGetStringValue(
                    WinReg.HKEY_CURRENT_USER, "Software\\Overwolf\\Curseforge", "minecraft_root");
            if (value == null || value.isBlank()) {
                return null;
            }
            Path path = Path.of(value);
            return Files.isDirectory(path) ? path : null;
        } catch (Exception e) {
            return null;
        }
    }

    public List<ProfileInfo> listProfiles(Path curseForgeRoot) throws IOException {
        List<ProfileInfo> profiles = new ArrayList<>();
        Path instancesDir = curseForgeRoot.resolve("Instances");
        if (!Files.isDirectory(instancesDir)) {
            return profiles;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(instancesDir)) {
            for (Path dir : stream) {
                if (Files.isDirectory(dir) && Files.isRegularFile(dir.resolve("minecraftinstance.json"))) {
                    profiles.add(new ProfileInfo(dir.getFileName().toString(), dir));
                }
            }
        }
        return profiles;
    }

    public ParsedProfile parseProfile(Path instanceFolder) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(instanceFolder.resolve("minecraftinstance.json").toFile());

        String displayName = root.path("name").asText(null);
        String minecraftVersion = root.path("gameVersion").asText(null);
        String modLoaderVersion = root.path("baseModLoader").path("forgeVersion").asText(null);
        String rawModLoaderName = root.path("baseModLoader").path("name").asText(null);
        String modLoaderName = rawModLoaderName != null ? rawModLoaderName.split("-", 2)[0] : null;

        if (displayName == null || minecraftVersion == null || modLoaderVersion == null || modLoaderName == null
                || displayName.isBlank() || minecraftVersion.isBlank() || modLoaderVersion.isBlank() || modLoaderName.isBlank()) {
            throw new IOException("This CurseForge profile's minecraftinstance.json is missing required fields "
                    + "(name/gameVersion/baseModLoader) — it may be corrupted or from an unsupported CurseForge version.");
        }

        return new ParsedProfile(displayName, minecraftVersion, modLoaderName, modLoaderVersion);
    }

    /** Clears stale modpack leftovers, then copies the instance's files into the server directory. */
    public void importInto(Path instanceFolder, Path serverDir) throws IOException {
        for (String item : STALE_ITEMS_TO_CLEAR) {
            deleteRecursively(serverDir.resolve(item));
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(instanceFolder)) {
            for (Path source : stream) {
                String name = source.getFileName().toString();
                if (EXCLUDED_FROM_COPY.contains(name)) {
                    continue;
                }
                Path target = serverDir.resolve(name);
                if (Files.isDirectory(source)) {
                    copyRecursively(source, target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void copyRecursively(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (var walk = Files.walk(source)) {
            for (Path path : (Iterable<Path>) walk::iterator) {
                Path relative = source.relativize(path);
                Path dest = target.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        if (Files.isDirectory(path)) {
            try (var walk = Files.walk(path)) {
                walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                                // Best-effort cleanup.
                            }
                        });
            }
        } else {
            Files.deleteIfExists(path);
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
