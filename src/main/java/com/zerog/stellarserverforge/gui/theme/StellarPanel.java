package com.zerog.stellarserverforge.gui.theme;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * A Nocturne card — surface-filled rounded container, 8px radius, a level-1 hairline edge. Every
 * content region sits in one of these; nothing floats on the raw window ground except the header
 * bar and the screen title.
 */
public class StellarPanel extends JPanel {

    public StellarPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float r = StellarTheme.RADIUS_CONTROL;
        RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, r, r);
        g2.setColor(StellarTheme.SURFACE);
        g2.fill(shape);
        g2.setColor(StellarTheme.NEUTRAL_800);
        g2.draw(shape);

        g2.dispose();
        super.paintComponent(g);
    }
}
