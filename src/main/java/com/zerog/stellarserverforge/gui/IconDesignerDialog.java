package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.gui.theme.StellarButton;
import com.zerog.stellarserverforge.gui.theme.StellarLabels;
import com.zerog.stellarserverforge.gui.theme.StellarTheme;
import com.zerog.stellarserverforge.utility.IconGeneratorService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Replaces the old "background color chooser -> text color chooser -> text input" chain of three
 * sequential blocking dialogs with a single dialog that shows a live preview as each value
 * changes, before anything is written to {@code server-icon.png}.
 */
public class IconDesignerDialog extends JDialog {

    private final IconGeneratorService iconGeneratorService;
    private final Path serverDir;
    private final Consumer<String> onResult;

    private Color background = Color.BLUE;
    private Color textColor = Color.YELLOW;

    private final JLabel previewLabel = new JLabel();
    private final JTextField textField = new JTextField(10);
    private final JLabel charCountLabel = StellarLabels.muted("0 / 10");
    private final JButton backgroundSwatch = swatchButton();
    private final JButton textSwatch = swatchButton();

    public IconDesignerDialog(Frame owner, IconGeneratorService iconGeneratorService, Path serverDir,
                               Consumer<String> onResult) {
        super(owner, "Design server icon", true);
        this.iconGeneratorService = iconGeneratorService;
        this.serverDir = serverDir;
        this.onResult = onResult;

        getContentPane().setBackground(StellarTheme.BG);
        setLayout(new BorderLayout(StellarTheme.SPACE_17, StellarTheme.SPACE_17));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(
                StellarTheme.SPACE_17, StellarTheme.SPACE_17, StellarTheme.SPACE_17, StellarTheme.SPACE_17));

        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewLabel.setBorder(BorderFactory.createLineBorder(StellarTheme.NEUTRAL_800));
        previewLabel.setPreferredSize(new Dimension(128, 128));
        JPanel previewWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        previewWrap.setOpaque(false);
        previewWrap.add(previewLabel);
        add(previewWrap, BorderLayout.WEST);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(StellarLabels.kicker("BACKGROUND COLOR"));
        form.add(rowOf(backgroundSwatch, StellarLabels.muted("Click to choose")));
        form.add(Box.createVerticalStrut(StellarTheme.SPACE_11));

        form.add(StellarLabels.kicker("TEXT COLOR"));
        form.add(rowOf(textSwatch, StellarLabels.muted("Click to choose")));
        form.add(Box.createVerticalStrut(StellarTheme.SPACE_11));

        form.add(StellarLabels.kicker("TEXT (OPTIONAL, MAX 10 CHARACTERS)"));
        form.add(rowOf(textField, charCountLabel));
        form.add(Box.createVerticalStrut(StellarTheme.SPACE_11));
        form.add(StellarLabels.muted("Leave blank to generate a plain color swatch with no text."));

        add(form, BorderLayout.CENTER);

        StellarButton generateButton = new StellarButton("Generate", StellarButton.Variant.PRIMARY);
        StellarButton cancelButton = new StellarButton("Cancel", StellarButton.Variant.GHOST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, StellarTheme.SPACE_8, 0));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(generateButton);
        add(buttons, BorderLayout.SOUTH);

        backgroundSwatch.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Background color", background);
            if (chosen != null) {
                background = chosen;
                backgroundSwatch.setBackground(background);
                refreshPreview();
            }
        });
        textSwatch.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Text color", textColor);
            if (chosen != null) {
                textColor = chosen;
                textSwatch.setBackground(textColor);
                refreshPreview();
            }
        });
        textField.getDocument().addDocumentListener((SimpleDocumentListener) this::onTextChanged);
        generateButton.addActionListener(e -> generate());
        cancelButton.addActionListener(e -> dispose());

        backgroundSwatch.setBackground(background);
        textSwatch.setBackground(textColor);
        refreshPreview();

        setSize(460, 300);
        setMinimumSize(new Dimension(420, 280));
        setLocationRelativeTo(owner);
    }

    private static JButton swatchButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(48, 28));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setBorder(BorderFactory.createLineBorder(StellarTheme.NEUTRAL_800));
        return button;
    }

    private JPanel rowOf(JComponent first, JComponent second) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(first);
        row.add(second);
        return row;
    }

    private void onTextChanged() {
        String text = textField.getText();
        if (text.length() > 10) {
            text = text.substring(0, 10);
            textField.setText(text);
            return; // setText re-triggers this listener with the truncated value.
        }
        charCountLabel.setText(text.length() + " / 10");
        refreshPreview();
    }

    private void refreshPreview() {
        BufferedImage image = iconGeneratorService.renderCustom(background, textColor, textField.getText());
        Image scaled = image.getScaledInstance(96, 96, Image.SCALE_SMOOTH);
        previewLabel.setIcon(new ImageIcon(scaled));
    }

    private void generate() {
        try {
            iconGeneratorService.generateCustom(serverDir, background, textColor, textField.getText());
            onResult.accept("Generated custom server-icon.png.");
            dispose();
        } catch (IOException ex) {
            onResult.accept("Failed to generate icon: " + ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface SimpleDocumentListener extends DocumentListener {
        void update();

        @Override
        default void insertUpdate(DocumentEvent e) {
            update();
        }

        @Override
        default void removeUpdate(DocumentEvent e) {
            update();
        }

        @Override
        default void changedUpdate(DocumentEvent e) {
            update();
        }
    }
}
