package com.zerog.stellarserverforge.gui.theme;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** A translucent, rounded "glass card" panel — the standard content container over the starfield. */
public class StellarPanel extends JPanel {

    public StellarPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18);
        g2.setColor(StellarTheme.PANEL_BG);
        g2.fill(shape);
        g2.setColor(StellarTheme.PANEL_BORDER);
        g2.draw(shape);

        g2.dispose();
        super.paintComponent(g);
    }
}
