package com.zerog.network.stellarforge.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Proactively scans for known problems before or during server operation
 * and returns human-readable diagnostic messages.
 */
public class ProblemDetector {

    private static final Logger logger = LoggerFactory.getLogger(ProblemDetector.class);

    public enum Severity { INFO, WARNING, ERROR }

    public static class Problem {
        public final Severity severity;
        public final String title;
        public final String description;
        public final String suggestion;

        public Problem(Severity s, String title, String desc, String suggestion) {
            this.severity = s; this.title = title;
            this.description = desc; this.suggestion = suggestion;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Run a full pre-launch check for the given server directory and port.
     * @param serverPath   Path to the server folder
     * @param port         Server port to check
     * @param minRamGb     Minimum required RAM in GB
     * @return list of Problems found (empty = all clear)
     */
    public static List<Problem> checkPreLaunch(Path serverPath, int port, int minRamGb) {
        List<Problem> problems = new ArrayList<>();

        checkServerDirectory(serverPath, problems);
        checkPort(port, problems);
        checkJavaAvailable(problems);
        checkSystemMemory(minRamGb, problems);
        checkDiskSpace(serverPath, problems);

        return problems;
    }

    /** Check if any log content indicates a crash or critical error */
    public static List<Problem> analyzeLog(List<String> logLines) {
        List<Problem> problems = new ArrayList<>();

        for (String line : logLines) {
            String upper = line.toUpperCase();

            if (upper.contains("JAVA.LANG.OUTOFMEMORYERROR")) {
                problems.add(new Problem(Severity.ERROR, "Out of Memory",
                        "Server ran out of heap memory.",
                        "Increase RAM allocation or reduce view-distance / mod count."));
            }
            if (upper.contains("ADDRESS ALREADY IN USE")) {
                problems.add(new Problem(Severity.ERROR, "Port Conflict",
                        "The server port is already in use by another process.",
                        "Change the server port in Configuration > Network, or stop the conflicting process."));
            }
            if (upper.contains("FAILED TO BIND PORT")) {
                problems.add(new Problem(Severity.ERROR, "Cannot Bind Port",
                        "Server could not bind to the configured port.",
                        "Check firewall rules and ensure the port is not blocked."));
            }
            if (upper.contains("MIXIN") && upper.contains("ERROR")) {
                problems.add(new Problem(Severity.WARNING, "Mixin Error",
                        "A Mixin injection failed — usually caused by mod incompatibility.",
                        "Check for mod updates and remove recently added mods."));
            }
            if (upper.contains("INCOMPATIBLE MOD") || upper.contains("MOD CONFLICT")) {
                problems.add(new Problem(Severity.WARNING, "Mod Conflict",
                        "Two or more installed mods are incompatible.",
                        "Review the conflict list in the log and remove one of the conflicting mods."));
            }
        }

        return problems;
    }

    // ── Checks ────────────────────────────────────────────────────────────────

    private static void checkServerDirectory(Path serverPath, List<Problem> out) {
        if (serverPath == null) {
            out.add(new Problem(Severity.ERROR, "No Server Path",
                    "Server directory is not configured.",
                    "Open Configuration and set the server path."));
            return;
        }
        if (!Files.exists(serverPath)) {
            out.add(new Problem(Severity.WARNING, "Server Directory Missing",
                    "The server directory does not exist yet.",
                    "Click 'Setup Server' to initialize it."));
        } else {
            Path jar = serverPath.resolve("server.jar");
            if (!Files.exists(jar)) {
                out.add(new Problem(Severity.WARNING, "server.jar Not Found",
                        "No server.jar in " + serverPath,
                        "Run Setup Server to download it, or place server.jar manually."));
            }
        }
    }

    private static void checkPort(int port, List<Problem> out) {
        if (port < 1024 || port > 65535) {
            out.add(new Problem(Severity.ERROR, "Invalid Port",
                    "Port " + port + " is not in the valid range (1024–65535).",
                    "Change the port in Configuration > Network."));
            return;
        }
        try (ServerSocket ss = new ServerSocket(port, 0, InetAddress.getByName("0.0.0.0"))) {
            // Port is free — good
        } catch (IOException e) {
            out.add(new Problem(Severity.WARNING, "Port " + port + " In Use",
                    "Something is already listening on port " + port + ".",
                    "Change the server port or stop the conflicting service."));
        }
    }

    private static void checkJavaAvailable(List<Problem> out) {
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            out.add(new Problem(Severity.ERROR, "Java Not Found",
                    "java is not on the system PATH.",
                    "Install Java from https://adoptium.net/ or set a custom Java path."));
        }
    }

    private static void checkSystemMemory(int minRamGb, List<Problem> out) {
        long freeBytes = Runtime.getRuntime().freeMemory()
                + (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory());
        long freeGb = freeBytes / (1024L * 1024 * 1024);
        if (freeGb < minRamGb) {
            out.add(new Problem(Severity.WARNING, "Low Available Memory",
                    String.format("Only ~%d GB free but server needs %d GB.", freeGb, minRamGb),
                    "Close other applications or reduce the RAM allocation in Configuration."));
        }
    }

    private static void checkDiskSpace(Path serverPath, List<Problem> out) {
        try {
            Path check = (serverPath != null && Files.exists(serverPath))
                    ? serverPath : Paths.get(".");
            long freeGb = check.toFile().getFreeSpace() / (1024L * 1024 * 1024);
            if (freeGb < 2) {
                out.add(new Problem(Severity.WARNING, "Low Disk Space",
                        "Less than 2 GB free on the server drive.",
                        "Free up disk space to avoid world corruption."));
            }
        } catch (Exception e) {
            logger.warn("Disk check failed: {}", e.getMessage());
        }
    }
}
