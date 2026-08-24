package com.zerog.stellarserverforge.gui.theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * A Nocturne tag/pill (§6): small 4px-radius label, 10px/600 uppercase text, optional status dot.
 */
public class StellarTag extends JComponent {

    public enum Variant { ACCENT, NEUTRAL, OUTLINE }

    private String text;
    private Variant variant;
    private Color dotColor;

    public StellarTag(String text, Variant variant) {
        this(text, variant, null);
    }

    public StellarTag(String text, Variant variant, Color dotColor) {
        this.text = text.toUpperCase();
        this.variant = variant;
        this.dotColor = dotColor;
        setFont(StellarTheme.FONT_KICKER);
        setOpaque(false);
    }

    public void setText(String text) {
        this.text = text.toUpperCase();
        revalidate();
        repaint();
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
        repaint();
    }

    public void setDotColor(Color dotColor) {
        this.dotColor = dotColor;
        repaint();
    }

    private Color fill() {
        return switch (variant) {
            case ACCENT -> StellarTheme.ACCENT_900;
            case NEUTRAL -> StellarTheme.NEUTRAL_900;
            case OUTLINE -> null;
        };
    }

    private Color textColor() {
        return switch (variant) {
            case ACCENT -> StellarTheme.ACCENT_300;
            case NEUTRAL -> StellarTheme.NEUTRAL_300;
            case OUTLINE -> StellarTheme.ACCENT;
        };
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int dot = dotColor != null ? 12 : 0;
        int w = fm.stringWidth(text) + dot + 20;
        return new Dimension(w, 20);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        float r = StellarTheme.RADIUS_TAG;
        RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, r, r);

        Color fill = fill();
        if (fill != null) {
            g2.setColor(fill);
            g2.fill(shape);
        }
        if (variant == Variant.OUTLINE) {
            g2.setColor(StellarTheme.NEUTRAL_700);
            g2.draw(shape);
        }

        int textX = 10;
        if (dotColor != null) {
            int dotSize = 7;
            g2.setColor(dotColor);
            g2.fill(new Ellipse2D.Float(10, (h - dotSize) / 2f, dotSize, dotSize));
            textX = 22;
        }

        g2.setFont(getFont());
        g2.setColor(textColor());
        FontMetrics fm = g2.getFontMetrics();
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, textX, y);

        g2.dispose();
    }
}
