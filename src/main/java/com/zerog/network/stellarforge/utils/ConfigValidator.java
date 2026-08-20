package com.zerog.network.stellarforge.utils;

import com.zerog.network.stellarforge.model.ServerConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates ServerConfig fields and returns structured results.
 */
public class ConfigValidator {

    public static ValidationResult validate(ServerConfig cfg) {
        ValidationResult result = new ValidationResult();
        if (cfg.getServerName() == null || cfg.getServerName().trim().isEmpty())
            result.addError("serverName", "Server name is required");
        else if (cfg.getServerName().trim().length() < 3)
            result.addWarning("serverName", "Server name is very short");

        if (cfg.getServerPath() == null || cfg.getServerPath().trim().isEmpty())
            result.addError("serverPath", "Server path is required");
        else if (!isValidPath(cfg.getServerPath()))
            result.addError("serverPath", "Path contains invalid characters");

        if (cfg.getMaxRamGb() < 1)
            result.addError("maxRamGb", "RAM must be at least 1 GB");
        else if (cfg.getMaxRamGb() < 2)
            result.addWarning("maxRamGb", "< 2 GB may cause performance issues");

        if (cfg.getPort() < 1024 || cfg.getPort() > 65535)
            result.addError("port", "Port must be 1024-65535");

        if (cfg.getMaxPlayers() < 1)
            result.addError("maxPlayers", "Max players must be >= 1");

        return result;
    }

    public static FieldStatus validateServerName(String name) {
        if (name == null || name.trim().isEmpty()) return FieldStatus.error("Name is required");
        if (name.trim().length() < 3) return FieldStatus.warning("Name should be >= 3 chars");
        return FieldStatus.ok();
    }

    public static FieldStatus validateServerPath(String path) {
        if (path == null || path.trim().isEmpty()) return FieldStatus.error("Path is required");
        if (!isValidPath(path)) return FieldStatus.error("Path contains invalid characters");
        File f = new File(path);
        if (f.exists() && !f.isDirectory()) return FieldStatus.error("Path exists but is not a directory");
        return FieldStatus.ok();
    }

    public static FieldStatus validatePort(int port) {
        if (port < 1024 || port > 65535) return FieldStatus.error("Must be 1024-65535");
        return FieldStatus.ok();
    }

    public static FieldStatus validateRam(int gb) {
        if (gb < 1) return FieldStatus.error("Minimum 1 GB");
        if (gb < 2) return FieldStatus.warning("< 2 GB may cause lag");
        return FieldStatus.ok();
    }

    private static final Pattern INVALID_PATH = Pattern.compile("[<>:\"|?*]");
    private static boolean isValidPath(String p) { return !INVALID_PATH.matcher(p).find(); }

    // ── Result types ──────────────────────────────────────────────────────────

    public static class ValidationResult {
        private final List<Issue> issues = new ArrayList<>();
        void addError(String f, String m)   { issues.add(new Issue(f, m, Severity.ERROR)); }
        void addWarning(String f, String m) { issues.add(new Issue(f, m, Severity.WARNING)); }
        public boolean isValid() { return issues.stream().noneMatch(i -> i.severity == Severity.ERROR); }
        public List<Issue> getIssues() { return issues; }
        public String getSummary() {
            if (issues.isEmpty()) return "\u2713 Configuration is valid";
            StringBuilder sb = new StringBuilder();
            for (Issue i : issues)
                sb.append(i.severity == Severity.ERROR ? "\u2717 " : "\u26a0 ").append(i.message).append("\n");
            return sb.toString().trim();
        }
    }

    public enum Severity { ERROR, WARNING }

    public static class Issue {
        public final String field, message;
        public final Severity severity;
        Issue(String f, String m, Severity s) { field = f; message = m; severity = s; }
    }

    public static class FieldStatus {
        public enum Level { OK, WARNING, ERROR }
        public final Level level;
        public final String message;
        private FieldStatus(Level l, String m) { level = l; message = m; }
        public static FieldStatus ok()              { return new FieldStatus(Level.OK, ""); }
        public static FieldStatus warning(String m) { return new FieldStatus(Level.WARNING, m); }
        public static FieldStatus error(String m)   { return new FieldStatus(Level.ERROR, m); }
        public boolean isOk()      { return level == Level.OK; }
        public boolean isWarning() { return level == Level.WARNING; }
        public boolean isError()   { return level == Level.ERROR; }
    }
}

