package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.gui.theme.StellarButton;
import com.zerog.stellarserverforge.gui.theme.StellarLabels;
import com.zerog.stellarserverforge.gui.theme.StellarPanel;
import com.zerog.stellarserverforge.gui.theme.StellarTheme;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.model.ServerSettings;
import com.zerog.stellarserverforge.mods.FabricQuiltModScanner;
import com.zerog.stellarserverforge.mods.ForgeNeoForgeModScanner;
import com.zerog.stellarserverforge.mods.McreatorScanner;
import com.zerog.stellarserverforge.mods.ModEntry;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Mods folder tools: client-only mod scanning (spec §10.1), MCreator mod detection (spec §10.2),
 * and a plain folder listing (spec §10.3). A real in-window screen (reached from the dashboard's
 * "Mods" nav link/toolbar button) rather than a separate modal dialog.
 */
public class ModsPanel extends JPanel {

    private final AppContext ctx;
    private final ServerSettings settings;

    private final DefaultListModel<ModEntry> clientModsModel = new DefaultListModel<>();
    private final JList<ModEntry> clientModsList = new JList<>(clientModsModel);
    private final JLabel statusLabel = StellarLabels.muted(" ");
    private final JTextArea infoArea = new JTextArea(6, 50);
    private final StellarButton moveButton = new StellarButton("Move selected to CLIENTMODS", StellarButton.Variant.SECONDARY);

