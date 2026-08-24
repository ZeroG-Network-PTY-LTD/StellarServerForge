package com.zerog.stellarserverforge.gui.theme;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A header-bar nav link: an icon/emoji + label, with a hover tint and a 2px accent underline
 * marking the active section (per the design manual: "the active section is marked by a 2px
 * accent underline, not a fill").
 */
public class StellarNavLink extends JComponent {

    private final String text;
    private final boolean active;
    private boolean hovered;

    public StellarNavLink(String text, boolean active, Runnable onClick) {
        this.text = text;
        this.active = active;
        setOpaque(false);
        setFont(StellarTheme.FONT_BODY);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

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

            @Override
            public void mouseClicked(MouseEvent e) {
                if (onClick != null) {
                    onClick.run();
                }
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = fm.stringWidth(text) + StellarTheme.SPACE_22;
        return new Dimension(w, 32);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (hovered && !active) {
            RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1,
                    StellarTheme.RADIUS_CONTROL, StellarTheme.RADIUS_CONTROL);
            g2.setColor(StellarTheme.ACCENT_900);
            g2.fill(shape);
        }

        g2.setFont(getFont());
        g2.setColor(active ? StellarTheme.ACCENT_300 : (hovered ? StellarTheme.ACCENT_400 : StellarTheme.TEXT_SECONDARY));
        FontMetrics fm = g2.getFontMetrics();
        int x = StellarTheme.SPACE_11;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);

        if (active) {
            g2.setColor(StellarTheme.ACCENT);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(StellarTheme.SPACE_8, h - 2, w - StellarTheme.SPACE_8, h - 2);
        }

        g2.dispose();
    }
}
