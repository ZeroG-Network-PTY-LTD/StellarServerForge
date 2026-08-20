package com.zerog.network.stellarforge.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Toast notification component — lightweight, non-blocking user feedback.
 * Appears top-right and auto-dismisses.
 */
public class ToastNotification extends JWindow {

    public enum Type { SUCCESS, WARNING, ERROR, INFO }

    private static final Color COLOR_SUCCESS = new Color(39, 174, 96);
    private static final Color COLOR_WARNING = new Color(230, 126, 34);
    private static final Color COLOR_ERROR   = new Color(192, 57, 43);
    private static final Color COLOR_INFO    = new Color(41, 128, 185);

    private final Timer dismissTimer;

    private ToastNotification(Window owner, String title, String message, Type type, int durationMs) {
        super(owner);

        Color bg;
        if (type == Type.SUCCESS)      bg = COLOR_SUCCESS;
        else if (type == Type.WARNING) bg = COLOR_WARNING;
        else if (type == Type.ERROR)   bg = COLOR_ERROR;
        else                           bg = COLOR_INFO;

        String icon;
        if (type == Type.SUCCESS)      icon = "✓";
        else if (type == Type.WARNING) icon = "⚠";
        else if (type == Type.ERROR)   icon = "✗";
        else                           icon = "ℹ";

        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBackground(bg);
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        JLabel msgLabel = new JLabel("<html><body style='width:200px'>" + message + "</body></html>");
        msgLabel.setForeground(new Color(230, 230, 230));
        msgLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

        textPanel.add(titleLabel);
        textPanel.add(msgLabel);

        JButton closeBtn = new JButton("×");
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setOpaque(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dismiss());

        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(textPanel, BorderLayout.CENTER);
        panel.add(closeBtn, BorderLayout.EAST);

        setContentPane(panel);
        pack();

        // Position top-right of owner
        if (owner != null) {
            Dimension ownerSize = owner.getSize();
            Point ownerLoc  = owner.getLocation();
            int x = ownerLoc.x + ownerSize.width - getWidth() - 20;
            int y = ownerLoc.y + 60;
            setLocation(x, y);
        }

        dismissTimer = new Timer(durationMs, e -> dismiss());
        dismissTimer.setRepeats(false);
    }

    private void display() {
        setVisible(true);
        dismissTimer.start();
    }

    private void dismiss() {
        dismissTimer.stop();
        setVisible(false);
        dispose();
    }

    // ─── Static factory helpers ───────────────────────────────────────────────

    public static void success(Window owner, String message) {
        show(owner, "Success", message, Type.SUCCESS, 3000);
    }

    public static void warning(Window owner, String message) {
        show(owner, "Warning", message, Type.WARNING, 5000);
    }

    public static void error(Window owner, String message) {
        show(owner, "Error", message, Type.ERROR, 8000);
    }

    public static void info(Window owner, String message) {
        show(owner, "Info", message, Type.INFO, 4000);
    }

    public static void show(Window owner, String title, String message, Type type, int durationMs) {
        SwingUtilities.invokeLater(() -> {
            ToastNotification toast = new ToastNotification(owner, title, message, type, durationMs);
            toast.show();
        });
    }
}





