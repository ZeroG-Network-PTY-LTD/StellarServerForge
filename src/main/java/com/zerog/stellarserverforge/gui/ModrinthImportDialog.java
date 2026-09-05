package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.gui.theme.StellarButton;
import com.zerog.stellarserverforge.gui.theme.StellarLabels;
import com.zerog.stellarserverforge.gui.theme.StellarTheme;
import com.zerog.stellarserverforge.javamanaged.JavaVersionRules;
import com.zerog.stellarserverforge.model.JavaOverrideMode;
import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ServerSettings;
import com.zerog.stellarserverforge.modrinth.ModrinthModpackImportService;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Imports a Modrinth modpack export ({@code .mrpack} file) — the Modrinth equivalent of {@link
 * CurseForgeImportDialog}, but built around a single portable file rather than scanning a local
 * launcher's instance folder, since that's the format Modrinth actually publishes modpacks in.
 */
public class ModrinthImportDialog extends JDialog {

    private final AppContext ctx;
    private final Consumer<ServerSettings> onImported;

    private final JLabel statusLabel = StellarLabels.muted(" ");
    private final JLabel nameLabel = StellarLabels.body("No .mrpack file selected yet.");
    private final JLabel detailLabel = StellarLabels.muted("");
    private final StellarButton browseButton = new StellarButton("Browse for .mrpack file...", StellarButton.Variant.SECONDARY);
    private final StellarButton importButton = new StellarButton("Import", StellarButton.Variant.PRIMARY);

    private Path mrpackFile;
    private ModrinthModpackImportService.ParsedModpack parsed;

    public ModrinthImportDialog(Frame owner, AppContext ctx, Consumer<ServerSettings> onImported) {
        super(owner, "Import Modrinth modpack", true);
        this.ctx = ctx;
        this.onImported = onImported;

        getContentPane().setBackground(StellarTheme.BG);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(StellarLabels.heading("Import Modrinth modpack"), BorderLayout.NORTH);
        JTextArea caption = new JTextArea("Pick a .mrpack file (downloaded from a modpack's Modrinth page) — its "
                + "mods and overrides will be downloaded/extracted into this server folder.", 2, 60);
        caption.setEditable(false);
        caption.setFocusable(false);
        caption.setOpaque(false);
        caption.setLineWrap(true);
        caption.setWrapStyleWord(true);
        caption.setFont(StellarTheme.FONT_CAPTION);
        caption.setForeground(StellarTheme.TEXT_SECONDARY);
        north.add(caption, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_11, 0, StellarTheme.SPACE_11, 0));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(nameLabel);
        center.add(Box.createVerticalStrut(StellarTheme.SPACE_6));
        center.add(detailLabel);
        center.add(Box.createVerticalStrut(StellarTheme.SPACE_11));
        center.add(statusLabel);
        add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        StellarButton cancelButton = new StellarButton("Cancel", StellarButton.Variant.GHOST);
        buttons.add(browseButton);
        buttons.add(importButton);
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);

        importButton.setEnabled(false);
        browseButton.addActionListener(e -> browse());
        importButton.addActionListener(e -> doImport());
        cancelButton.addActionListener(e -> dispose());

        setSize(520, 320);
        setMinimumSize(new Dimension(460, 280));
        setLocationRelativeTo(owner);
    }

    private void browse() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("Modrinth modpack (.mrpack)", "mrpack"));
        chooser.setDialogTitle("Select a .mrpack file");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path chosen = chooser.getSelectedFile().toPath();
        try {
            ModrinthModpackImportService.ParsedModpack result = ctx.modrinthModpackImportService.parse(chosen);
            mrpackFile = chosen;
            parsed = result;
            nameLabel.setText(result.name());
            detailLabel.setText(result.minecraftVersion() + " · " + result.modLoader() + " " + result.modLoaderVersion()
                    + " · " + result.serverFiles().size() + " file(s) to download");
            statusLabel.setText(" ");
            importButton.setEnabled(true);
        } catch (IOException e) {
            mrpackFile = null;
            parsed = null;
            importButton.setEnabled(false);
            nameLabel.setText("No .mrpack file selected yet.");
            detailLabel.setText("");
            statusLabel.setText("Could not read that file: " + e.getMessage());
        }
    }

    private void doImport() {
        if (mrpackFile == null || parsed == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Import \"" + parsed.name() + "\" (" + parsed.minecraftVersion() + ", " + parsed.modLoader() + " "
                        + parsed.modLoaderVersion() + ") into this server folder? " + parsed.serverFiles().size()
                        + " file(s) will be downloaded, and any existing files at the same paths will be overwritten.",
                "Confirm import", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        setControlsEnabled(false);
        statusLabel.setText("Downloading 0/" + parsed.serverFiles().size() + "...");

        new SwingWorker<Void, Void>() {
            Exception failure;

            @Override
            protected Void doInBackground() {
                try {
                    ctx.modrinthModpackImportService.importInto(mrpackFile, parsed, ctx.serverDir,
                            (done, total) -> SwingUtilities.invokeLater(
                                    () -> statusLabel.setText("Downloading " + done + "/" + total + "...")));
                } catch (Exception e) {
                    failure = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setControlsEnabled(true);
                if (failure != null) {
                    statusLabel.setText("Import failed: " + failure.getMessage());
                    return;
                }

                ServerSettings settings = new ServerSettings();
                settings.setMinecraftVersion(parsed.minecraftVersion());
                settings.setModLoader(parsed.modLoader());
                settings.setModLoaderVersion(parsed.modLoaderVersion());
                settings.setJavaVersion(JavaVersionRules.resolve(McVersion.parse(parsed.minecraftVersion())).defaultVersion());
                settings.setJavaOverrideMode(JavaOverrideMode.AUTOMATIC);

                onImported.accept(settings);
                statusLabel.setText("Imported successfully.");
                dispose();
            }
        }.execute();
    }

    private void setControlsEnabled(boolean enabled) {
        browseButton.setEnabled(enabled);
        importButton.setEnabled(enabled && parsed != null);
    }
}
