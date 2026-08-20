package com.zerog.stellarserverforge.mods;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerog.stellarserverforge.model.ModLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Detects client-only mods for Fabric/Quilt (spec §10.1), including the "kept because another
 * mod depends on it" case, which the original bat script silently omitted from its report — here
 * it's surfaced explicitly via {@link ScanResult#keptAsDependency()}.
 */
public class FabricQuiltModScanner {

    /** Known-mismarked mod IDs force-flagged as client regardless of declared environment. */
    private static final Set<String> FORCED_CLIENT_IDS = Set.of(
            "e4mc_minecraft", "moremcmeta_emissive_plugin", "mainhandswitch", "mobility", "notifyme",
            "removewardeneffect", "sparkle", "vs-wakes-compat", "wakes", "zoomify");

    public record ScanResult(List<ModEntry> removable, List<ModEntry> keptAsDependency) {
    }

    private record Candidate(String modId, String fileName, boolean clientFlagged, Set<String> depends) {
    }

    public ScanResult scan(Path modsDir, ModLoader loader) throws IOException {
        List<Candidate> all = new ArrayList<>();

        if (Files.isDirectory(modsDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar")) {
                for (Path jar : stream) {
                    readCandidate(jar, loader).ifPresent(all::add);
                }
            }
        }

        Set<String> allDependencyIds = new HashSet<>();
        for (Candidate c : all) {
            allDependencyIds.addAll(c.depends());
        }

        List<ModEntry> removable = new ArrayList<>();
        List<ModEntry> kept = new ArrayList<>();
        for (Candidate c : all) {
            if (!c.clientFlagged()) {
                continue;
            }
            ModEntry entry = new ModEntry(c.modId(), c.fileName());
            if (allDependencyIds.contains(c.modId())) {
                kept.add(entry);
            } else {
                removable.add(entry);
            }
        }
        return new ScanResult(removable, kept);
    }

    private java.util.Optional<Candidate> readCandidate(Path jar, ModLoader loader) {
        String fileName = jar.getFileName().toString();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = loader == ModLoader.QUILT
                    ? firstPresent(zip, "quilt.mod.json", "fabric.mod.json")
                    : zip.getEntry("fabric.mod.json");
            if (entry == null) {
                return java.util.Optional.empty();
            }

            try (InputStream in = zip.getInputStream(entry)) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(in);
                String modId = root.path("id").asText(null);
                if (modId == null) {
                    return java.util.Optional.empty();
                }
                String environment = root.path("environment").asText("*");
                boolean clientFlagged = "client".equals(environment) || FORCED_CLIENT_IDS.contains(modId);

                Set<String> depends = new HashSet<>();
                JsonNode dependsNode = root.path("depends");
                if (dependsNode.isObject()) {
                    dependsNode.fieldNames().forEachRemaining(depends::add);
                }

                return java.util.Optional.of(new Candidate(modId, fileName, clientFlagged, depends));
            }
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    private ZipEntry firstPresent(ZipFile zip, String... names) {
        for (String name : names) {
            ZipEntry entry = zip.getEntry(name);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }
}
