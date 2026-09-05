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

    /** A profile paired with its eagerly-parsed metadata, when parsing succeeds — {@code parsed}
     * is null for a profile whose {@code minecraftinstance.json} couldn't be read/validated, so
     * the list can still show it (with a warning) rather than silently omitting it. */
    private record DisplayEntry(CurseForgeImportService.ProfileInfo info, CurseForgeImportService.ParsedProfile parsed) {
    }

    private final AppContext ctx;
    private final Consumer<ServerSettings> onImported;

    private final DefaultListModel<DisplayEntry> model = new DefaultListModel<>();
    private final JList<DisplayEntry> list = new JList<>(model);
    private final JLabel statusLabel = StellarLabels.muted(" ");
    private final StellarButton browseButton = new StellarButton("Browse for instances folder", StellarButton.Variant.SECONDARY);
    private final StellarButton refreshButton = new StellarButton("Refresh", StellarButton.Variant.SECONDARY);
    private final StellarButton importButton = new StellarButton("Import selected", StellarButton.Variant.PRIMARY);
    private Path lastRoot;

    public CurseForgeImportDialog(Frame owner, AppContext ctx, Consumer<ServerSettings> onImported) {
        super(owner, "Import CurseForge profile", true);
        this.ctx = ctx;
        this.onImported = onImported;

        getContentPane().setBackground(StellarTheme.BG);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        list.setBackground(StellarTheme.SURFACE);
        list.setForeground(StellarTheme.TEXT_PRIMARY);
        list.setCellRenderer(new ProfileRenderer());
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedValue() != null) {
                    importSelected();
                }
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
        StellarButton cancelButton = new StellarButton("Cancel", StellarButton.Variant.GHOST);
        buttons.add(browseButton);
        buttons.add(refreshButton);
        buttons.add(importButton);
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);

        browseButton.addActionListener(e -> browseManually());
        refreshButton.addActionListener(e -> {
            if (lastRoot != null) {
                loadProfiles(lastRoot);
            } else {
                statusLabel.setText("Nothing to refresh yet — browse for an instances folder first.");
            }
        });
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
        lastRoot = root;
        try {
            List<CurseForgeImportService.ProfileInfo> profiles = ctx.curseForgeImportService.listProfiles(root);
            model.clear();
            int unparseable = 0;
            for (CurseForgeImportService.ProfileInfo info : profiles) {
                CurseForgeImportService.ParsedProfile parsed;
                try {
                    parsed = ctx.curseForgeImportService.parseProfile(info.path());
                } catch (IOException e) {
                    // Shown in the list as "details unavailable" instead of failing the whole
                    // listing — importSelected() re-parses (and surfaces the real error) anyway.
                    parsed = null;
                    unparseable++;
                }
                model.addElement(new DisplayEntry(info, parsed));
            }
            if (profiles.isEmpty()) {
                statusLabel.setText("No profiles with a minecraftinstance.json were found under " + root);
            } else {
                statusLabel.setText("Found " + profiles.size() + " profile(s) under " + root
                        + (unparseable > 0 ? " (" + unparseable + " with unreadable details)" : ""));
            }
        } catch (IOException e) {
            statusLabel.setText("Could not read that folder: " + e.getMessage());
        }
    }

    private void importSelected() {
        DisplayEntry selected = list.getSelectedValue();
        if (selected == null) {
            statusLabel.setText("Select a profile first.");
            return;
        }

        try {
            CurseForgeImportService.ParsedProfile parsed = selected.parsed() != null
                    ? selected.parsed() : ctx.curseForgeImportService.parseProfile(selected.info().path());

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

            setControlsEnabled(false);
            statusLabel.setText("Importing \"" + parsed.displayName() + "\"...");

            ModLoader finalLoader = loader;
            McVersion finalMc = mc;
            new SwingWorker<Void, Void>() {
                IOException failure;

                @Override
                protected Void doInBackground() {
                    try {
                        ctx.curseForgeImportService.importInto(selected.info().path(), ctx.serverDir);
                    } catch (IOException e) {
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
                    settings.setModLoader(finalLoader);
                    settings.setModLoaderVersion(parsed.modLoaderVersion());
                    settings.setJavaVersion(JavaVersionRules.resolve(finalMc).defaultVersion());
                    settings.setJavaOverrideMode(JavaOverrideMode.AUTOMATIC);

                    onImported.accept(settings);
                    statusLabel.setText("Imported successfully.");
                    dispose();
                }
            }.execute();
        } catch (IOException e) {
            statusLabel.setText("Import failed: " + e.getMessage());
        }
    }

    private void setControlsEnabled(boolean enabled) {
        browseButton.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        importButton.setEnabled(enabled);
        list.setEnabled(enabled);
    }

    private final class ProfileRenderer extends JPanel implements ListCellRenderer<DisplayEntry> {
        private final JLabel nameLabel = StellarLabels.body("");
        private final JLabel detailLabel = StellarLabels.muted("");
        private final JLabel folderLabel = StellarLabels.muted("");

        ProfileRenderer() {
            setLayout(new BorderLayout(StellarTheme.SPACE_8, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            JPanel textCol = new JPanel();
            textCol.setOpaque(false);
            textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
            textCol.add(nameLabel);
            textCol.add(detailLabel);
            add(textCol, BorderLayout.CENTER);
            folderLabel.setFont(StellarTheme.FONT_CAPTION);
            add(folderLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends DisplayEntry> jList, DisplayEntry entry,
                                                        int index, boolean isSelected, boolean cellHasFocus) {
            CurseForgeImportService.ParsedProfile parsed = entry.parsed();
            if (parsed != null) {
                nameLabel.setText(parsed.displayName());
                detailLabel.setText(parsed.minecraftVersion() + " · " + parsed.modLoaderName() + " "
                        + parsed.modLoaderVersion());
            } else {
                nameLabel.setText(entry.info().folderName());
                detailLabel.setText("Details unavailable — profile may be corrupted or unsupported.");
            }
            folderLabel.setText(entry.info().folderName());
            setBackground(isSelected ? StellarTheme.ACCENT_900 : StellarTheme.SURFACE);
            setOpaque(true);
            return this;
        }
    }
}
