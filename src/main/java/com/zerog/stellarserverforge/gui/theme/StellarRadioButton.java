package com.zerog.stellarserverforge.gui.theme;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JRadioButton;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

/**
 * A Nocturne radio button — an outlined circle (accent-filled dot when selected, accent outline
 * on hover) replacing the look-and-feel's default radio glyph, so it matches the rest of the
 * app's outlined/accent visual language instead of Nimbus's stock chrome.
 */
public class StellarRadioButton extends JRadioButton {

    public StellarRadioButton(String text, boolean selected) {
        super(text, selected);
        setOpaque(false);
        setForeground(StellarTheme.TEXT_PRIMARY);
        setFont(StellarTheme.FONT_BODY);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setIconTextGap(8);
        setIcon(new RadioIcon());
    }

    private static final class RadioIcon implements Icon {
        private static final int SIZE = 16;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            AbstractButton button = (AbstractButton) c;
            boolean selected = button.isSelected();
            boolean hovered = button.isEnabled() && (button.getModel().isRollover() || button.getModel().isArmed());

            Ellipse2D.Float outer = new Ellipse2D.Float(x + 0.5f, y + 0.5f, SIZE - 1, SIZE - 1);
            g2.setColor(!button.isEnabled() ? withAlpha(StellarTheme.NEUTRAL_700, 0.45f)
                    : selected || hovered ? StellarTheme.ACCENT_400 : StellarTheme.NEUTRAL_700);
            g2.draw(outer);

            if (selected) {
                float dotSize = 8f;
                float offset = (SIZE - dotSize) / 2f;
                g2.setColor(button.isEnabled() ? StellarTheme.ACCENT : withAlpha(StellarTheme.ACCENT, 0.45f));
                g2.fill(new Ellipse2D.Float(x + offset, y + offset, dotSize, dotSize));
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }

        private static java.awt.Color withAlpha(java.awt.Color c, float alpha) {
            return new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(alpha * 255));
        }
    }
}
