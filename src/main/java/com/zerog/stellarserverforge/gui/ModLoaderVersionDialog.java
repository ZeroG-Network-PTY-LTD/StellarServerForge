package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.gui.theme.StellarButton;
import com.zerog.stellarserverforge.gui.theme.StellarLabels;
import com.zerog.stellarserverforge.gui.theme.StellarTheme;
import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.model.ServerSettings;
import com.zerog.stellarserverforge.modloader.ModLoaderVersionResolver;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Lets the user re-point an already-configured modloader server at a different build/version of
 * the same modloader (e.g. switch NeoForge 20.1.176 to 20.1.180) without re-running the whole
 * setup wizard. Mirrors the modloader-version step of {@link SetupWizardPanel}. The next Launch
 * installs the newly chosen version if it isn't already present.
 */
public class ModLoaderVersionDialog extends JDialog {

    private final AppContext ctx;
    private final ServerSettings settings;
    private final Runnable onChanged;
    private final McVersion mc;
    private final ModLoader loader;

    private final JLabel infoLabel = StellarLabels.muted(" ");
    private final JLabel statusLabel = StellarLabels.muted(" ");
    private final JRadioButton useNewestRadio = themedRadio("Use the newest published version", true);
    private final JRadioButton useCustomRadio = themedRadio("Enter a custom version", false);
    private final JTextField customField = themedField(14);
    private final StellarButton applyButton = new StellarButton("Apply", StellarButton.Variant.PRIMARY);
    private final StellarButton cancelButton = new StellarButton("Cancel", StellarButton.Variant.GHOST);

    private String resolvedNewest;

    public ModLoaderVersionDialog(Frame owner, AppContext ctx, ServerSettings settings, Runnable onChanged) {
        super(owner, "Change " + settings.getModLoader() + " version", true);
        this.ctx = ctx;
        this.settings = settings;
        this.onChanged = onChanged;
        this.loader = settings.getModLoader();
        this.mc = McVersion.parse(settings.getMinecraftVersion());

        getContentPane().setBackground(StellarTheme.BG);
        setLayout(new BorderLayout(StellarTheme.SPACE_11, StellarTheme.SPACE_11));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(
                StellarTheme.SPACE_17, StellarTheme.SPACE_17, StellarTheme.SPACE_17, StellarTheme.SPACE_17));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(StellarLabels.heading("Change " + loader + " version"));
        center.add(Box.createVerticalStrut(StellarTheme.SPACE_6));
        center.add(StellarLabels.muted("Currently: " + (settings.getModLoaderVersion().isEmpty()
                ? "(none)" : settings.getModLoaderVersion())));
        center.add(Box.createVerticalStrut(StellarTheme.SPACE_11));
        center.add(infoLabel);
        center.add(Box.createVerticalStrut(StellarTheme.SPACE_8));

