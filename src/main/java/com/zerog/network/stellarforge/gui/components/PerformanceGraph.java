package com.zerog.network.stellarforge.gui.components;

import com.zerog.network.stellarforge.model.ServerMetrics;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A lightweight live-updating line graph that shows scrolling performance data.
 * Supports up to two series: TPS (green) and RAM% (blue).
 */
public class PerformanceGraph extends JPanel {

    private static final int MAX_POINTS = 60; // 60 snapshots visible
    private static final Color BG          = new Color(20, 20, 25);
    private static final Color GRID        = new Color(50, 50, 60);
    private static final Color TPS_COLOR   = new Color(0, 200, 100);
    private static final Color RAM_COLOR   = new Color(60, 150, 255);
    private static final Color TEXT_COLOR  = new Color(180, 180, 190);

    private final List<Double> tpsSeries  = new ArrayList<>();
    private final List<Double> ramSeries  = new ArrayList<>();

    private boolean showTps = true;
    private boolean showRam = true;

    public PerformanceGraph() {
        setBackground(BG);
        setPreferredSize(new Dimension(300, 100));
        setBorder(BorderFactory.createLineBorder(new Color(60, 60, 75), 1));
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Push a new metrics snapshot; repaints the graph */
    public void addMetrics(ServerMetrics m) {
        // TPS capped at 20, scale to 0-100
        double tpsPct = Math.min(m.getTps1m() / 20.0, 1.0) * 100.0;
        tpsSeries.add(tpsPct);
        ramSeries.add((double) m.memPercent());

        while (tpsSeries.size() > MAX_POINTS) tpsSeries.remove(0);
        while (ramSeries.size() > MAX_POINTS) ramSeries.remove(0);

        repaint();
    }

    public void setShowTps(boolean v) { this.showTps = v; repaint(); }
    public void setShowRam(boolean v) { this.showRam = v; repaint(); }

    // ── Painting ──────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int padL = 30, padR = 8, padT = 8, padB = 18;
        int graphW = w - padL - padR;
        int graphH = h - padT - padB;

        // Grid lines (0%, 25%, 50%, 75%, 100%)
        g2.setColor(GRID);
        for (int pct = 0; pct <= 100; pct += 25) {
            int y = padT + (int) ((1.0 - pct / 100.0) * graphH);
            g2.drawLine(padL, y, w - padR, y);
            g2.setColor(TEXT_COLOR);
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
            g2.drawString(pct + "%", 1, y + 4);
            g2.setColor(GRID);
        }

        // Axes
        g2.setColor(new Color(80, 80, 90));
        g2.drawLine(padL, padT, padL, h - padB);
        g2.drawLine(padL, h - padB, w - padR, h - padB);

        // Series
        if (showTps && tpsSeries.size() > 1) {
            drawSeries(g2, tpsSeries, TPS_COLOR, padL, padT, graphW, graphH);
        }
        if (showRam && ramSeries.size() > 1) {
            drawSeries(g2, ramSeries, RAM_COLOR, padL, padT, graphW, graphH);
        }

        // Legend
        int lx = padL + 4; int ly = padT + 10;
        if (showTps) {
            g2.setColor(TPS_COLOR);
            g2.fillRect(lx, ly - 7, 10, 3);
            g2.setColor(TEXT_COLOR);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
            g2.drawString("TPS", lx + 12, ly);
            lx += 36;
        }
        if (showRam) {
            g2.setColor(RAM_COLOR);
            g2.fillRect(lx, ly - 7, 10, 3);
            g2.setColor(TEXT_COLOR);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
            g2.drawString("RAM", lx + 12, ly);
        }
    }

    private void drawSeries(Graphics2D g2, List<Double> series, Color color,
                            int ox, int oy, int gw, int gh) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.5f));

        int n = series.size();
        int prevX = -1, prevY = -1;
        for (int i = 0; i < n; i++) {
            int x = ox + (int) ((double) i / (MAX_POINTS - 1) * gw);
            int y = oy + (int) ((1.0 - series.get(i) / 100.0) * gh);
            if (prevX >= 0) g2.drawLine(prevX, prevY, x, y);
            prevX = x; prevY = y;
        }
    }
}
