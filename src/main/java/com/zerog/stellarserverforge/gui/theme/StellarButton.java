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
 * A Nocturne button — every button in the system is outlined on a transparent ground; there is no
 * solid-filled button. PRIMARY marks the one main action in a screen region, SECONDARY is
 * everything else, GHOST is for Back/Cancel-weight actions, DANGER is for destructive actions.
 */
public class StellarButton extends JButton {

    public enum Variant { PRIMARY, SECONDARY, GHOST, DANGER }

    private final Variant variant;
    private boolean hovered;
    private boolean pressedPaint;

    public StellarButton(String text) {
        this(text, Variant.SECONDARY);
    }

    public StellarButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setFont(StellarTheme.FONT_BODY);
        setForeground(textColor());
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
                pressedPaint = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressedPaint = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressedPaint = false;
                repaint();
            }
        });
    }

    private Color borderColor() {
        return switch (variant) {
            case PRIMARY -> StellarTheme.ACCENT;
            case SECONDARY -> StellarTheme.NEUTRAL_700;
            case GHOST -> null;
            case DANGER -> StellarTheme.STATUS_FAILED;
        };
    }

    private Color hoverBorderColor() {
        return switch (variant) {
            case PRIMARY -> StellarTheme.ACCENT_400;
            case SECONDARY -> StellarTheme.ACCENT_400;
            case GHOST -> null;
            case DANGER -> StellarTheme.STATUS_FAILED;
        };
    }

    private Color textColor() {
        return switch (variant) {
            case PRIMARY -> StellarTheme.ACCENT_300;
            case SECONDARY -> StellarTheme.TEXT_PRIMARY;
            case GHOST -> StellarTheme.TEXT_SECONDARY;
            case DANGER -> StellarTheme.STATUS_FAILED;
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        float arc = StellarTheme.RADIUS_CONTROL * 2f;
        RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, arc, arc);

        if (isEnabled()) {
            if (pressedPaint) {
                g2.setColor(StellarTheme.ACCENT_800);
                g2.fill(shape);
                setForeground(StellarTheme.ACCENT_200);
            } else if (hovered) {
                g2.setColor(StellarTheme.ACCENT_900);
                g2.fill(shape);
                setForeground(textColor());
            } else {
                setForeground(textColor());
            }

            Color border = hovered ? hoverBorderColor() : borderColor();
            if (border != null) {
                g2.setColor(border);
                g2.setStroke(new java.awt.BasicStroke(1f));
                g2.draw(shape);
            }
        } else {
            setForeground(withAlpha(textColor(), 0.45f));
            Color border = borderColor();
            if (border != null) {
                g2.setColor(withAlpha(border, 0.45f));
                g2.draw(shape);
            }
        }

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 60), Math.max(d.height, 34));
    }

    private static Color withAlpha(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(alpha * 255));
    }
}
