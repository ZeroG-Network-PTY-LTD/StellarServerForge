package com.zerog.stellarserverforge.gui;

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
 * and a plain folder listing (spec §10.3).
 */
public class ModsDialog extends JDialog {

    private final AppContext ctx;
    private final ServerSettings settings;

    private final DefaultListModel<ModEntry> clientModsModel = new DefaultListModel<>();
    private final JList<ModEntry> clientModsList = new JList<>(clientModsModel);
    private final JLabel statusLabel = new JLabel(" ");
    private final JTextArea infoArea = new JTextArea(6, 50);

    public ModsDialog(Frame owner, AppContext ctx, ServerSettings settings) {
        super(owner, "Mods", true);
        this.ctx = ctx;
        this.settings = settings;

        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        add(new JScrollPane(infoArea), BorderLayout.NORTH);

        clientModsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        clientModsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                ModEntry entry = (ModEntry) value;
                return super.getListCellRendererComponent(list, entry.modId() + "  (" + entry.fileName() + ")",
                        index, isSelected, cellHasFocus);
            }
        });
        add(new JScrollPane(clientModsList), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.add(statusLabel, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton scanClientButton = new JButton("Scan for Client-Only Mods");
        JButton moveButton = new JButton("Move Selected to CLIENTMODS");
        JButton mcreatorButton = new JButton("Scan for MCreator Mods");
        JButton listButton = new JButton("List Mods Folder");
        JButton closeButton = new JButton("Close");
        buttons.add(scanClientButton);
        buttons.add(moveButton);
        buttons.add(mcreatorButton);
        buttons.add(listButton);
        buttons.add(closeButton);
        south.add(buttons, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        scanClientButton.addActionListener(e -> scanClientMods());
        moveButton.addActionListener(e -> moveSelected());
        mcreatorButton.addActionListener(e -> scanMcreator());
        listButton.addActionListener(e -> listModsFolder());
        closeButton.addActionListener(e -> dispose());

        setSize(600, 480);
        setLocationRelativeTo(owner);
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
