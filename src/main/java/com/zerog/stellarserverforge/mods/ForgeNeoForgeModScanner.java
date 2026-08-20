package com.zerog.stellarserverforge.mods;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Detects client-only mods for Forge/NeoForge (spec §10.1). Reads jar metadata directly via
 * {@link ZipFile} rather than shelling out to {@code tar xOf} + PowerShell TOML/JSON parsing —
 * and always uses a real TOML parser (no optional-module/regex-fallback split).
 */
public class ForgeNeoForgeModScanner {

    public record ScanResult(List<ModEntry> flaggedClientMods, List<String> essentialMods) {
    }

    public ScanResult scan(Path modsDir, int mcMajor, Set<String> clientOnlyIds) throws IOException {
        List<ModEntry> flagged = new ArrayList<>();
        List<String> essential = new ArrayList<>();

        if (!Files.isDirectory(modsDir)) {
            return new ScanResult(flagged, essential);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar")) {
            for (Path jar : stream) {
                String fileName = jar.getFileName().toString();
                try (ZipFile zip = new ZipFile(jar.toFile())) {
                    if (fileName.toLowerCase().contains("essential") && hasEntry(zip, "essential-loader.properties")) {
                        essential.add(fileName);
                        continue;
                    }

                    if (mcMajor > 12) {
                        scanModernJar(zip, fileName, clientOnlyIds, flagged);
                    } else {
                        scanLegacyJar(zip, fileName, clientOnlyIds, flagged);
                    }
                } catch (IOException ignored) {
                    // Unreadable/corrupt jar — skip it rather than failing the whole scan.
                }
            }
        }
        return new ScanResult(flagged, essential);
    }

    private void scanModernJar(ZipFile zip, String fileName, Set<String> clientOnlyIds, List<ModEntry> flagged) {
        for (String candidate : List.of("META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
            ZipEntry entry = zip.getEntry(candidate);
            if (entry == null) {
                continue;
            }
            try (InputStream in = zip.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                UnmodifiableConfig config = new TomlParser().parse(reader);
                List<UnmodifiableConfig> mods = config.getOrElse("mods", List.of());
                for (UnmodifiableConfig mod : mods) {
                    String modId = mod.get("modId");
                    Boolean clientSideOnly = mod.getOrElse("clientSideOnly", false);
                    if (modId == null) {
                        continue;
                    }
                    boolean flaggedByList = clientOnlyIds.contains(modId);
                    if (Boolean.TRUE.equals(clientSideOnly) || flaggedByList) {
                        flagged.add(new ModEntry(modId, fileName));
                    }
                }
            } catch (Exception ignored) {
                // Malformed TOML — skip this jar's metadata.
            }
            return;
        }
    }

    private void scanLegacyJar(ZipFile zip, String fileName, Set<String> clientOnlyIds, List<ModEntry> flagged) {
        ZipEntry entry = zip.getEntry("mcmod.info");
        if (entry == null) {
            return;
        }
        try (InputStream in = zip.getInputStream(entry)) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(in);
            JsonNode modNode = root.isArray() ? (root.size() > 0 ? root.get(0) : null) : root;
            if (modNode == null) {
                return;
            }
            String modId = modNode.path("modid").asText(null);
            if (modId != null && clientOnlyIds.contains(modId)) {
                flagged.add(new ModEntry(modId, fileName));
            }
        } catch (Exception ignored) {
            // Malformed/absent mcmod.info — this is expected to reduce detection accuracy for MC<=12.
        }
    }

    private boolean hasEntry(ZipFile zip, String name) {
        return zip.stream().anyMatch(e -> e.getName().endsWith(name));
    }
}
