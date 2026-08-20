package com.zerog.stellarserverforge.gui.theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Random;

/**
 * A deep-space backdrop: a dark vertical gradient, a couple of soft nebula glows, and a field of
 * twinkling stars. Used as the content background behind every screen so the app reads as one
 * consistent "Stellar" identity rather than default grey Swing panels.
 */
public class StarfieldPanel extends JPanel {

    private record Star(float xFrac, float yFrac, float radius, float baseAlpha, float twinkleSpeed, float twinklePhase) {
    }

    private final Star[] stars;
    private final javax.swing.Timer twinkleTimer;
    private long tick;

    public StarfieldPanel() {
        this(new java.awt.BorderLayout());
    }

    public StarfieldPanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        Random random = new Random(42);
        stars = new Star[140];
        for (int i = 0; i < stars.length; i++) {
            stars[i] = new Star(
                    random.nextFloat(), random.nextFloat(),
                    0.6f + random.nextFloat() * 1.6f,
                    0.25f + random.nextFloat() * 0.55f,
                    0.5f + random.nextFloat() * 1.5f,
                    random.nextFloat() * (float) (Math.PI * 2));
        }
        twinkleTimer = new javax.swing.Timer(90, e -> {
            tick++;
            repaint();
        });
        twinkleTimer.start();
    }

    @Override
    public void removeNotify() {
        twinkleTimer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setPaint(new java.awt.GradientPaint(0, 0, StellarTheme.VOID_BLACK, 0, h, StellarTheme.DEEP_SPACE));
        g2.fillRect(0, 0, w, h);

        paintNebula(g2, w * 0.18f, h * 0.15f, Math.max(w, h) * 0.55f, StellarTheme.NEBULA_PURPLE, 0.30f);
        paintNebula(g2, w * 0.85f, h * 0.75f, Math.max(w, h) * 0.5f, StellarTheme.NEBULA_BLUE, 0.35f);

        double time = tick * 0.09;
        for (Star star : stars) {
            float x = star.xFrac() * w;
            float y = star.yFrac() * h;
            double twinkle = 0.5 + 0.5 * Math.sin(time * star.twinkleSpeed() + star.twinklePhase());
            float alpha = (float) Math.max(0.05, Math.min(1.0, star.baseAlpha() + twinkle * 0.4));
            g2.setColor(withAlpha(StellarTheme.TEXT_PRIMARY, alpha));
            g2.fill(new Ellipse2D.Float(x, y, star.radius(), star.radius()));
        }

        g2.dispose();
    }

    private void paintNebula(Graphics2D g2, float cx, float cy, float radius, Color color, float peakAlpha) {
        if (radius <= 0) {
            return;
        }
        RadialGradientPaint paint = new RadialGradientPaint(
                new Point2D.Float(cx, cy), radius,
                new float[]{0f, 1f},
                new Color[]{withAlpha(color, peakAlpha), withAlpha(color, 0f)});
        g2.setPaint(paint);
        g2.fill(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2));
    }

    private static Color withAlpha(Color c, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
