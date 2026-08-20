package com.zerog.stellarserverforge.gui.theme;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Factory methods for the app's recurring text styles, so every screen matches. */
public final class StellarLabels {

    private StellarLabels() {
    }

    /** A large title with a soft cyan glow behind the text — used once per screen as the banner. */
    public static JLabel title(String text) {
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(getFont());
                var fm = g2.getFontMetrics();
                int x = 0;
                int y = fm.getAscent();
                Color glow = new Color(StellarTheme.STAR_CYAN.getRed(), StellarTheme.STAR_CYAN.getGreen(),
                        StellarTheme.STAR_CYAN.getBlue(), 70);
                g2.setColor(glow);
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        if (dx != 0 || dy != 0) {
                            g2.drawString(getText(), x + dx, y + dy);
                        }
                    }
                }
                g2.setColor(getForeground());
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        label.setFont(StellarTheme.FONT_TITLE);
        label.setForeground(StellarTheme.TEXT_PRIMARY);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(StellarTheme.FONT_HEADING);
        label.setForeground(StellarTheme.STAR_CYAN);
        return label;
    }

    public static JLabel body(String text) {
        JLabel label = new JLabel(text);
        label.setFont(StellarTheme.FONT_BODY);
        label.setForeground(StellarTheme.TEXT_PRIMARY);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(StellarTheme.FONT_BODY);
        label.setForeground(StellarTheme.TEXT_SECONDARY);
        return label;
    }

    public static JLabel value(String text) {
        JLabel label = new JLabel(text);
        label.setFont(StellarTheme.FONT_LABEL);
        label.setForeground(StellarTheme.STELLAR_GOLD);
        return label;
    }
}
