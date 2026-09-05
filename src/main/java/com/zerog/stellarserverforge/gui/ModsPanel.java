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
import java.util.ArrayList;
import java.util.Comparator;
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
    private final StellarButton viewToggleButton = new StellarButton("View CLIENTMODS", StellarButton.Variant.SECONDARY);
    private final StellarButton openFolderButton = new StellarButton("Open folder", StellarButton.Variant.SECONDARY);
    private final JTextField filterField = new JTextField(20);
    private final List<ModEntry> masterEntries = new ArrayList<>();
    private final java.util.Set<String> selectedFileNames = new java.util.LinkedHashSet<>();
    private boolean viewingClientMods;
    private boolean restoringSelection;

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
        actionsRow.add(viewToggleButton);
        actionsRow.add(openFolderButton);
        top.add(actionsRow);
        top.add(Box.createVerticalStrut(StellarTheme.SPACE_8));

        JPanel filterRow = new JPanel(new BorderLayout(StellarTheme.SPACE_8, 0));
        filterRow.setOpaque(false);
        filterRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterRow.add(StellarLabels.body("Filter:"), BorderLayout.WEST);
        filterField.setBackground(StellarTheme.FIELD_BG);
        filterField.setForeground(StellarTheme.TEXT_PRIMARY);
        filterField.setCaretColor(StellarTheme.ACCENT);
        filterField.setFont(StellarTheme.FONT_BODY);
        filterField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(StellarTheme.NEUTRAL_800),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        filterRow.add(filterField, BorderLayout.CENTER);
        top.add(filterRow);
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
        // Tracks selection by file name (not list index) so it survives the list being rebuilt —
        // by filtering, or by a fresh scan/listing — instead of silently dropping selected mods.
        clientModsList.addListSelectionListener(e -> {
            if (restoringSelection || e.getValueIsAdjusting()) {
                return;
            }
            selectedFileNames.clear();
            for (ModEntry entry : clientModsList.getSelectedValuesList()) {
                selectedFileNames.add(entry.fileName());
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
        listButton.addActionListener(e -> loadFolder(modsDir(), false));
        viewToggleButton.addActionListener(e -> {
            boolean goingToClientMods = !viewingClientMods;
            loadFolder(goingToClientMods ? clientModsDir() : modsDir(), goingToClientMods);
        });
        openFolderButton.addActionListener(e -> {
            try {
                Path dir = viewingClientMods ? clientModsDir() : modsDir();
                Files.createDirectories(dir);
                Desktop.getDesktop().open(dir.toFile());
            } catch (IOException | UnsupportedOperationException ex) {
                statusLabel.setText("Could not open the folder: " + ex.getMessage());
            }
        });
        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }
        });

        loadFolder(modsDir(), false);
    }

    private Path modsDir() {
        return ctx.serverDir.resolve("mods");
    }

    private Path clientModsDir() {
        return ctx.serverDir.resolve("CLIENTMODS");
    }

    /** Replaces the full (unfiltered) set of entries currently backing the list — called after a
     * fresh scan or folder listing, so any selection from a previous listing is discarded — then
     * re-applies whatever filter text is already in the box. */
    private void setEntries(List<ModEntry> entries) {
        masterEntries.clear();
        masterEntries.addAll(entries);
        selectedFileNames.clear();
        applyFilter();
    }

    /** Rebuilds the visible list from {@link #masterEntries} using the current filter text, then
     * restores selection (by file name, from {@link #selectedFileNames}) for whichever previously
     * selected mods are still visible — narrowing or widening the filter would otherwise silently
     * clear the selection, since {@code DefaultListModel.clear()} resets it. */
    private void applyFilter() {
        String query = filterField.getText().trim().toLowerCase();
        clientModsModel.clear();
        for (ModEntry entry : masterEntries) {
            if (query.isEmpty() || entry.modId().toLowerCase().contains(query)
                    || entry.fileName().toLowerCase().contains(query)) {
                clientModsModel.addElement(entry);
            }
        }
        restoringSelection = true;
        try {
            for (int i = 0; i < clientModsModel.size(); i++) {
                if (selectedFileNames.contains(clientModsModel.get(i).fileName())) {
                    clientModsList.addSelectionInterval(i, i);
                }
            }
        } finally {
            restoringSelection = false;
        }
    }

    /** Lists the plain contents of a mods-style folder (mods/ or CLIENTMODS/) into the same list
     * used for scan results, so filtering, sorting, and the move button work identically regardless
     * of whether the list was populated by a scan or a plain listing. */
    private void loadFolder(Path dir, boolean clientModsMode) {
        viewingClientMods = clientModsMode;
        moveButton.setText(clientModsMode ? "Move selected back to mods" : "Move selected to CLIENTMODS");
        viewToggleButton.setText(clientModsMode ? "View mods folder" : "View CLIENTMODS");
        filterField.setText("");

        if (!Files.isDirectory(dir)) {
            setEntries(List.of());
            infoArea.setText("No " + dir.getFileName() + " folder found yet.");
            return;
        }

        List<ModEntry> entries = new ArrayList<>();
        long totalBytes = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    continue;
                }
                String name = p.getFileName().toString();
                int dot = name.lastIndexOf('.');
                String id = dot > 0 ? name.substring(0, dot) : name;
                entries.add(new ModEntry(id, name));
                totalBytes += Files.size(p);
            }
        } catch (IOException e) {
            infoArea.setText("Error listing " + dir.getFileName() + "/: " + e.getMessage());
            return;
        }
        entries.sort(Comparator.comparing(ModEntry::fileName, String.CASE_INSENSITIVE_ORDER));
        setEntries(entries);
        infoArea.setText(entries.size() + " file(s) in " + dir.getFileName() + "/, " + formatSize(totalBytes) + " total.");
    }

    private static String formatSize(long bytes) {
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private void scanClientMods() {
        viewingClientMods = false;
        moveButton.setText("Move selected to CLIENTMODS");
        viewToggleButton.setText("View CLIENTMODS");
        setEntries(List.of());
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
                            moveFiles(modsDir(), clientModsDir(), result.essentialMods());
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
                setEntries(flagged);
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
        List<String> fileNames = selected.stream().map(ModEntry::fileName).toList();
        try {
            if (viewingClientMods) {
                moveFiles(clientModsDir(), modsDir(), fileNames);
                statusLabel.setText("Moved " + selected.size() + " mod(s) back to mods/.");
                loadFolder(clientModsDir(), true);
            } else {
                moveFiles(modsDir(), clientModsDir(), fileNames);
                statusLabel.setText("Moved " + selected.size() + " mod(s) to CLIENTMODS.");
                selected.forEach(clientModsModel::removeElement);
                masterEntries.removeAll(selected);
                fileNames.forEach(selectedFileNames::remove);
            }
        } catch (IOException e) {
            statusLabel.setText("Move failed: " + e.getMessage());
        }
    }

    private void moveFiles(Path from, Path to, List<String> fileNames) throws IOException {
        Files.createDirectories(to);
        for (String fileName : fileNames) {
            Path source = from.resolve(fileName);
            if (Files.exists(source)) {
                Files.move(source, to.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
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

}
