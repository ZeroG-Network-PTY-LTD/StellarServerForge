package com.zerog.stellarserverforge.gui.theme;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A minimal parser for the SVG {@code <path d="...">} mini-language, supporting the commands
 * used by real-world brand icon sets (M/L/H/V/C/S/Q/T/A/Z, both absolute and relative). Used to
 * render the footer's brand logos from their original vector path data instead of a raster or an
 * emoji glyph, without pulling in a full SVG rendering library for a handful of static icons.
 */
final class SvgPath {

    private SvgPath() {
    }

    private static final Pattern TOKEN = Pattern.compile("[+-]?(?:\\d+\\.\\d+|\\.\\d+|\\d+)(?:[eE][+-]?\\d+)?");

    /** Mutable parse-time cursor state — one instance per {@link #parse}, never shared. */
    private static final class State {
        double cx, cy, sx, sy;
        double lastCubicCx, lastCubicCy;
        double lastQuadCx, lastQuadCy;
        char lastCommand = ' ';
    }

    static Path2D.Double parse(String d) {
        Path2D.Double path = new Path2D.Double();
        State st = new State();

        List<String> tokens = new ArrayList<>();
        char command = ' ';
        int i = 0;
        int len = d.length();
        while (i < len) {
            char c = d.charAt(i);
            if (Character.isLetter(c)) {
                if (command != ' ') {
                    apply(path, st, command, tokens);
                }
                command = c;
                tokens.clear();
                i++;
            } else if (Character.isWhitespace(c) || c == ',') {
                i++;
            } else {
                // Elliptical-arc flags (large-arc-flag, sweep-flag — argument positions 3 and 4 of
                // each 7-value A/a group) are single 0/1 digits and may run into the next number
                // with no separator (e.g. "00-4.8851"), so they can't use the general number regex.
                boolean isArcFlagSlot = (Character.toUpperCase(command) == 'A') && (tokens.size() % 7 == 3 || tokens.size() % 7 == 4);
                if (isArcFlagSlot && (c == '0' || c == '1')) {
                    tokens.add(String.valueOf(c));
                    i++;
                    continue;
                }
                Matcher m = TOKEN.matcher(d).region(i, len);
                if (m.lookingAt()) {
                    tokens.add(m.group());
                    i = m.end();
                } else {
                    i++;
                }
            }
        }
        if (command != ' ') {
            apply(path, st, command, tokens);
        }
        return path;
    }

