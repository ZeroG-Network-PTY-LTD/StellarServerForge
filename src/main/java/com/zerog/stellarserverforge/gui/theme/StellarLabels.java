package com.zerog.stellarserverforge.gui.theme;

import javax.swing.JLabel;

/** Factory methods for the app's recurring text styles (Nocturne §4), so every screen matches. */
public final class StellarLabels {

    private StellarLabels() {
    }

    /** Screen title — one per screen, flush left. No glow, no decoration. */
    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(StellarTheme.FONT_TITLE);
        label.setForeground(StellarTheme.TEXT_PRIMARY);
        return label;
    }

    /** Card title / dialog title. */
    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(StellarTheme.FONT_HEADING);
        label.setForeground(StellarTheme.TEXT_PRIMARY);
        return label;
    }

    /** Field-group label / card eyebrow: uppercase, small, neutral-500. Pass text already upper-cased. */
    public static JLabel kicker(String text) {
        JLabel label = new JLabel(text);
        label.setFont(StellarTheme.FONT_KICKER);
        label.setForeground(StellarTheme.TEXT_KICKER);
        return label;
    }

    /** Default body copy. */
    public static JLabel body(String text) {
        JLabel label = new JLabel(text);
        label.setFont(StellarTheme.FONT_BODY);
        label.setForeground(StellarTheme.TEXT_PRIMARY);
        return label;
    }

    /** Helper text under a field, or de-emphasized supporting copy. */
    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(StellarTheme.FONT_CAPTION);
        label.setForeground(StellarTheme.TEXT_SECONDARY);
        return label;
    }

    /** Monospace value — paths, versions, ports. Primary text color, not accent. */
    public static JLabel value(String text) {
        JLabel label = new JLabel(text);
        label.setFont(StellarTheme.FONT_MONO);
        label.setForeground(StellarTheme.TEXT_PRIMARY);
        return label;
    }
}
