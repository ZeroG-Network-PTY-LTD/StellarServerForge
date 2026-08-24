package com.zerog.stellarserverforge.gui.theme;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.net.URI;

/**
 * A small circular outlined link icon for the footer bar. Brand marks (Discord/GitHub/Ko-fi) are
 * rendered from their real vector path data (CC0-licensed Simple Icons source) via {@link SvgPath}
 * so the footer shows the actual logos rather than an emoji glyph or a text monogram; the generic
 * icons (wiki/issues/website) are simple hand-drawn line-art in the same 24x24 coordinate space.
 * Opens the given URL in the system browser on click.
 */
public class StellarLinkIcon extends JComponent {

    private static final int SIZE = 32;
    private static final int VIEWBOX = 24;

    public enum Kind { DISCORD, GITHUB, KOFI, WIKI, ISSUES, WEBSITE }

    // Simple Icons (CC0) path data, viewBox 0 0 24 24.
    private static final String DISCORD_PATH = "M20.317 4.3698a19.7913 19.7913 0 00-4.8851-1.5152.0741.0741 0 00-.0785.0371c-.211.3753-.4447.8648-.6083 1.2495-1.8447-.2762-3.68-.2762-5.4868 0-.1636-.3933-.4058-.8742-.6177-1.2495a.077.077 0 00-.0785-.037 19.7363 19.7363 0 00-4.8852 1.515.0699.0699 0 00-.0321.0277C.5334 9.0458-.319 13.5799.0992 18.0578a.0824.0824 0 00.0312.0561c2.0528 1.5076 4.0413 2.4228 5.9929 3.0294a.0777.0777 0 00.0842-.0276c.4616-.6304.8731-1.2952 1.226-1.9942a.076.076 0 00-.0416-.1057c-.6528-.2476-1.2743-.5495-1.8722-.8923a.077.077 0 01-.0076-.1277c.1258-.0943.2517-.1923.3718-.2914a.0743.0743 0 01.0776-.0105c3.9278 1.7933 8.18 1.7933 12.0614 0a.0739.0739 0 01.0785.0095c.1202.099.246.1981.3728.2924a.077.077 0 01-.0066.1276 12.2986 12.2986 0 01-1.873.8914.0766.0766 0 00-.0407.1067c.3604.698.7719 1.3628 1.225 1.9932a.076.076 0 00.0842.0286c1.961-.6067 3.9495-1.5219 6.0023-3.0294a.077.077 0 00.0313-.0552c.5004-5.177-.8382-9.6739-3.5485-13.6604a.061.061 0 00-.0312-.0286zM8.02 15.3312c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9555-2.4189 2.157-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.9555 2.4189-2.1569 2.4189zm7.9748 0c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9554-2.4189 2.1569-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.946 2.4189-2.1568 2.4189Z";
    private static final String GITHUB_PATH = "M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12";
    private static final String KOFI_PATH = "M11.351 2.715c-2.7 0-4.986.025-6.83.26C2.078 3.285 0 5.154 0 8.61c0 3.506.182 6.13 1.585 8.493 1.584 2.701 4.233 4.182 7.662 4.182h.83c4.209 0 6.494-2.234 7.637-4a9.5 9.5 0 0 0 1.091-2.338C21.792 14.688 24 12.22 24 9.208v-.415c0-3.247-2.13-5.507-5.792-5.87-1.558-.156-2.65-.208-6.857-.208m0 1.947c4.208 0 5.09.052 6.571.182 2.624.311 4.13 1.584 4.13 4v.39c0 2.156-1.792 3.844-3.87 3.844h-.935l-.156.649c-.208 1.013-.597 1.818-1.039 2.546-.909 1.428-2.545 3.064-5.922 3.064h-.805c-2.571 0-4.831-.883-6.078-3.195-1.09-2-1.298-4.155-1.298-7.506 0-2.181.857-3.402 3.012-3.714 1.533-.233 3.559-.26 6.39-.26m6.547 2.287c-.416 0-.65.234-.65.546v2.935c0 .311.234.545.65.545 1.324 0 2.051-.754 2.051-2s-.727-2.026-2.052-2.026m-10.39.182c-1.818 0-3.013 1.48-3.013 3.142 0 1.533.858 2.857 1.949 3.897.727.701 1.87 1.429 2.649 1.896a1.47 1.47 0 0 0 1.507 0c.78-.467 1.922-1.195 2.623-1.896 1.117-1.039 1.974-2.364 1.974-3.897 0-1.662-1.247-3.142-3.039-3.142-1.065 0-1.792.545-2.338 1.298-.493-.753-1.246-1.298-2.312-1.298";