    private static void apply(Path2D.Double path, State st, char command, List<String> tokens) {
        double[] n = tokens.stream().mapToDouble(Double::parseDouble).toArray();
        boolean relative = Character.isLowerCase(command);
        char cmd = Character.toUpperCase(command);
        int argCount = switch (cmd) {
            case 'M', 'L', 'T' -> 2;
            case 'H', 'V' -> 1;
            case 'C' -> 6;
            case 'S', 'Q' -> 4;
            case 'A' -> 7;
            case 'Z' -> 0;
            default -> 0;
        };

        if (cmd == 'Z') {
            path.closePath();
            st.cx = st.sx;
            st.cy = st.sy;
            st.lastCommand = cmd;
            return;
        }

        if (argCount == 0) {
            return;
        }

        for (int off = 0; off + argCount <= n.length; off += argCount) {
            switch (cmd) {
                case 'M' -> {
                    double x = n[off] + (relative ? st.cx : 0);
                    double y = n[off + 1] + (relative ? st.cy : 0);
                    if (off == 0) {
                        path.moveTo(x, y);
                        st.sx = x;
                        st.sy = y;
                    } else {
                        path.lineTo(x, y);
                    }
                    st.cx = x;
                    st.cy = y;
                }
                case 'L' -> {
                    double x = n[off] + (relative ? st.cx : 0);
                    double y = n[off + 1] + (relative ? st.cy : 0);
                    path.lineTo(x, y);
                    st.cx = x;
                    st.cy = y;
                }
                case 'H' -> {
                    double x = n[off] + (relative ? st.cx : 0);
                    path.lineTo(x, st.cy);
                    st.cx = x;
                }
                case 'V' -> {
                    double y = n[off] + (relative ? st.cy : 0);
                    path.lineTo(st.cx, y);
                    st.cy = y;
                }
                case 'C' -> {
                    double x1 = n[off] + (relative ? st.cx : 0);
                    double y1 = n[off + 1] + (relative ? st.cy : 0);
                    double x2 = n[off + 2] + (relative ? st.cx : 0);
                    double y2 = n[off + 3] + (relative ? st.cy : 0);
                    double x = n[off + 4] + (relative ? st.cx : 0);
                    double y = n[off + 5] + (relative ? st.cy : 0);
                    path.curveTo(x1, y1, x2, y2, x, y);
                    st.lastCubicCx = x2;
                    st.lastCubicCy = y2;
                    st.cx = x;
                    st.cy = y;
                }
                case 'S' -> {
                    double x1 = (st.lastCommand == 'C' || st.lastCommand == 'S') ? 2 * st.cx - st.lastCubicCx : st.cx;
                    double y1 = (st.lastCommand == 'C' || st.lastCommand == 'S') ? 2 * st.cy - st.lastCubicCy : st.cy;
                    double x2 = n[off] + (relative ? st.cx : 0);
                    double y2 = n[off + 1] + (relative ? st.cy : 0);
                    double x = n[off + 2] + (relative ? st.cx : 0);
                    double y = n[off + 3] + (relative ? st.cy : 0);
                    path.curveTo(x1, y1, x2, y2, x, y);
                    st.lastCubicCx = x2;
                    st.lastCubicCy = y2;
                    st.cx = x;
                    st.cy = y;
                }
                case 'Q' -> {
                    double x1 = n[off] + (relative ? st.cx : 0);
                    double y1 = n[off + 1] + (relative ? st.cy : 0);
                    double x = n[off + 2] + (relative ? st.cx : 0);
                    double y = n[off + 3] + (relative ? st.cy : 0);
                    path.quadTo(x1, y1, x, y);
                    st.lastQuadCx = x1;
                    st.lastQuadCy = y1;
                    st.cx = x;
                    st.cy = y;
                }
                case 'T' -> {
                    double x1 = (st.lastCommand == 'Q' || st.lastCommand == 'T') ? 2 * st.cx - st.lastQuadCx : st.cx;
                    double y1 = (st.lastCommand == 'Q' || st.lastCommand == 'T') ? 2 * st.cy - st.lastQuadCy : st.cy;
                    double x = n[off] + (relative ? st.cx : 0);
                    double y = n[off + 1] + (relative ? st.cy : 0);
                    path.quadTo(x1, y1, x, y);
                    st.lastQuadCx = x1;
                    st.lastQuadCy = y1;
                    st.cx = x;
                    st.cy = y;
                }
                case 'A' -> {
                    double rx = n[off];
                    double ry = n[off + 1];
                    double xAxisRot = n[off + 2];
                    boolean largeArc = n[off + 3] != 0;
                    boolean sweep = n[off + 4] != 0;
                    double x = n[off + 5] + (relative ? st.cx : 0);
                    double y = n[off + 6] + (relative ? st.cy : 0);
                    arcTo(path, st.cx, st.cy, rx, ry, xAxisRot, largeArc, sweep, x, y);
                    st.cx = x;
                    st.cy = y;
                }
                default -> {
                }
            }
            st.lastCommand = cmd;
        }
    }

