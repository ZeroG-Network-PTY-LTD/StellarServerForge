package com.zerog.stellarserverforge.net_port;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Optional;

/**
 * Checks whether a port is already bound and offers to end the owning process, mirroring the
 * bat script's NETSTAT/TASKLIST/TASKKILL flow (spec §9.1) — the original script's kill-or-quit
 * prompt was unreachable dead code; this implements the intended working behavior instead.
 */
public class PortConflictService {

    public record OwningProcess(long pid, String name) {
    }

    /** Cheap, cross-platform "is this port free" check via a bind attempt. */
    public boolean isPortFree(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Best-effort lookup of the process currently bound to the port, where the platform allows it. */
    public Optional<OwningProcess> findOwningProcess(int port) {
        if (isWindows()) {
            return findOwningProcessWindows(port);
        }
        return Optional.empty();
    }

    public boolean killProcess(long pid) {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        if (handle.isEmpty()) {
            return false;
        }
        return handle.get().destroyForcibly();
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private Optional<OwningProcess> findOwningProcessWindows(int port) {
        try {
            Process netstat = new ProcessBuilder("netstat", "-ano").redirectErrorStream(true).start();
            Long pid = null;
            try (var reader = netstat.inputReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("TCP") && !trimmed.startsWith("UDP")) {
                        continue;
                    }
                    String[] parts = trimmed.split("\\s+");
                    if (parts.length < 2) {
                        continue;
                    }
                    String localAddress = parts[1];
                    if (!localAddress.endsWith(":" + port)) {
                        continue;
                    }
                    if (trimmed.startsWith("TCP") && parts.length >= 4 && !"LISTENING".equalsIgnoreCase(parts[3])) {
                        continue;
                    }
                    String pidToken = parts[parts.length - 1];
                    try {
                        pid = Long.parseLong(pidToken);
                    } catch (NumberFormatException ignored) {
                        continue;
                    }
                    break;
                }
            }
            netstat.waitFor();
            if (pid == null) {
                return Optional.empty();
            }
            String name = ProcessHandle.of(pid)
                    .flatMap(h -> h.info().command())
                    .orElse("unknown");
            return Optional.of(new OwningProcess(pid, name));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }
}
