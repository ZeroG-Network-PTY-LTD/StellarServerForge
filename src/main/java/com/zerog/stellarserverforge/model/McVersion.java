package com.zerog.stellarserverforge.model;

/**
 * Parsed Minecraft version identifier.
 *
 * <p>Handles two numbering schemes, per the original Universalator spec:
 * <ul>
 *   <li>Legacy {@code 1.x.y} versions (everything released so far) — major is the
 *       second token, minor is the third (defaults to 0 if absent), no hotfix concept.</li>
 *   <li>A possible future year-based scheme (e.g. {@code 26.1} or {@code 26.1.2}) where the
 *       first token is not {@code 1} — major is the first token, minor the second, and an
 *       optional third token is a hotfix number (defaults to 0 if absent).</li>
 * </ul>
 */
public final class McVersion {

    private final String raw;
    private final int major;
    private final int minor;
    private final int hotfix;

    private McVersion(String raw, int major, int minor, int hotfix) {
        this.raw = raw;
        this.major = major;
        this.minor = minor;
        this.hotfix = hotfix;
    }

    public static McVersion parse(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Minecraft version is not set — run the setup wizard first.");
        }
        String[] tokens = version.split("\\.", 3);
        int major;
        int minor = 0;
        int hotfix = 0;

        if (tokens.length > 0 && tokens[0].equals("1")) {
            major = parseIntOrZero(tokens.length > 1 ? tokens[1] : "0");
            minor = tokens.length > 2 ? parseIntOrZero(tokens[2]) : 0;
        } else {
            major = parseIntOrZero(tokens.length > 0 ? tokens[0] : "0");
            minor = tokens.length > 1 ? parseIntOrZero(tokens[1]) : 0;
            hotfix = tokens.length > 2 ? parseIntOrZero(tokens[2]) : 0;
        }

        return new McVersion(version, major, minor, hotfix);
    }

    private static int parseIntOrZero(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String raw() {
        return raw;
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int hotfix() {
        return hotfix;
    }

    @Override
    public String toString() {
        return raw;
    }
}
