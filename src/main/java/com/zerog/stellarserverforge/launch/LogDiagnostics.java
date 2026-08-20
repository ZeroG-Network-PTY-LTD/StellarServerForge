package com.zerog.stellarserverforge.launch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Scans {@code logs/latest.log} for known crash signatures and prints guidance, mirroring
 * {@code logsscan} (spec §12.5). Informational only — never modifies anything.
 */
public final class LogDiagnostics {

    private LogDiagnostics() {
    }

    public static boolean isGracefulStop(Path logFile) {
        try {
            if (!Files.isRegularFile(logFile)) {
                return false;
            }
            return Files.readString(logFile, StandardCharsets.UTF_8).contains("Stopping the server");
        } catch (IOException e) {
            return false;
        }
    }

    public static List<String> diagnose(Path logFile) {
        List<String> guidance = new ArrayList<>();
        if (!Files.isRegularFile(logFile)) {
            return guidance;
        }

        String content;
        try {
            content = Files.readString(logFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return guidance;
        }

        if (content.contains("Stopping the server")) {
            return guidance;
        }

        if (content.contains("Unsupported class file major version")) {
            guidance.add("This looks like a Java/modloader version mismatch (\"Unsupported class file major version\"). "
                    + "Double-check the Java version required for this Minecraft/modloader version.");
        }

        if (content.contains("invalid dist DEDICATED_SERVER") || content.contains("Loading errors encountered")) {
            List<String> failedMods = linesContaining(content, "has failed to load correctly");
            guidance.add("A client-only mod appears to be installed on the server ("
                    + "\"invalid dist DEDICATED_SERVER\"/\"Loading errors encountered\"). Remove client-only mods "
                    + "from the mods folder.");
            if (!failedMods.isEmpty()) {
                guidance.add("Mods that failed to load: " + String.join("; ", failedMods));
            }
        }

        if (content.contains("FAILED TO BIND TO PORT")) {
            guidance.add("The server could not bind to its port (\"FAILED TO BIND TO PORT\") — another process is "
                    + "likely already using it. Check the port setting or close the other process.");
        }

        if (content.contains("Missing or unsupported mandatory dependencies:")) {
            List<String> missingMods = linesContaining(content, "Mod ID:");
            guidance.add("Missing or mismatched mod dependencies were detected.");
            if (!missingMods.isEmpty()) {
                guidance.add("Affected: " + String.join("; ", missingMods));
            }
        }

        if (content.contains("Tried to read NBT tag with too high complexity, depth > 512")) {
            guidance.add("An NBT tag exceeded the maximum depth (\"depth > 512\") — a corrupted or malicious item/entity "
                    + "may be involved. Consider a \"Long NBT Killer\"-style mod to clean it up.");
        }

        if (guidance.isEmpty()) {
            guidance.add("The server may have crashed or stopped unexpectedly — check logs/latest.log for details.");
        }
        return guidance;
    }

    private static List<String> linesContaining(String content, String marker) {
        List<String> matches = new ArrayList<>();
        for (String line : content.split("\\R")) {
            if (line.contains(marker)) {
                matches.add(line.trim());
            }
        }
        return matches;
    }
}
