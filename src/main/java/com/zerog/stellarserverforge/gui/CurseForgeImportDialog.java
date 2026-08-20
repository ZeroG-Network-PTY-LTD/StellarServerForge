package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.curseforge.CurseForgeImportService;
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
    private final JLabel statusLabel = new JLabel(" ");

    public CurseForgeImportDialog(Frame owner, AppContext ctx, Consumer<ServerSettings> onImported) {
        super(owner, "Import CurseForge Profile", true);
        this.ctx = ctx;
        this.onImported = onImported;

        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jList, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                CurseForgeImportService.ProfileInfo info = (CurseForgeImportService.ProfileInfo) value;
                return super.getListCellRendererComponent(jList, info.folderName(), index, isSelected, cellHasFocus);
            }
        });
        add(new JScrollPane(list), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton browseButton = new JButton("Browse for CurseForge Instances Folder...");
        JButton importButton = new JButton("Import Selected");
        JButton cancelButton = new JButton("Cancel");
        buttons.add(browseButton);
        buttons.add(importButton);
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);

        browseButton.addActionListener(e -> browseManually());
        importButton.addActionListener(e -> importSelected());
        cancelButton.addActionListener(e -> dispose());

        setSize(560, 420);
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
            ModLoader loader = ModLoader.valueOf(parsed.modLoaderName().toUpperCase());
            settings.setModLoader(loader);
            settings.setModLoaderVersion(parsed.modLoaderVersion());
            McVersion mc = McVersion.parse(parsed.minecraftVersion());
            settings.setJavaVersion(JavaVersionRules.resolve(mc).defaultVersion());
            settings.setJavaOverrideMode(JavaOverrideMode.AUTOMATIC);

            onImported.accept(settings);
            statusLabel.setText("Imported successfully.");
            dispose();
        } catch (IllegalArgumentException e) {
            statusLabel.setText("Unrecognized modloader type in this profile — import cancelled.");
        } catch (IOException e) {
            statusLabel.setText("Import failed: " + e.getMessage());
        }
    }
}
