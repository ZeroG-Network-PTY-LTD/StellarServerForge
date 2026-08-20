package com.zerog.network.stellarforge.model;

import java.util.function.Supplier;

/**
 * Represents a single recoverable action presented inside SmartErrorDialog.
 */
public class RecoveryAction {

    public enum ActionType {
        AUTO_FIX, GUIDED_FIX, RETRY, OPEN_LINK, COPY_LOG, REPORT_BUG, DISMISS
    }

    private final String label;
    private final String description;
    private final ActionType type;
    private final Runnable action;

    public RecoveryAction(String label, String description, ActionType type, Runnable action) {
        this.label = label;
        this.description = description;
        this.type = type;
        this.action = action;
    }

    public static RecoveryAction retry(String label, Runnable retry) {
        return new RecoveryAction(label, "Try the operation again", ActionType.RETRY, retry);
    }

    public static RecoveryAction dismiss() {
        return new RecoveryAction("Close", "Dismiss this error", ActionType.DISMISS, () -> {});
    }

    public static RecoveryAction copyLog(Supplier<String> logSupplier) {
        return new RecoveryAction("Copy Log", "Copy error details to clipboard",
                ActionType.COPY_LOG, () -> {
            try {
                java.awt.datatransfer.StringSelection sel =
                        new java.awt.datatransfer.StringSelection(logSupplier.get());
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
            } catch (Exception ignored) {}
        });
    }

    public static RecoveryAction openDoc(String label, String url) {
        return new RecoveryAction(label, "Opens documentation in browser",
                ActionType.OPEN_LINK, () -> {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            } catch (Exception ignored) {}
        });
    }

    public String getLabel()        { return label; }
    public String getDescription()  { return description; }
    public ActionType getType()     { return type; }
    public void execute()           { if (action != null) action.run(); }
}