    public ModsPanel(AppContext ctx, ServerSettings settings, Runnable onBack) {
        this.ctx = ctx;
        this.settings = settings;

        setOpaque(false);
        setLayout(new BorderLayout(0, StellarTheme.SPACE_17));
        setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_22, StellarTheme.SPACE_22,
                StellarTheme.SPACE_22, StellarTheme.SPACE_22));

        JPanel header = new JPanel(new BorderLayout(StellarTheme.SPACE_11, 0));
        header.setOpaque(false);
        StellarButton back = new StellarButton("Back", StellarButton.Variant.GHOST);
        back.addActionListener(e -> onBack.run());
        header.add(back, BorderLayout.WEST);
        header.add(StellarLabels.title("Mods"), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        StellarPanel body = new StellarPanel(new BorderLayout(0, StellarTheme.SPACE_11));
        body.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_17, StellarTheme.SPACE_17,
                StellarTheme.SPACE_17, StellarTheme.SPACE_17));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel scannerKicker = StellarLabels.kicker("Client-only mod scanner");
        scannerKicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(scannerKicker);
        top.add(Box.createVerticalStrut(StellarTheme.SPACE_6));
        JTextArea caption = new JTextArea("Flags mods that only belong on the client, so they can be moved out "
                + "of a dedicated server's mods folder without breaking anything else that depends on them.",
                2, 80);
        caption.setEditable(false);
        caption.setFocusable(false);
        caption.setOpaque(false);
        caption.setLineWrap(true);
        caption.setWrapStyleWord(true);
        caption.setFont(StellarTheme.FONT_CAPTION);
        caption.setForeground(StellarTheme.TEXT_SECONDARY);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        caption.setBorder(BorderFactory.createEmptyBorder());
        top.add(caption);
        top.add(Box.createVerticalStrut(StellarTheme.SPACE_11));

        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_8, 0));
        actionsRow.setOpaque(false);
        actionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        StellarButton scanClientButton = new StellarButton("Scan for client-only mods", StellarButton.Variant.PRIMARY);
        StellarButton mcreatorButton = new StellarButton("Scan for MCreator mods", StellarButton.Variant.SECONDARY);
        StellarButton listButton = new StellarButton("List mods folder", StellarButton.Variant.SECONDARY);
        actionsRow.add(scanClientButton);
        actionsRow.add(mcreatorButton);
        actionsRow.add(listButton);
        top.add(actionsRow);
        body.add(top, BorderLayout.NORTH);

        clientModsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        clientModsList.setBackground(StellarTheme.SURFACE);
        clientModsList.setForeground(StellarTheme.TEXT_PRIMARY);
        clientModsList.setFont(StellarTheme.FONT_MONO);
        clientModsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                ModEntry entry = (ModEntry) value;
                Component c = super.getListCellRendererComponent(list, entry.modId() + "  (" + entry.fileName() + ")",
                        index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return c;
            }
        });

        JPanel center = new JPanel(new BorderLayout(0, StellarTheme.SPACE_8));
        center.setOpaque(false);
        JScrollPane listScroll = new JScrollPane(clientModsList);
        listScroll.setBorder(BorderFactory.createLineBorder(StellarTheme.NEUTRAL_800));
        listScroll.setPreferredSize(new Dimension(100, 140));
        center.add(listScroll, BorderLayout.CENTER);

        JPanel moveRow = new JPanel(new BorderLayout());
        moveRow.setOpaque(false);
        moveRow.add(moveButton, BorderLayout.WEST);
        moveRow.add(statusLabel, BorderLayout.EAST);
        center.add(moveRow, BorderLayout.SOUTH);
        body.add(center, BorderLayout.CENTER);

        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setBackground(StellarTheme.CONSOLE_BG);
        infoArea.setForeground(StellarTheme.NEUTRAL_100);
        infoArea.setFont(StellarTheme.FONT_MONO);
        infoArea.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_11, StellarTheme.SPACE_11,
                StellarTheme.SPACE_11, StellarTheme.SPACE_11));
        JScrollPane infoScroll = new JScrollPane(infoArea);
        infoScroll.setBorder(BorderFactory.createEmptyBorder());
        infoScroll.setPreferredSize(new Dimension(100, 140));
        body.add(infoScroll, BorderLayout.SOUTH);

        add(body, BorderLayout.CENTER);

        scanClientButton.addActionListener(e -> scanClientMods());
        moveButton.addActionListener(e -> moveSelected());
        mcreatorButton.addActionListener(e -> scanMcreator());
        listButton.addActionListener(e -> listModsFolder());
    }

    private Path modsDir() {
        return ctx.serverDir.resolve("mods");
    }

    private void scanClientMods() {
        clientModsModel.clear();
        infoArea.setText("Scanning...");
        statusLabel.setText(" ");

        new SwingWorker<Void, Void>() {
            List<ModEntry> flagged = List.of();
            List<ModEntry> keptAsDependency = List.of();
            String error;

            @Override
            protected Void doInBackground() {
                try {
                    var clientOnlyIds = new com.zerog.stellarserverforge.mods.ClientOnlyModListService(ctx.cacheDir).fetch();
                    ModLoader loader = settings.getModLoader();
                    if (loader == ModLoader.FABRIC || loader == ModLoader.QUILT) {
                        FabricQuiltModScanner.ScanResult result = new FabricQuiltModScanner().scan(modsDir(), loader);
                        flagged = result.removable();
                        keptAsDependency = result.keptAsDependency();
                    } else {
                        int mcMajor = com.zerog.stellarserverforge.model.McVersion.parse(settings.getMinecraftVersion()).major();
                        ForgeNeoForgeModScanner.ScanResult result = new ForgeNeoForgeModScanner().scan(modsDir(), mcMajor, clientOnlyIds);
                        flagged = result.flaggedClientMods();
                        if (!result.essentialMods().isEmpty()) {
                            moveFilesToClientMods(result.essentialMods());
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    error = e.getMessage();
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    infoArea.setText("Scan failed: " + error);
                    return;
                }
                flagged.forEach(clientModsModel::addElement);
                StringBuilder sb = new StringBuilder();
                sb.append("Found ").append(flagged.size()).append(" client-only mod(s) safe to move.");
                if (!keptAsDependency.isEmpty()) {
                    sb.append("\nKept because required as a dependency by another mod: ");
                    keptAsDependency.forEach(m -> sb.append(m.modId()).append(", "));
                }
                infoArea.setText(sb.toString());
            }
        }.execute();
    }

    private void moveSelected() {
        List<ModEntry> selected = clientModsList.getSelectedValuesList();
        if (selected.isEmpty()) {
            statusLabel.setText("Select one or more mods first.");
            return;
        }
        try {
            moveFilesToClientMods(selected.stream().map(ModEntry::fileName).toList());
            selected.forEach(clientModsModel::removeElement);
            statusLabel.setText("Moved " + selected.size() + " mod(s) to CLIENTMODS.");
        } catch (IOException e) {
            statusLabel.setText("Move failed: " + e.getMessage());
        }
    }

    private void moveFilesToClientMods(List<String> fileNames) throws IOException {
        Path target = ctx.serverDir.resolve("CLIENTMODS");
        Files.createDirectories(target);
        for (String fileName : fileNames) {
            Path source = modsDir().resolve(fileName);
            if (Files.exists(source)) {
                Files.move(source, target.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void scanMcreator() {
        infoArea.setText("Scanning for MCreator mods...");
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return McreatorScanner.scan(modsDir());
            }

            @Override
            protected void done() {
                try {
                    List<String> found = get();
                    if (found.isEmpty()) {
                        infoArea.setText("No MCreator-made mods detected.");
                    } else {
                        infoArea.setText("MCreator mods detected (these are commonly poorly optimized and a "
                                + "frequent cause of server issues):\n" + String.join("\n", found));
                    }
                } catch (Exception e) {
                    infoArea.setText("Scan failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void listModsFolder() {
        clientModsModel.clear();
        Path dir = modsDir();
        if (!Files.isDirectory(dir)) {
            infoArea.setText("No mods folder found.");
            return;
        }
        StringBuilder sb = new StringBuilder("Contents of mods/:\n");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                sb.append(p.getFileName()).append('\n');
            }
        } catch (IOException e) {
            sb.append("(error listing folder: ").append(e.getMessage()).append(")");
        }
        infoArea.setText(sb.toString());
    }
}
