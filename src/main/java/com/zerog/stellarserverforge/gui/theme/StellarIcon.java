package com.zerog.stellarserverforge.gui.theme;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/** Generates the app window icon programmatically — a Nocturne-surface circle with an "S" monogram. */
public final class StellarIcon {

    private StellarIcon() {
    }

    public static BufferedImage create(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Ellipse2D circle = new Ellipse2D.Float(1, 1, size - 2, size - 2);
        g.setColor(StellarTheme.SURFACE);
        g.fill(circle);
        g.setColor(StellarTheme.ACCENT);
        g.setStroke(new java.awt.BasicStroke(Math.max(1f, size / 16f)));
        g.draw(circle);

        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(size * 0.5f));
        g.setFont(font);
        var fm = g.getFontMetrics();
        String text = "S";
        int x = (size - fm.stringWidth(text)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g.setColor(StellarTheme.ACCENT_300);
        g.drawString(text, x, y);

        g.dispose();
        return image;
    }
}