    /** Endpoint-to-center arc parameterization (SVG spec Appendix F.6), approximated with cubic Beziers. */
    private static void arcTo(Path2D.Double path, double x0, double y0, double rx, double ry,
                               double xAxisRotDeg, boolean largeArc, boolean sweep, double x, double y) {
        if (rx == 0 || ry == 0 || (x0 == x && y0 == y)) {
            path.lineTo(x, y);
            return;
        }
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        double phi = Math.toRadians(xAxisRotDeg % 360);
        double cosPhi = Math.cos(phi), sinPhi = Math.sin(phi);

        double dx2 = (x0 - x) / 2.0, dy2 = (y0 - y) / 2.0;
        double x1p = cosPhi * dx2 + sinPhi * dy2;
        double y1p = -sinPhi * dx2 + cosPhi * dy2;

        double rxSq = rx * rx, rySq = ry * ry;
        double x1pSq = x1p * x1p, y1pSq = y1p * y1p;
        double radiiCheck = x1pSq / rxSq + y1pSq / rySq;
        if (radiiCheck > 1) {
            double scale = Math.sqrt(radiiCheck);
            rx *= scale;
            ry *= scale;
            rxSq = rx * rx;
            rySq = ry * ry;
        }

        double sign = (largeArc != sweep) ? 1 : -1;
        double num = rxSq * rySq - rxSq * y1pSq - rySq * x1pSq;
        double den = rxSq * y1pSq + rySq * x1pSq;
        double coef = sign * Math.sqrt(Math.max(0, num / den));
        double cxp = coef * (rx * y1p / ry);
        double cyp = coef * -(ry * x1p / rx);

        double cxCenter = cosPhi * cxp - sinPhi * cyp + (x0 + x) / 2.0;
        double cyCenter = sinPhi * cxp + cosPhi * cyp + (y0 + y) / 2.0;

        double ux = (x1p - cxp) / rx, uy = (y1p - cyp) / ry;
        double vx = (-x1p - cxp) / rx, vy = (-y1p - cyp) / ry;
        double startAngle = angleBetween(1, 0, ux, uy);
        double deltaAngle = angleBetween(ux, uy, vx, vy);
        if (!sweep && deltaAngle > 0) {
            deltaAngle -= 2 * Math.PI;
        } else if (sweep && deltaAngle < 0) {
            deltaAngle += 2 * Math.PI;
        }

        // Approximate the elliptical arc with cubic Beziers directly in path-space (no intermediate
        // conversion to Arc2D's degrees-based convention, which has its own y-axis sign quirks).
        int segments = (int) Math.ceil(Math.abs(deltaAngle) / (Math.PI / 2));
        double segAngle = deltaAngle / segments;
        double alpha = Math.sin(segAngle) * (Math.sqrt(4 + 3 * Math.tan(segAngle / 2) * Math.tan(segAngle / 2)) - 1) / 3;

        double angle = startAngle;
        for (int s = 0; s < segments; s++) {
            double a1 = angle;
            double a2 = angle + segAngle;

            double cosA1 = Math.cos(a1), sinA1 = Math.sin(a1);
            double cosA2 = Math.cos(a2), sinA2 = Math.sin(a2);

            double p1x = cosA1, p1y = sinA1;
            double p2x = cosA2, p2y = sinA2;
            double q1x = p1x - alpha * sinA1, q1y = p1y + alpha * cosA1;
            double q2x = p2x + alpha * sinA2, q2y = p2y - alpha * cosA2;

            path.curveTo(
                    ellipsePointX(q1x, q1y, rx, ry, cosPhi, sinPhi, cxCenter),
                    ellipsePointY(q1x, q1y, rx, ry, cosPhi, sinPhi, cyCenter),
                    ellipsePointX(q2x, q2y, rx, ry, cosPhi, sinPhi, cxCenter),
                    ellipsePointY(q2x, q2y, rx, ry, cosPhi, sinPhi, cyCenter),
                    ellipsePointX(p2x, p2y, rx, ry, cosPhi, sinPhi, cxCenter),
                    ellipsePointY(p2x, p2y, rx, ry, cosPhi, sinPhi, cyCenter));

            angle = a2;
        }
    }

    private static double ellipsePointX(double ux, double uy, double rx, double ry,
                                         double cosPhi, double sinPhi, double cxCenter) {
        return cosPhi * (ux * rx) - sinPhi * (uy * ry) + cxCenter;
    }

    private static double ellipsePointY(double ux, double uy, double rx, double ry,
                                         double cosPhi, double sinPhi, double cyCenter) {
        return sinPhi * (ux * rx) + cosPhi * (uy * ry) + cyCenter;
    }

    private static double angleBetween(double ux, double uy, double vx, double vy) {
        double sign = (ux * vy - uy * vx) < 0 ? -1 : 1;
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        double angle = Math.acos(Math.max(-1, Math.min(1, dot / len)));
        return sign * angle;
    }
}
