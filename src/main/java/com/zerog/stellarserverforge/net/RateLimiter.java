package com.zerog.stellarserverforge.net;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple sliding-window call limiter, persisted to a small state file so the window survives
 * app restarts (an in-memory-only limiter would reset every time the app relaunches). Used to cap
 * how often a given API key gets used, independent of any rate limiting the remote service itself
 * applies.
 */
public class RateLimiter {

    /** Thrown when a call would exceed the limit; carries how long until the oldest call ages out. */
    public static final class RateLimitExceededException extends IOException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }

    private final Path stateFile;
    private final int maxCalls;
    private final Duration window;

    public RateLimiter(Path stateFile, int maxCalls, Duration window) {
        this.stateFile = stateFile;
        this.maxCalls = maxCalls;
        this.window = window;
    }

    /** Checks the current window isn't exhausted, records this call if it isn't, and throws
     * {@link RateLimitExceededException} (naming when the next slot frees up) if it is. */
    public synchronized void checkAndRecord() throws IOException {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        List<Instant> timestamps = load();
        timestamps.removeIf(t -> t.isBefore(cutoff));

        if (timestamps.size() >= maxCalls) {
            Instant oldest = timestamps.get(0);
            Duration wait = Duration.between(now, oldest.plus(window));
            throw new RateLimitExceededException("Rate limit reached: " + maxCalls + " calls per "
                    + formatWindow(window) + ". Try again in " + formatWait(wait) + ".");
        }

        timestamps.add(now);
        save(timestamps);
    }

    private List<Instant> load() {
        List<Instant> timestamps = new ArrayList<>();
        if (!Files.isRegularFile(stateFile)) {
            return timestamps;
        }
        try {
            for (String line : Files.readAllLines(stateFile)) {
                if (!line.isBlank()) {
                    try {
                        timestamps.add(Instant.parse(line.trim()));
                    } catch (Exception ignored) {
                        // Skip a corrupt/foreign line rather than failing the whole limiter.
                    }
                }
            }
        } catch (IOException ignored) {
            // Treat an unreadable state file as an empty window rather than blocking all calls.
        }
        return timestamps;
    }

    private void save(List<Instant> timestamps) throws IOException {
        Files.createDirectories(stateFile.getParent());
        StringBuilder sb = new StringBuilder();
        for (Instant t : timestamps) {
            sb.append(t).append(System.lineSeparator());
        }
        Files.writeString(stateFile, sb.toString());
    }

    private static String formatWindow(Duration d) {
        long hours = d.toHours();
        return hours == 1 ? "1 hour" : hours + " hours";
    }

    private static String formatWait(Duration d) {
        long minutes = Math.max(1, d.toMinutes());
        return minutes == 1 ? "1 minute" : minutes + " minutes";
    }
}
