package com.zerog.network.stellarforge.utils;

import com.zerog.network.stellarforge.model.ServerMetrics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw Minecraft server console output.
 * - Classifies lines as INFO / WARN / ERROR / DEBUG
 * - Extracts TPS, memory, player counts from log patterns
 */
public class LogParser {

    public enum LogLevel { DEBUG, INFO, WARN, ERROR, SYSTEM }

    // ── Patterns ──────────────────────────────────────────────────────────────

    // [HH:MM:SS] [Server thread/INFO]: text
    private static final Pattern VANILLA_LOG =
            Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\] \\[([^/]+)/([A-Z]+)\\]: (.*)$");

    // [INFO] text  (some loaders)
    private static final Pattern BARE_LEVEL =
            Pattern.compile("^\\[([A-Z]+)\\] (.*)$");

    // TPS from Essentials: TPS from last 1m, 5m, 15m: 19.93, 19.93, 19.93
    private static final Pattern TPS_PATTERN =
            Pattern.compile("TPS from last 1m, 5m, 15m: ([\\d.]+),\\s*([\\d.]+),\\s*([\\d.]+)");

    // Memory: Used: 1024 MB / 4096 MB
    private static final Pattern MEM_PATTERN =
            Pattern.compile("Used:\\s*(\\d+)\\s*MB\\s*/\\s*(\\d+)\\s*MB");

    // Done (X.XXXs)!
    private static final Pattern DONE_PATTERN =
            Pattern.compile("Done \\(([\\d.]+)s\\)!");

    // Player joined: UUID player joined the game
    private static final Pattern JOINED_PATTERN =
            Pattern.compile("(\\S+) joined the game");

    // Player left: UUID player left the game
    private static final Pattern LEFT_PATTERN =
            Pattern.compile("(\\S+) left the game");

    // Players online: There are X of a max of Y players online
    private static final Pattern PLAYERS_PATTERN =
            Pattern.compile("There are (\\d+) of a max of (\\d+) players online");

    // ── Public API ────────────────────────────────────────────────────────────

    public static LogLevel classifyLine(String line) {
        if (line == null || line.isEmpty()) return LogLevel.INFO;

        Matcher m = VANILLA_LOG.matcher(line);
        if (m.matches()) {
            return parseLevel(m.group(2));
        }
        Matcher m2 = BARE_LEVEL.matcher(line);
        if (m2.matches()) {
            return parseLevel(m2.group(1));
        }

        // Heuristics for unformatted lines
        String upper = line.toUpperCase();
        if (upper.contains("ERROR") || upper.contains("EXCEPTION")
                || upper.contains("FATAL") || upper.contains("CAUSED BY")) {
            return LogLevel.ERROR;
        }
        if (upper.contains("WARN")) return LogLevel.WARN;
        return LogLevel.INFO;
    }

    /** Extract the plain message body from a log line (strip prefix) */
    public static String extractMessage(String line) {
        Matcher m = VANILLA_LOG.matcher(line);
        if (m.matches()) return m.group(3);
        Matcher m2 = BARE_LEVEL.matcher(line);
        if (m2.matches()) return m2.group(2);
        return line;
    }

    /** Return true if this line contains the server "Done" signal */
    public static boolean isStartupComplete(String line) {
        return DONE_PATTERN.matcher(line).find();
    }

    /** Try to parse TPS data from this line; returns null if not a TPS line */
    public static double[] parseTps(String line) {
        Matcher m = TPS_PATTERN.matcher(line);
        if (m.find()) {
            try {
                return new double[]{
                    Double.parseDouble(m.group(1)),
                    Double.parseDouble(m.group(2)),
                    Double.parseDouble(m.group(3))
                };
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /** Try to parse memory data; returns [usedMb, maxMb] or null */
    public static long[] parseMemory(String line) {
        Matcher m = MEM_PATTERN.matcher(line);
        if (m.find()) {
            try {
                return new long[]{Long.parseLong(m.group(1)), Long.parseLong(m.group(2))};
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /** Returns the name of a player who joined, or null */
    public static String parsePlayerJoined(String line) {
        Matcher m = JOINED_PATTERN.matcher(extractMessage(line));
        return m.find() ? m.group(1) : null;
    }

    /** Returns the name of a player who left, or null */
    public static String parsePlayerLeft(String line) {
        Matcher m = LEFT_PATTERN.matcher(extractMessage(line));
        return m.find() ? m.group(1) : null;
    }

    /** Returns [onlinePlayers, maxPlayers] from a /list response, or null */
    public static int[] parsePlayers(String line) {
        Matcher m = PLAYERS_PATTERN.matcher(line);
        if (m.find()) {
            try {
                return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static LogLevel parseLevel(String s) {
        if (s == null) return LogLevel.INFO;
        switch (s.toUpperCase()) {
            case "ERROR": case "FATAL": return LogLevel.ERROR;
            case "WARN":  case "WARNING": return LogLevel.WARN;
            case "DEBUG": case "TRACE":   return LogLevel.DEBUG;
            default:                      return LogLevel.INFO;
        }
    }
}