    private static final Path2D.Double DISCORD_SHAPE = SvgPath.parse(DISCORD_PATH);
    private static final Path2D.Double GITHUB_SHAPE = SvgPath.parse(GITHUB_PATH);
    private static final Path2D.Double KOFI_SHAPE = SvgPath.parse(KOFI_PATH);

    private final Kind kind;
    private boolean hovered;

    public StellarLinkIcon(Kind kind, String tooltip, String url) {
        this.kind = kind;
        setToolTipText(tooltip);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(SIZE, SIZE));

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
                openUrl(url);
            }
        });
    }

    private static void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {
            // Best-effort; nothing sensible to show the user if the system has no browser handler.
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(SIZE, SIZE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Ellipse2D circle = new Ellipse2D.Float(0.5f, 0.5f, SIZE - 1, SIZE - 1);
        if (hovered) {
            g2.setColor(StellarTheme.ACCENT_900);
            g2.fill(circle);
        }
        g2.setColor(hovered ? StellarTheme.ACCENT_400 : StellarTheme.NEUTRAL_700);
        g2.draw(circle);

        Color iconColor = hovered ? StellarTheme.ACCENT_300 : StellarTheme.TEXT_SECONDARY;
        g2.setColor(iconColor);

        // Center an 18px glyph (in the 24x24 source coordinate space) inside the 32px circle.
        float glyphSize = 18f;
        float scale = glyphSize / VIEWBOX;
        float offset = (SIZE - glyphSize) / 2f;
        AffineTransform at = new AffineTransform();
        at.translate(offset, offset);
        at.scale(scale, scale);

        switch (kind) {
            case DISCORD -> g2.fill(at.createTransformedShape(DISCORD_SHAPE));
            case GITHUB -> g2.fill(at.createTransformedShape(GITHUB_SHAPE));
            case KOFI -> g2.fill(at.createTransformedShape(KOFI_SHAPE));
            case WIKI -> paintWiki(g2, at);
            case ISSUES -> paintIssues(g2, at);
            case WEBSITE -> paintWebsite(g2, at);
        }

        g2.dispose();
    }

    private void paintWiki(Graphics2D g2, AffineTransform at) {
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Rectangle2D.Double cover = new Rectangle2D.Double(3, 4, 18, 16);
        Line2D.Double spine = new Line2D.Double(12, 4, 12, 20);
        g2.draw(at.createTransformedShape(roundedRect(cover, 2)));
        g2.draw(at.createTransformedShape(spine));
    }

    private void paintIssues(Graphics2D g2, AffineTransform at) {
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Ellipse2D.Double outer = new Ellipse2D.Double(2.5, 2.5, 19, 19);
        Line2D.Double stem = new Line2D.Double(12, 7, 12, 13.5);
        Ellipse2D.Double dot = new Ellipse2D.Double(11.25, 16, 1.5, 1.5);
        g2.draw(at.createTransformedShape(outer));
        g2.draw(at.createTransformedShape(stem));
        g2.fill(at.createTransformedShape(dot));
    }

    private void paintWebsite(Graphics2D g2, AffineTransform at) {
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Ellipse2D.Double outer = new Ellipse2D.Double(2.5, 2.5, 19, 19);
        Ellipse2D.Double meridian = new Ellipse2D.Double(8, 2.5, 8, 19);
        Line2D.Double equator = new Line2D.Double(2.5, 12, 21.5, 12);
        g2.draw(at.createTransformedShape(outer));
        g2.draw(at.createTransformedShape(meridian));
        g2.draw(at.createTransformedShape(equator));
    }

    private static Path2D.Double roundedRect(Rectangle2D.Double r, double arc) {
        Path2D.Double p = new Path2D.Double();
        java.awt.geom.RoundRectangle2D.Double rr = new java.awt.geom.RoundRectangle2D.Double(
                r.x, r.y, r.width, r.height, arc, arc);
        p.append(rr, false);
        return p;
    }
}
