package com.zerog.network.stellarforge.utils;

import com.zerog.network.stellarforge.model.ServerMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Background monitor that polls a running Minecraft server process and
 * accumulates ServerMetrics history.  Sends periodic commands to the server
 * stdin to request stats when possible.
 *
 * Usage:
 *   ServerMonitor mon = new ServerMonitor(serverInputWriter, metricsConsumer);
 *   mon.start();
 *   // from console reader: mon.feedLine(rawConsoleLine);
 *   mon.stop();
 */
public class ServerMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ServerMonitor.class);
    private static final int POLL_SECONDS = 10;

    private final PrintWriter serverInput;          // stdin of the server process (may be null)
    private final Consumer<ServerMetrics> onUpdate; // called on EDT with latest snapshot

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<ServerMetrics> latestMetrics =
            new AtomicReference<>(ServerMetrics.empty());

    private final List<ServerMetrics> history =
            Collections.synchronizedList(new ArrayList<>());

    private Thread pollThread;

    // Mutable state updated from console feed
    private volatile double tps1 = 20.0, tps5 = 20.0, tps15 = 20.0;
    private volatile long memUsed = 0, memMax = 0;
    private volatile int players = 0, maxPlayers = 20;

    public ServerMonitor(PrintWriter serverInput, Consumer<ServerMetrics> onUpdate) {
        this.serverInput = serverInput;
        this.onUpdate    = onUpdate;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void start() {
        if (running.getAndSet(true)) return;
        pollThread = new Thread(this::pollLoop, "ServerMonitor-Poll");
        pollThread.setDaemon(true);
        pollThread.start();
        logger.info("ServerMonitor started");
    }

    public void stop() {
        running.set(false);
        if (pollThread != null) pollThread.interrupt();
        logger.info("ServerMonitor stopped");
    }

    // ── Console line ingestion ────────────────────────────────────────────────

    /**
     * Feed every line of server console output here (called from the reader thread).
     * Extracts metrics and updates internal state; notifies listener on EDT.
     */
    public void feedLine(String line) {
        // TPS
        double[] tpsParsed = LogParser.parseTps(line);
        if (tpsParsed != null) {
            tps1 = tpsParsed[0]; tps5 = tpsParsed[1]; tps15 = tpsParsed[2];
        }

        // Memory
        long[] mem = LogParser.parseMemory(line);
        if (mem != null) {
            memUsed = mem[0]; memMax = mem[1];
        }

        // Players (from /list)
        int[] pl = LogParser.parsePlayers(line);
        if (pl != null) {
            players = pl[0]; maxPlayers = pl[1];
        }

        // Player join/leave (increment/decrement)
        if (LogParser.parsePlayerJoined(line) != null) players = Math.max(0, players + 1);
        if (LogParser.parsePlayerLeft(line)   != null) players = Math.max(0, players - 1);

        buildSnapshot();
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public ServerMetrics getLatest() {
        return latestMetrics.get();
    }

    public List<ServerMetrics> getHistory() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void pollLoop() {
        while (running.get()) {
            try {
                Thread.sleep(POLL_SECONDS * 1000L);
                if (!running.get()) break;

                // Request TPS + player list from server (works if Essentials/EssentialsX installed)
                if (serverInput != null) {
                    serverInput.println("tps");
                    serverInput.println("list");
                    serverInput.flush();
                }

                // Also sample JVM memory (reflects server process on same JVM; proxy approach)
                if (memMax == 0) {
                    MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
                    MemoryUsage heap = bean.getHeapMemoryUsage();
                    memUsed = heap.getUsed()      / (1024 * 1024);
                    memMax  = heap.getCommitted()  / (1024 * 1024);
                }

                buildSnapshot();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warn("Monitor poll error: {}", e.getMessage());
            }
        }
    }

    private void buildSnapshot() {
        ServerMetrics snap = ServerMetrics.empty()
                .tps(tps1, tps5, tps15)
                .mem(memUsed, memMax)
                .players(players, maxPlayers);

        latestMetrics.set(snap);
        synchronized (history) {
            history.add(snap);
            // Trim history
            while (history.size() > ServerMetrics.HISTORY_SIZE) history.remove(0);
        }

        if (onUpdate != null) {
            SwingUtilities.invokeLater(() -> onUpdate.accept(snap));
        }
    }
}
