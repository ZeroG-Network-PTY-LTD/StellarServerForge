package com.zerog.stellarserverforge.launch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Runs the Minecraft server process, streaming its console output line-by-line to a listener
 * (spec §12.3). Replaces the batch script's direct {@code java ...} invocation.
 */
public class ServerProcessRunner {

    private final Path workingDirectory;
    private Process process;

    public ServerProcessRunner(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    /**
     * @param jvmArgs  {@code -Xmx}, tuning, and other JVM flags (spec §8) — everything before
     *                 the launch target.
     * @param tailArgs the launch target and anything after it, e.g. {@code ["-jar", "server.jar", "nogui"]}
     *                 for vanilla/Fabric/Quilt/legacy-Forge, or {@code ["@libraries/.../win_args.txt", "nogui"]}
     *                 for modern Forge/NeoForge (spec §6.7).
     */
    public void start(String javaCommand, List<String> jvmArgs, List<String> tailArgs, Consumer<String> onOutputLine)
            throws IOException {
        if (isRunning()) {
            throw new IllegalStateException("A server process is already running");
        }

        List<String> command = new java.util.ArrayList<>();
        command.add(javaCommand);
        command.addAll(jvmArgs);
        command.addAll(tailArgs);

        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true);
        process = builder.start();

        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    onOutputLine.accept(line);
                }
            } catch (IOException ignored) {
                // Stream closed because the process ended.
            }
        }, "server-console-reader");
        reader.setDaemon(true);
        reader.start();
    }

    /** Sends a graceful "stop" command via stdin, falling back to a forced kill after the timeout. */
    public void stop(long gracefulTimeoutSeconds) {
        if (!isRunning()) {
            return;
        }
        try {
            OutputStream stdin = process.getOutputStream();
            stdin.write("stop\n".getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        } catch (IOException ignored) {
            // Fall through to forced termination below.
        }

        try {
            if (!process.waitFor(gracefulTimeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    public int waitForExit() throws InterruptedException {
        return process.waitFor();
    }
}
