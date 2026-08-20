package com.zerog.network.stellarforge.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Centralized tracker for all background operations.
 *
 * Usage:
 * <pre>
 *   String id = ProgressManager.getInstance().start("Downloading mods", true);
 *   // … work …
 *   ProgressManager.getInstance().update(id, 50, "Halfway done");
 *   ProgressManager.getInstance().finish(id, "All mods downloaded");
 *   // or
 *   ProgressManager.getInstance().fail(id, "Network error");
 * </pre>
 */
public class ProgressManager {

    private static final Logger log = LoggerFactory.getLogger(ProgressManager.class);
    private static final int HISTORY_LIMIT = 100;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static volatile ProgressManager instance;
    public static ProgressManager getInstance() {
        if (instance == null) synchronized (ProgressManager.class) {
            if (instance == null) instance = new ProgressManager();
        }
        return instance;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Map<String, Operation>     active  = new LinkedHashMap<>();
    private final Deque<Operation>           history = new ArrayDeque<>();
    private final List<Consumer<Operation>>  listeners = new CopyOnWriteArrayList<>();

    private ProgressManager() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /** Register a listener that is notified (on the EDT) whenever an operation changes. */
    public void addListener(Consumer<Operation> listener) { listeners.add(listener); }
    public void removeListener(Consumer<Operation> listener) { listeners.remove(listener); }

    /**
     * Start a new operation.
     * @param name     User-visible name
     * @param cancellable  Whether the user may cancel it
     * @return opId used for subsequent calls
     */
    public String start(String name, boolean cancellable) {
        String id = UUID.randomUUID().toString();
        Operation op = new Operation(id, name, cancellable);
        synchronized (this) { active.put(id, op); }
        notifyListeners(op);
        log.debug("Operation started: {}", name);
        return id;
    }

    /** Update progress (0-100) and optional status message. */
    public void update(String id, int percent, String statusMsg) {
        Operation op = getOp(id);
        if (op == null) return;
        op.percent    = Math.max(0, Math.min(100, percent));
        op.statusMsg  = statusMsg;
        notifyListeners(op);
    }

    /** Mark the operation successfully completed. */
    public void finish(String id, String resultMsg) {
        complete(id, Operation.Status.SUCCESS, resultMsg);
    }

    /** Mark the operation as failed. */
    public void fail(String id, String errorMsg) {
        complete(id, Operation.Status.FAILED, errorMsg);
    }

    /** Cancel the operation (if cancellable). */
    public void cancel(String id) {
        complete(id, Operation.Status.CANCELLED, "Cancelled by user");
    }

    /** Return all currently running operations (snapshot). */
    public synchronized List<Operation> getActive() {
        return new ArrayList<>(active.values());
    }

    /** Return the last HISTORY_LIMIT completed operations (newest first). */
    public synchronized List<Operation> getHistory() {
        List<Operation> list = new ArrayList<>(history);
        Collections.reverse(list);
        return list;
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void complete(String id, Operation.Status status, String msg) {
        Operation op;
        synchronized (this) {
            op = active.remove(id);
            if (op == null) return;
            op.status    = status;
            op.statusMsg = msg;
            op.percent   = 100;
            op.endTime   = Instant.now();
            history.addLast(op);
            if (history.size() > HISTORY_LIMIT) history.removeFirst();
        }
        notifyListeners(op);
        log.debug("Operation {}: {} — {}", status, op.name, msg);
    }

    private synchronized Operation getOp(String id) { return active.get(id); }

    private void notifyListeners(Operation op) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> listeners.forEach(l -> l.accept(op)));
        } else {
            listeners.forEach(l -> l.accept(op));
        }
    }

    // ── Operation model ───────────────────────────────────────────────────────

    public static class Operation {
        public enum Status { RUNNING, SUCCESS, FAILED, CANCELLED }

        public final String  id;
        public final String  name;
        public final boolean cancellable;
        public final Instant startTime;

        public volatile String   statusMsg = "";
        public volatile int      percent   = 0;
        public volatile Status   status    = Status.RUNNING;
        public volatile Instant  endTime   = null;

        Operation(String id, String name, boolean cancellable) {
            this.id          = id;
            this.name        = name;
            this.cancellable = cancellable;
            this.startTime   = Instant.now();
        }

        public boolean isRunning()   { return status == Status.RUNNING; }
        public boolean isCompleted() { return status != Status.RUNNING; }

        /** Duration string, e.g. "2.3 s" */
        public String durationStr() {
            Instant end = (endTime != null) ? endTime : Instant.now();
            long ms = end.toEpochMilli() - startTime.toEpochMilli();
            return ms < 1000 ? ms + " ms" : String.format("%.1f s", ms / 1000.0);
        }

        public String statusIcon() {
            switch (status) {
                case RUNNING:   return "\u23f3"; // ⏳
                case SUCCESS:   return "\u2705"; // ✅
                case FAILED:    return "\u274c"; // ❌
                case CANCELLED: return "\u26d4"; // ⛔
                default:        return "?";
            }
        }
    }
}

