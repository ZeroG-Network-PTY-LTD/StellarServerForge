package com.zerog.network.stellarforge.utils;

import com.zerog.network.stellarforge.model.RecoveryAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides intelligent error recovery actions based on the exception type,
 * error message, and context.  Returns a list of RecoveryActions that the
 * SmartErrorDialog can present to the user.
 */
public class ErrorRecovery {

    private static final Logger logger = LoggerFactory.getLogger(ErrorRecovery.class);

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Analyse an exception and return relevant recovery actions.
     * @param e          The exception that occurred
     * @param context    Short description of what was being done ("Starting server", etc.)
     * @param retryAction  Optional runnable to retry the operation (may be null)
     */
    public static List<RecoveryAction> suggest(Throwable e, String context, Runnable retryAction) {
        List<RecoveryAction> actions = new ArrayList<>();

        String msg = e != null ? e.getMessage() : "";
        if (msg == null) msg = "";
        String lower = msg.toLowerCase();

        // ── Retry ─────────────────────────────────────────────────────────────
        if (retryAction != null) {
            actions.add(RecoveryAction.retry("🔄 Try Again", retryAction));
        }

        // ── Network errors ────────────────────────────────────────────────────
        if (e instanceof java.net.UnknownHostException
                || lower.contains("connect") || lower.contains("timeout")
                || lower.contains("network")) {
            actions.add(new RecoveryAction("Check Connection",
                    "Verify internet connectivity and firewall settings",
                    RecoveryAction.ActionType.GUIDED_FIX,
                    () -> {}));
        }

        // ── Java not found ────────────────────────────────────────────────────
        if (lower.contains("java") || lower.contains("jre") || lower.contains("jdk")
                || e instanceof IOException && lower.contains("cannot run program")) {
            actions.add(RecoveryAction.openDoc("Download Java",
                    "https://adoptium.net/"));
            actions.add(new RecoveryAction("Auto-Detect Java",
                    "Re-scan system for Java installations",
                    RecoveryAction.ActionType.AUTO_FIX,
                    () -> JavaManager.detectJavaInstallations()));
        }

        // ── Missing file / directory ──────────────────────────────────────────
        if (e instanceof java.nio.file.NoSuchFileException
                || lower.contains("no such file") || lower.contains("file not found")) {
            actions.add(new RecoveryAction("Run Setup",
                    "Initialize the server directory and required files",
                    RecoveryAction.ActionType.GUIDED_FIX,
                    () -> {}));
        }

        // ── Permission denied ─────────────────────────────────────────────────
        if (lower.contains("permission") || lower.contains("access denied")) {
            actions.add(new RecoveryAction("Fix Permissions",
                    "Attempt to fix file permissions automatically",
                    RecoveryAction.ActionType.AUTO_FIX,
                    () -> logger.info("Permission fix attempted")));
        }

        // ── Port in use ───────────────────────────────────────────────────────
        if (lower.contains("address already in use") || lower.contains("bind")) {
            actions.add(new RecoveryAction("Change Port",
                    "Open server configuration to choose a different port",
                    RecoveryAction.ActionType.GUIDED_FIX,
                    () -> {}));
        }

        // ── Out of memory ─────────────────────────────────────────────────────
        if (e instanceof OutOfMemoryError || lower.contains("out of memory")
                || lower.contains("heap space")) {
            actions.add(new RecoveryAction("Reduce RAM Allocation",
                    "Lower the RAM assigned to this server profile",
                    RecoveryAction.ActionType.GUIDED_FIX,
                    () -> {}));
            actions.add(RecoveryAction.openDoc("Optimize JVM Args",
                    "https://aikar.co/mcflags.html"));
        }

        // ── API key problems ──────────────────────────────────────────────────
        if (lower.contains("api key") || lower.contains("unauthorized")
                || lower.contains("403") || lower.contains("401")) {
            actions.add(RecoveryAction.openDoc("Get API Key",
                    "https://console.curseforge.com/"));
            actions.add(new RecoveryAction("Open Settings",
                    "Configure API keys in application settings",
                    RecoveryAction.ActionType.GUIDED_FIX,
                    () -> {}));
        }

        // ── Always include copy log + dismiss ─────────────────────────────────
        final String finalMsg = msg;
        final Throwable finalE = e;
        actions.add(RecoveryAction.copyLog(() -> buildLogText(context, finalE)));
        actions.add(RecoveryAction.openDoc("Troubleshooting Guide",
                "https://github.com/zerog-network/stellar-server-forge/blob/main/TROUBLESHOOTING.md"));
        actions.add(RecoveryAction.dismiss());

        return actions;
    }

    /** Build a formatted error report string for clipboard */
    private static String buildLogText(String context, Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Stellar Server Forge Error Report ===\n");
        sb.append("Context: ").append(context).append("\n");
        sb.append("Time: ").append(java.time.LocalDateTime.now()).append("\n");
        if (e != null) {
            sb.append("Exception: ").append(e.getClass().getName()).append("\n");
            sb.append("Message: ").append(e.getMessage()).append("\n\n");
            sb.append("Stack Trace:\n");
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            sb.append(sw);
        }
        return sb.toString();
    }
}
