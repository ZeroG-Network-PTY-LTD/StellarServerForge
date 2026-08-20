package com.zerog.stellarserverforge.settings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Manages {@code eula.txt} — mirrors {@code :eula} from the spec (§12.1).
 */
public class EulaService {

    public static final String MOJANG_EULA_URL = "https://account.mojang.com/documents/minecraft_eula";

    private final Path serverDir;

    public EulaService(Path serverDir) {
        this.serverDir = serverDir;
    }

    public Path eulaPath() {
        return serverDir.resolve("eula.txt");
    }

    public boolean isAccepted() throws IOException {
        Path path = eulaPath();
        if (!Files.exists(path)) {
            return false;
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (String line : lines) {
            String normalized = line.replace('=', '#');
            if (normalized.contains("eula#") && !normalized.contains("eula#true")) {
                return false;
            }
        }
        // Also require at least one explicit eula=true line to be present.
        return lines.stream().anyMatch(l -> l.trim().equals("eula=true"));
    }

    public void accept() throws IOException {
        Files.writeString(eulaPath(), "eula=true" + System.lineSeparator(), StandardCharsets.UTF_8);
    }
}
