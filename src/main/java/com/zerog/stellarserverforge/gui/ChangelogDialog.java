package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.gui.theme.StellarButton;
import com.zerog.stellarserverforge.gui.theme.StellarLabels;
import com.zerog.stellarserverforge.gui.theme.StellarTheme;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Shows the bundled {@code CHANGELOG.md} (packaged as a classpath resource — see
 * {@code build.gradle.kts}) so users can see what changed without leaving the app.
 */
public class ChangelogDialog extends JDialog {

    public ChangelogDialog(Frame owner) {
        super(owner, "Changelog", true);

        getContentPane().setBackground(StellarTheme.BG);
        setLayout(new BorderLayout(0, StellarTheme.SPACE_11));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(
                StellarTheme.SPACE_17, StellarTheme.SPACE_17, StellarTheme.SPACE_17, StellarTheme.SPACE_17));

        add(StellarLabels.heading("Changelog"), BorderLayout.NORTH);

        JTextArea textArea = new JTextArea(readChangelog());
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(StellarTheme.FONT_BODY);
        textArea.setBackground(StellarTheme.CONSOLE_BG);
        textArea.setForeground(StellarTheme.NEUTRAL_100);
        textArea.setCaretPosition(0);
        textArea.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_11, StellarTheme.SPACE_11,
                StellarTheme.SPACE_11, StellarTheme.SPACE_11));
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        StellarButton closeButton = new StellarButton("Close", StellarButton.Variant.GHOST);
        closeButton.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(closeButton);
        add(buttons, BorderLayout.SOUTH);

        setSize(560, 480);
        setMinimumSize(new Dimension(420, 320));
        setLocationRelativeTo(owner);
    }

    private static String readChangelog() {
        try (InputStream in = ChangelogDialog.class.getResourceAsStream("/CHANGELOG.md")) {
            if (in == null) {
                return "CHANGELOG.md is not available in this build.";
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append(System.lineSeparator());
                }
            }
            return sb.toString();
        } catch (IOException e) {
            return "Could not read CHANGELOG.md: " + e.getMessage();
        }
    }
}
