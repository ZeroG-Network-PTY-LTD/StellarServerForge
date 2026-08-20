package com.zerog.stellarserverforge.utility;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates {@code server-icon.png} (spec §3.5), replacing PowerShell's {@code System.Drawing}
 * usage with {@code Graphics2D}/{@code BufferedImage}/{@code ImageIO} — a close 1:1 mapping.
 */
public class IconGeneratorService {

    private static final int SIZE = 64;

    public void generateDefault(Path serverDir) throws IOException {
        BufferedImage image = swatch(new Color(0x1E, 0x3A, 0x8A));
        drawCenteredText(image, "SSF", Color.YELLOW, false);
        save(serverDir, image);
    }

    public void generateCustom(Path serverDir, Color background, Color textColor, String customText) throws IOException {
        BufferedImage image = swatch(background);
        if (customText != null && !customText.isBlank()) {
            drawCenteredText(image, customText.length() > 10 ? customText.substring(0, 10) : customText, textColor, true);
        }
        save(serverDir, image);
    }

    private BufferedImage swatch(Color background) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(background);
        g.fillRect(0, 0, SIZE, SIZE);
        g.dispose();
        return image;
    }

    private void drawCenteredText(BufferedImage image, String text, Color textColor, boolean diagonal) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(textColor);

        int fontSize = fitFontSize(g, text, 85);
        Font font = new Font(Font.MONOSPACED, Font.BOLD, fontSize);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();

        if (diagonal) {
            g.rotate(Math.toRadians(-45), SIZE / 2.0, SIZE / 2.0);
        }
        g.drawString(text, (SIZE - textWidth) / 2f, (SIZE + textHeight) / 2f - fm.getDescent());
        g.dispose();
    }

    private int fitFontSize(Graphics2D g, String text, int maxWidthPx) {
        int size = 30;
        while (size > 6) {
            Font font = new Font(Font.MONOSPACED, Font.BOLD, size);
            FontMetrics fm = g.getFontMetrics(font);
            if (fm.stringWidth(text) <= maxWidthPx) {
                break;
            }
            size--;
        }
        return size;
    }

    private void save(Path serverDir, BufferedImage image) throws IOException {
        Path target = serverDir.resolve("server-icon.png");
        if (Files.exists(target)) {
            int n = 1;
            while (Files.exists(serverDir.resolve("server-icon" + n + ".png"))) {
                n++;
            }
            Files.move(target, serverDir.resolve("server-icon" + n + ".png"));
        }
        ImageIO.write(image, "png", target.toFile());
    }
}
