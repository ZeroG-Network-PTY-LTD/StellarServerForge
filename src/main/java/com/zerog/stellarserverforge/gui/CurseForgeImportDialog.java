package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.curseforge.CurseForgeImportService;
import com.zerog.stellarserverforge.gui.theme.StellarButton;
import com.zerog.stellarserverforge.gui.theme.StellarLabels;
import com.zerog.stellarserverforge.gui.theme.StellarTheme;
import com.zerog.stellarserverforge.model.JavaOverrideMode;
import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.model.ServerSettings;
import com.zerog.stellarserverforge.javamanaged.JavaVersionRules;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * CurseForge profile import (spec §11): finds the CurseForge app's instance folder (via the
 * Windows registry, or a manual folder pick as the cross-platform fallback), lets the user pick
 * a profile, and imports its files + settings into the current server directory.
 */
public class CurseForgeImportDialog extends JDialog {

    private final AppContext ctx;
    private final Consumer<ServerSettings> onImported;

    private final DefaultListModel<CurseForgeImportService.ProfileInfo> model = new DefaultListModel<>();
    private final JList<CurseForgeImportService.ProfileInfo> list = new JList<>(model);
    private final JLabel statusLabel = StellarLabels.muted(" ");

    public CurseForgeImportDialog(Frame owner, AppContext ctx, Consumer<ServerSettings> onImported) {
        super(owner, "Import CurseForge profile", true);
        this.ctx = ctx;
        this.onImported = onImported;

        getContentPane().setBackground(StellarTheme.BG);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        list.setBackground(StellarTheme.SURFACE);
        list.setForeground(StellarTheme.TEXT_PRIMARY);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jList, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                CurseForgeImportService.ProfileInfo info = (CurseForgeImportService.ProfileInfo) value;
                return super.getListCellRendererComponent(jList, info.folderName(), index, isSelected, cellHasFocus);
            }
        });
        add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(StellarLabels.heading("Import CurseForge profile"), BorderLayout.NORTH);
        north.add(statusLabel, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        StellarButton browseButton = new StellarButton("Browse for instances folder", StellarButton.Variant.SECONDARY);
        StellarButton importButton = new StellarButton("Import selected", StellarButton.Variant.PRIMARY);
        StellarButton cancelButton = new StellarButton("Cancel", StellarButton.Variant.GHOST);
        buttons.add(browseButton);
        buttons.add(importButton);
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);

        browseButton.addActionListener(e -> browseManually());
        importButton.addActionListener(e -> importSelected());
        cancelButton.addActionListener(e -> dispose());

        setSize(560, 420);
        setMinimumSize(new Dimension(460, 340));
        setLocationRelativeTo(owner);

        autoDetect();
    }

    private void autoDetect() {
        Path root = ctx.curseForgeImportService.findCurseForgeRoot();
        if (root == null) {
            statusLabel.setText("No CurseForge installation was auto-detected — browse for its Instances folder.");
            return;
        }
        loadProfiles(root);
    }

    private void browseManually() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select the CurseForge minecraft root folder (containing an Instances folder)");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadProfiles(chooser.getSelectedFile().toPath());
        }
    }

    private void loadProfiles(Path root) {
        try {
            List<CurseForgeImportService.ProfileInfo> profiles = ctx.curseForgeImportService.listProfiles(root);
            model.clear();
            profiles.forEach(model::addElement);
            statusLabel.setText(profiles.isEmpty()
                    ? "No profiles with a minecraftinstance.json were found under " + root
                    : "Found " + profiles.size() + " profile(s) under " + root);
        } catch (IOException e) {
            statusLabel.setText("Could not read that folder: " + e.getMessage());
        }
    }

    private void importSelected() {
        CurseForgeImportService.ProfileInfo selected = list.getSelectedValue();
        if (selected == null) {
            statusLabel.setText("Select a profile first.");
            return;
        }

        try {
            CurseForgeImportService.ParsedProfile parsed = ctx.curseForgeImportService.parseProfile(selected.path());

            // Validate everything BEFORE the destructive step below (importInto replaces existing
            // mods/config/etc.) — a profile with an unrecognized modloader name or an unparseable
            // Minecraft version must fail here, not after files have already been overwritten.
            ModLoader loader;
            McVersion mc;
            try {
                loader = ModLoader.valueOf(parsed.modLoaderName().toUpperCase());
                mc = McVersion.parse(parsed.minecraftVersion());
            } catch (IllegalArgumentException e) {
                statusLabel.setText("Unrecognized modloader type or Minecraft version in this profile — import cancelled.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Import \"" + parsed.displayName() + "\" (" + parsed.minecraftVersion() + ", "
                            + parsed.modLoaderName() + " " + parsed.modLoaderVersion() + ") into this server folder? "
                            + "Existing mods/config/etc. will be replaced.",
                    "Confirm import", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            ctx.curseForgeImportService.importInto(selected.path(), ctx.serverDir);

            ServerSettings settings = new ServerSettings();
            settings.setMinecraftVersion(parsed.minecraftVersion());
            settings.setModLoader(loader);
            settings.setModLoaderVersion(parsed.modLoaderVersion());
            settings.setJavaVersion(JavaVersionRules.resolve(mc).defaultVersion());
            settings.setJavaOverrideMode(JavaOverrideMode.AUTOMATIC);

            onImported.accept(settings);
            statusLabel.setText("Imported successfully.");
            dispose();
        } catch (IOException e) {
            statusLabel.setText("Import failed: " + e.getMessage());
        }
    }
}
