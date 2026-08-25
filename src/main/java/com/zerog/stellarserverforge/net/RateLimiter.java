package com.zerog.stellarserverforge.net;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
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
 * <p>
 * The read-modify-write is done under an exclusive {@link FileLock} spanning the whole operation
 * (matching the PHP proxy's {@code flock()} use for the same reason) — {@code synchronized} alone
 * only protects against races within one JVM; two app instances pointed at the same server
 * directory need real cross-process locking or they can each read a stale count and undercount the
 * limit between them.
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
        Files.createDirectories(stateFile.getParent());

        try (RandomAccessFile raf = new RandomAccessFile(stateFile.toFile(), "rw");
             FileChannel channel = raf.getChannel();
             FileLock lock = channel.lock()) {

            byte[] bytes = new byte[(int) channel.size()];
            raf.readFully(bytes);
            String content = new String(bytes, StandardCharsets.UTF_8);

            Instant now = Instant.now();
            Instant cutoff = now.minus(window);
            List<Instant> timestamps = parse(content);
            timestamps.removeIf(t -> t.isBefore(cutoff));

            if (timestamps.size() >= maxCalls) {
                Instant oldest = timestamps.get(0);
                Duration wait = Duration.between(now, oldest.plus(window));
                throw new RateLimitExceededException("Rate limit reached: " + maxCalls + " calls per "
                        + formatWindow(window) + ". Try again in " + formatWait(wait) + ".");
            }

            timestamps.add(now);

            StringBuilder sb = new StringBuilder();
            for (Instant t : timestamps) {
                sb.append(t).append('\n');
            }
            byte[] out = sb.toString().getBytes(StandardCharsets.UTF_8);
            channel.truncate(0);
            channel.position(0);
            raf.write(out);
        }
    }

    private static List<Instant> parse(String content) {
        List<Instant> timestamps = new ArrayList<>();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                try {
                    timestamps.add(Instant.parse(trimmed));
                } catch (Exception ignored) {
                    // Skip a corrupt/foreign line rather than failing the whole limiter.
                }
            }
        }
        return timestamps;
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
