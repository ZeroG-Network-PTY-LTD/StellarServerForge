package com.zerog.network.stellarforge.gui;

import com.zerog.network.stellarforge.model.RecoveryAction;
import com.zerog.network.stellarforge.utils.ErrorRecovery;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Replacement for plain JOptionPane.showMessageDialog for errors.
 * Shows a clear, non-technical explanation plus intelligent recovery actions.
 *
 * Usage:
 *   SmartErrorDialog.show(parent, "Starting Server", exception, retryAction);
 *   SmartErrorDialog.show(parent, "Title", "User-friendly message", exception, null);
 */
public class SmartErrorDialog extends JDialog {

    private static final Color BG        = new Color(30, 30, 35);
    private static final Color TITLE_FG  = new Color(255, 90, 90);
    private static final Color TEXT_FG   = new Color(210, 210, 220);
    private static final Color DETAIL_BG = new Color(20, 20, 25);
    private static final Color BORDER    = new Color(80, 30, 30);

    private SmartErrorDialog(Window parent, String title, String userMessage,
                              Throwable cause, List<RecoveryAction> actions) {
        super(parent, "Error — " + title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(BG);
        buildUI(title, userMessage, cause, actions);
        pack();
        setMinimumSize(new Dimension(520, 200));
        setLocationRelativeTo(parent);
    }

    // ── Static factory methods ─────────────────────────────────────────────────

    /** Show error with auto-suggested recovery actions */
    public static void show(Window parent, String context, Throwable cause, Runnable retryAction) {
        List<RecoveryAction> actions = ErrorRecovery.suggest(cause, context, retryAction);
        String msg = friendlyMessage(cause);
        new SmartErrorDialog(parent, context, msg, cause, actions).setVisible(true);
    }

    /** Show error with a custom user message */
    public static void show(Window parent, String context, String userMessage,
                            Throwable cause, Runnable retryAction) {
        List<RecoveryAction> actions = ErrorRecovery.suggest(cause, context, retryAction);
        new SmartErrorDialog(parent, context, userMessage, cause, actions).setVisible(true);
    }

    /** Show a simple error without an exception */
    public static void showSimple(Window parent, String title, String message) {
        List<RecoveryAction> actions = List.of(RecoveryAction.dismiss());
        new SmartErrorDialog(parent, title, message, null, actions).setVisible(true);
    }

    // ── UI Construction ────────────────────────────────────────────────────────

    private void buildUI(String title, String userMessage,
                         Throwable cause, List<RecoveryAction> actions) {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(20, 24, 16, 24));

        // ── Header ─────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(8, 4));
        header.setOpaque(false);

        JLabel iconLabel = new JLabel("⚠");
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 32));
        iconLabel.setForeground(TITLE_FG);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        titleLabel.setForeground(TITLE_FG);

        JLabel msgLabel = new JLabel("<html><body style='width:420px'>" +
                escapeHtml(userMessage) + "</body></html>");
        msgLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        msgLabel.setForeground(TEXT_FG);

        JPanel titleRight = new JPanel(new GridLayout(2, 1, 0, 4));
        titleRight.setOpaque(false);
        titleRight.add(titleLabel);
        titleRight.add(msgLabel);

        header.add(iconLabel, BorderLayout.WEST);
        header.add(titleRight, BorderLayout.CENTER);

        // ── Expandable detail ──────────────────────────────────────────────────
        JPanel detailPanel = null;
        if (cause != null) {
            String detail = buildDetail(cause);
            JTextArea ta = new JTextArea(detail);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            ta.setEditable(false);
            ta.setBackground(DETAIL_BG);
            ta.setForeground(new Color(150, 150, 160));
            ta.setRows(5);
            JScrollPane sp = new JScrollPane(ta);
            sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
            sp.setPreferredSize(new Dimension(460, 90));

            JPanel collapseWrapper = new JPanel(new BorderLayout());
            collapseWrapper.setOpaque(false);
            collapseWrapper.setVisible(false);
            collapseWrapper.add(sp, BorderLayout.CENTER);

            JButton toggleBtn = new JButton("▶ Show Technical Details");
            toggleBtn.setBackground(new Color(45, 45, 52));
            toggleBtn.setForeground(TEXT_FG);
            toggleBtn.setBorderPainted(false);
            toggleBtn.setFocusPainted(false);
            toggleBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            toggleBtn.addActionListener(e -> {
                boolean vis = !collapseWrapper.isVisible();
                collapseWrapper.setVisible(vis);
                toggleBtn.setText((vis ? "▼" : "▶") + " Technical Details");
                pack();
            });

            detailPanel = new JPanel(new BorderLayout(0, 4));
            detailPanel.setOpaque(false);
            detailPanel.add(toggleBtn, BorderLayout.NORTH);
            detailPanel.add(collapseWrapper, BorderLayout.CENTER);
        }

        // ── Recovery action buttons ────────────────────────────────────────────
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionPanel.setOpaque(false);

        for (RecoveryAction action : actions) {
            JButton btn = actionButton(action);
            actionPanel.add(btn);
        }

        // ── Assemble ───────────────────────────────────────────────────────────
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(header, BorderLayout.NORTH);
        if (detailPanel != null) center.add(detailPanel, BorderLayout.CENTER);

        root.add(center, BorderLayout.CENTER);
        root.add(actionPanel, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JButton actionButton(RecoveryAction action) {
        JButton btn = new JButton(action.getLabel());
        btn.setToolTipText(action.getDescription());
        btn.setFocusPainted(false);
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        switch (action.getType()) {
            case RETRY:
                btn.setBackground(new Color(0, 120, 60));
                btn.setForeground(Color.WHITE);
                break;
            case AUTO_FIX:
                btn.setBackground(new Color(30, 100, 200));
                btn.setForeground(Color.WHITE);
                break;
            case DISMISS:
                btn.setBackground(new Color(60, 60, 70));
                btn.setForeground(new Color(200, 200, 210));
                break;
            default:
                btn.setBackground(new Color(50, 50, 60));
                btn.setForeground(new Color(200, 200, 210));
        }

        btn.addActionListener(e -> {
            action.execute();
            if (action.getType() == RecoveryAction.ActionType.DISMISS
                    || action.getType() == RecoveryAction.ActionType.RETRY) {
                dispose();
            }
        });
        return btn;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String friendlyMessage(Throwable e) {
        if (e == null) return "An unexpected error occurred.";
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) msg = e.getClass().getSimpleName();

        // Make exception messages more user-friendly
        if (msg.toLowerCase().contains("unknown host")) return "Could not connect to the server — check your internet connection.";
        if (msg.toLowerCase().contains("connection refused")) return "Connection refused. The server may be offline or the URL is incorrect.";
        if (msg.toLowerCase().contains("address already in use")) return "The server port is already being used by another application.";
        if (msg.toLowerCase().contains("no such file")) return "A required file was not found. Run Setup Server to initialize.";
        if (msg.toLowerCase().contains("permission denied") || msg.toLowerCase().contains("access denied"))
            return "Permission denied. Try running as administrator or check folder permissions.";

        return msg;
    }

    private static String buildDetail(Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n\n");
        for (StackTraceElement el : e.getStackTrace()) {
            sb.append("  at ").append(el).append("\n");
        }
        Throwable cause = e.getCause();
        if (cause != null) {
            sb.append("\nCaused by: ").append(cause.getClass().getName())
              .append(": ").append(cause.getMessage());
        }
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