        ButtonGroup group = new ButtonGroup();
        group.add(useNewestRadio);
        group.add(useCustomRadio);
        center.add(useNewestRadio);
        JPanel customRow = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_8, 0));
        customRow.setOpaque(false);
        customRow.add(useCustomRadio);
        customRow.add(customField);
        center.add(customRow);
        center.add(statusLabel);
        add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, StellarTheme.SPACE_8, 0));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(applyButton);
        add(buttons, BorderLayout.SOUTH);

        applyButton.addActionListener(e -> apply());
        cancelButton.addActionListener(e -> dispose());

        setSize(460, 260);
        setMinimumSize(new Dimension(400, 240));
        setLocationRelativeTo(owner);

        beginResolution();
    }

    private static JTextField themedField(int columns) {
        JTextField field = new JTextField(columns);
        field.setBackground(StellarTheme.FIELD_BG);
        field.setForeground(StellarTheme.TEXT_PRIMARY);
        field.setCaretColor(StellarTheme.ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(StellarTheme.NEUTRAL_800),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        field.setFont(StellarTheme.FONT_BODY);
        return field;
    }

    private static JRadioButton themedRadio(String text, boolean selected) {
        JRadioButton radio = new JRadioButton(text, selected);
        radio.setOpaque(false);
        radio.setForeground(StellarTheme.TEXT_PRIMARY);
        radio.setFont(StellarTheme.FONT_BODY);
        return radio;
    }

    private void beginResolution() {
        infoLabel.setText("Fetching available " + loader + " versions for " + mc.raw() + "...");
        applyButton.setEnabled(false);
        useNewestRadio.setEnabled(false);
        resolvedNewest = null;

        new SwingWorker<Optional<String>, Void>() {
            @Override
            protected Optional<String> doInBackground() throws Exception {
                var preflight = ctx.networkPreflightService.checkModLoaderAndMojangHosts(loader);
                if (!preflight.ok()) {
                    throw new IOException("Could not resolve: " + String.join(", ", preflight.unresolvedHosts())
                            + " — check your internet connection or try a public DNS server (1.1.1.1 or 8.8.8.8).");
                }
                Path metadataFile = ctx.modLoaderMetadataService.ensureMetadataFile(loader, mc);
                Path promotionsFile = loader == ModLoader.FORGE
                        ? ctx.modLoaderMetadataService.ensurePromotionsFile()
                        : null;
                return ctx.modLoaderVersionResolver.resolveNewest(loader, mc, metadataFile, promotionsFile);
            }

            @Override
            protected void done() {
                applyButton.setEnabled(true);
                try {
                    Optional<String> newest = get();
                    if (newest.isPresent()) {
                        resolvedNewest = newest.get();
                        infoLabel.setText("Newest detected " + loader + " version for " + mc.raw()
                                + ": " + resolvedNewest);
                        useNewestRadio.setText("Use " + resolvedNewest);
                        useNewestRadio.setEnabled(true);
                        useNewestRadio.setSelected(true);
                    } else {
                        infoLabel.setText("Could not auto-detect a newest version — enter one manually.");
                        useCustomRadio.setSelected(true);
                    }
                } catch (Exception ex) {
                    infoLabel.setText("Could not fetch " + loader + " version data (" + rootMessage(ex)
                            + ") — enter a version manually.");
                    useCustomRadio.setSelected(true);
                }
            }
        }.execute();
    }

    private void apply() {
        if (useNewestRadio.isSelected() && resolvedNewest != null) {
            commit(resolvedNewest);
            return;
        }

        String entered = customField.getText().trim();
        if (entered.isEmpty()) {
            setStatus("Enter a version number.", StellarTheme.STATUS_WARNING);
            return;
        }
        if (loader == ModLoader.FORGE && ModLoaderVersionResolver.containsLetters(entered)) {
            setStatus("Forge versions are purely numeric — that doesn't look right.", StellarTheme.STATUS_FAILED);
            return;
        }

        applyButton.setEnabled(false);
        setStatus("Checking that version exists...", StellarTheme.TEXT_SECONDARY);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                Path metadataFile = ctx.modLoaderMetadataService.ensureMetadataFile(loader, mc);
                return ctx.modLoaderVersionResolver.isValidVersion(loader, mc, metadataFile, entered);
            }

            @Override
            protected void done() {
                applyButton.setEnabled(true);
                try {
                    if (get()) {
                        commit(entered);
                    } else {
                        setStatus("That version does not seem to exist on the " + loader
                                + " file server for Minecraft " + mc.raw() + " — try another.",
                                StellarTheme.STATUS_FAILED);
                    }
                } catch (Exception ex) {
                    setStatus("Could not verify that version: " + rootMessage(ex), StellarTheme.STATUS_FAILED);
                }
            }
        }.execute();
    }

    private void commit(String version) {
        settings.setModLoaderVersion(version);
        try {
            ctx.settingsService.save(settings);
        } catch (IOException e) {
            setStatus("Could not save settings.json: " + e.getMessage(), StellarTheme.STATUS_FAILED);
            return;
        }
        onChanged.run();
        dispose();
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.toString();
    }
}
