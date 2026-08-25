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
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple sliding-window call limiter, persisted to a small state file so the window survives
 * app restarts (an in-memory-only limiter would reset every time the app relaunches). Used to cap
 * how often a given API key gets used, independent of any rate limiting the remote service itself
 * applies.
 * <p>
 * The read-modify-write is done under an exclusive {@link FileLock} spanning the whole operation
 * (matching the PHP proxy's {@code flock()} use for the same reason) — that's what protects two
 * separate *processes* pointed at the same state file. Within a single JVM, {@link FileLock} isn't
 * re-entrant-safe: two different channels in the same process racing for an overlapping lock throw
 * {@link java.nio.channels.OverlappingFileLockException} instead of blocking. {@link #INTRA_JVM_LOCKS}
 * — a static, path-keyed monitor shared by every {@code RateLimiter} instance — serializes access
 * within the JVM first, so only one thread ever attempts {@code channel.lock()} for a given file at
 * a time; the {@code FileLock} itself remains what serializes across processes.
 */
public class RateLimiter {

    /** Thrown when a call would exceed the limit; carries how long until the oldest call ages out. */
    public static final class RateLimitExceededException extends IOException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }

    private static final ConcurrentHashMap<String, Object> INTRA_JVM_LOCKS = new ConcurrentHashMap<>();

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
    public void checkAndRecord() throws IOException {
        Files.createDirectories(stateFile.getParent());
        Object intraJvmLock = INTRA_JVM_LOCKS.computeIfAbsent(
                stateFile.toAbsolutePath().normalize().toString(), k -> new Object());

        synchronized (intraJvmLock) {
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
