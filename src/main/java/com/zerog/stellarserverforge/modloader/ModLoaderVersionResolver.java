package com.zerog.stellarserverforge.modloader;

import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Resolves modloader version choices against cached metadata: "newest published version" and
 * "does this custom version actually exist" (spec §6.2, §6.3).
 */
public class ModLoaderVersionResolver {

    private static final String NEOFORGE_1_20_1_HARDCODED = "47.1.106";

    /** Whether nothing at all matched for a NeoForge newest-version lookup (spec §6.3 MAVENISSUE). */
    public static class NoMatchingVersionException extends Exception {
        public NoMatchingVersionException(String message) {
            super(message);
        }
    }

    public Optional<String> resolveNewest(ModLoader loader, McVersion mc, Path metadataFile, Path promotionsFileOrNull)
            throws IOException, NoMatchingVersionException {
        return switch (loader) {
            case FABRIC, QUILT -> Optional.ofNullable(MavenMetadata.parse(metadataFile).release());
            case FORGE -> promotionsFileOrNull == null
                    ? Optional.empty()
                    : ForgePromotions.latestFor(promotionsFileOrNull, mc.raw());
            case NEOFORGE -> resolveNeoForgeNewest(mc, metadataFile);
            case VANILLA -> throw new IllegalArgumentException("Vanilla has no modloader version");
        };
    }

    public boolean isValidVersion(ModLoader loader, McVersion mc, Path metadataFile, String candidate) throws IOException {
        MavenMetadata metadata = MavenMetadata.parse(metadataFile);
        return switch (loader) {
            case FABRIC, QUILT -> metadata.contains(candidate);
            case FORGE -> metadata.contains(mc.raw() + "-" + candidate);
            case NEOFORGE -> "1.20.1".equals(mc.raw())
                    ? metadata.contains(mc.raw() + "-" + candidate)
                    : metadata.contains(candidate);
            case VANILLA -> throw new IllegalArgumentException("Vanilla has no modloader version");
        };
    }

    /** Forge versions must be purely numeric (dots/hyphens allowed) — NeoForge is exempt since it has -beta suffixes. */
    public static boolean containsLetters(String version) {
        for (char c : version.toCharArray()) {
            if (Character.isLetter(c)) {
                return true;
            }
        }
        return false;
    }

    private Optional<String> resolveNeoForgeNewest(McVersion mc, Path metadataFile)
            throws IOException, NoMatchingVersionException {
        if ("1.20.1".equals(mc.raw())) {
            return Optional.of(NEOFORGE_1_20_1_HARDCODED);
        }

        List<String> versions = MavenMetadata.parse(metadataFile).versions();
        String newest = null;
        int bestTrackedNumber = -1;

        for (String versionStr : versions) {
            String[] parts = versionStr.split("[.\\-]", 5);
            if (parts.length < 3) {
                continue;
            }
            Integer major = parseIntOrNull(parts[0]);
            Integer minor = parseIntOrNull(parts[1]);
            if (major == null || minor == null || major != mc.major() || minor != mc.minor()) {
                continue;
            }

            if (mc.major() < 26) {
                Integer patch = parseIntOrNull(parts[2]);
                if (patch != null && patch > bestTrackedNumber) {
                    bestTrackedNumber = patch;
                    newest = versionStr;
                }
            } else {
                if (parts.length < 4) {
                    continue;
                }
                Integer hotfix = parseIntOrNull(parts[2]);
                if (hotfix == null || hotfix != mc.hotfix()) {
                    continue;
                }
                Integer build = parseIntOrNull(parts[3]);
                if (build != null && build > bestTrackedNumber) {
                    bestTrackedNumber = build;
                    newest = versionStr;
                }
            }
        }

        if (newest == null) {
            throw new NoMatchingVersionException(
                    "No NeoForge versions were found in the maven metadata for Minecraft " + mc.raw());
        }
        return Optional.of(newest);
    }

    private static Integer parseIntOrNull(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
