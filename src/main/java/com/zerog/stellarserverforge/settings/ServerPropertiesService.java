package com.zerog.stellarserverforge.settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Manages {@code server.properties}: creates sane defaults if missing, and repairs a few fields
 * on every startup, mirroring {@code check_server_properties}/{@code check_port_settings} from the
 * original spec (§2.3, §12.2).
 */
public class ServerPropertiesService {

    private static final Set<String> VALID_DIFFICULTIES = Set.of("peaceful", "easy", "normal", "hard");

    private final Path serverDir;

    public ServerPropertiesService(Path serverDir) {
        this.serverDir = serverDir;
    }

    public Path propertiesPath() {
        return serverDir.resolve("server.properties");
    }

    /** Result of a repair pass, surfaced to the GUI so it can inform the user. */
    public record RepairResult(boolean created, boolean serverIpNonBlank, String serverIpValue) {
    }

    public RepairResult ensureValidAndSynced(int desiredPort) throws IOException {
        Path path = propertiesPath();
        boolean created = false;

        if (!Files.exists(path)) {
            ServerPropertiesFile fresh = new ServerPropertiesFile();
            fresh.set("allow-flight", "true");
            fresh.set("online-mode", "true");
            fresh.set("server-port", String.valueOf(desiredPort));
            fresh.set("server-ip", "");
            fresh.set("level-name", "world");
            fresh.set("motd", "A Minecraft Server");
            fresh.set("view-distance", "10");
            fresh.set("max-build-height", "256");
            fresh.set("spawn-npcs", "true");
            fresh.set("spawn-animals", "true");
            fresh.set("difficulty", "normal");
            fresh.save(path);
            return new RepairResult(true, false, "");
        }

        ServerPropertiesFile props = ServerPropertiesFile.load(path);

        if ("false".equals(props.get("allow-flight"))) {
            props.set("allow-flight", "true");
        }
        if ("false".equals(props.get("online-mode"))) {
            props.set("online-mode", "true");
        }

        String difficulty = props.get("difficulty");
        if (difficulty != null && !VALID_DIFFICULTIES.contains(difficulty)) {
            props.set("difficulty", "normal");
        }

        String serverIp = props.get("server-ip");
        boolean serverIpNonBlank = serverIp != null && !serverIp.isBlank();

        String currentPort = props.get("server-port");
        if (currentPort == null || !currentPort.equals(String.valueOf(desiredPort))) {
            props.set("server-port", String.valueOf(desiredPort));
        }

        props.save(path);
        return new RepairResult(created, serverIpNonBlank, serverIp == null ? "" : serverIp);
    }

    /** Blanks the server-ip field — used when the user accepts the "clear custom domain" prompt. */
    public void clearServerIp() throws IOException {
        Path path = propertiesPath();
        ServerPropertiesFile props = ServerPropertiesFile.load(path);
        props.set("server-ip", "");
        props.save(path);
    }
}
