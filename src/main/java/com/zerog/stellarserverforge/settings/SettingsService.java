package com.zerog.stellarserverforge.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zerog.stellarserverforge.model.ServerSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads/saves {@code settings.json} in the server working directory — a plain-data equivalent of
 * the original {@code settings-universalator.txt}, which was (unsafely) a literal batch-executable
 * fragment. This format is inert data only.
 */
public class SettingsService {

    public static final String SETTINGS_FILE_NAME = "settings.json";

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private final Path serverDir;

    public SettingsService(Path serverDir) {
        this.serverDir = serverDir;
    }

    public Path settingsPath() {
        return serverDir.resolve(SETTINGS_FILE_NAME);
    }

    public boolean exists() {
        return Files.exists(settingsPath());
    }

    public ServerSettings load() throws IOException {
        return mapper.readValue(settingsPath().toFile(), ServerSettings.class);
    }

    public void save(ServerSettings settings) throws IOException {
        mapper.writeValue(settingsPath().toFile(), settings);
    }
}
