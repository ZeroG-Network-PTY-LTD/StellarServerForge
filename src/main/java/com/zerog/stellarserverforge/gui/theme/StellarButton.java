package com.zerog.stellarserverforge.gui.theme;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A flat, rounded, glowing button matching the Stellar theme — Swing's default {@link JButton}
 * painting ignores most color customization under most look-and-feels, so this paints itself.
 */
public class StellarButton extends JButton {

    /** Visual weight: PRIMARY for the main action, SECONDARY for everything else, DANGER for destructive actions. */
    public enum Variant { PRIMARY, SECONDARY, DANGER }

    private final Variant variant;
    private boolean hovered;

    public StellarButton(String text) {
        this(text, Variant.SECONDARY);
    }

    public StellarButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setFont(StellarTheme.FONT_LABEL);
        setForeground(StellarTheme.TEXT_PRIMARY);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setMargin(new java.awt.Insets(6, 14, 6, 14));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    private Color baseColor() {
        return switch (variant) {
            case PRIMARY -> new Color(0x2E, 0x3E, 0xA8);
            case SECONDARY -> new Color(0x1C, 0x20, 0x40);
            case DANGER -> new Color(0x7A, 0x22, 0x2E);
        };
    }

    private Color accentColor() {
        return switch (variant) {
            case PRIMARY -> StellarTheme.STAR_CYAN;
            case SECONDARY -> StellarTheme.PANEL_BORDER;
            case DANGER -> StellarTheme.ERROR_RED;
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        float arc = h * 0.55f;
        RoundRectangle2D shape = new RoundRectangle2D.Float(1, 1, w - 2, h - 2, arc, arc);

        Color base = baseColor();
        Color fill = isEnabled() ? (hovered ? lighten(base, 0.18f) : base) : new Color(0x15, 0x17, 0x2A);
        g2.setColor(fill);
        g2.fill(shape);

        Color border = isEnabled() ? accentColor() : StellarTheme.TEXT_MUTED;
        g2.setColor(hovered && isEnabled() ? border : withAlpha(border, 0.55f));
        g2.setStroke(new java.awt.BasicStroke(hovered ? 1.6f : 1f));
        g2.draw(shape);

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 60), Math.max(d.height, 30));
    }

    private static Color lighten(Color c, float amount) {
        int r = (int) Math.min(255, c.getRed() + 255 * amount);
        int g = (int) Math.min(255, c.getGreen() + 255 * amount);
        int b = (int) Math.min(255, c.getBlue() + 255 * amount);
        return new Color(r, g, b);
    }

    private static Color withAlpha(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(alpha * 255));
    }
}
