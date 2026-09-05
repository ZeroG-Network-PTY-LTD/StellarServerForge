package com.zerog.stellarserverforge.gui.theme;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A Nocturne text field — a rounded, outlined input box matching {@link StellarPanel}/{@link
 * StellarButton}'s visual language, with an accent-colored border on focus, replacing the
 * look-and-feel's default (square-cornered, no focus color) field chrome.
 */
public class StellarTextField extends JTextField {

    public StellarTextField(int columns) {
        super(columns);
        setOpaque(false);
        setBackground(StellarTheme.FIELD_BG);
        setForeground(StellarTheme.TEXT_PRIMARY);
        setCaretColor(StellarTheme.ACCENT);
        setFont(StellarTheme.FONT_BODY);
        setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float r = StellarTheme.RADIUS_CONTROL;
        RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, r, r);
        g2.setColor(getBackground());
        g2.fill(shape);
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float r = StellarTheme.RADIUS_CONTROL;
        RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, r, r);
        g2.setColor(isFocusOwner() ? StellarTheme.ACCENT : StellarTheme.NEUTRAL_800);
        g2.draw(shape);
        g2.dispose();
    }
}
