package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.gui.theme.StellarButton;
import com.zerog.stellarserverforge.gui.theme.StellarLabels;
import com.zerog.stellarserverforge.gui.theme.StellarPanel;
import com.zerog.stellarserverforge.gui.theme.StellarTheme;
import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.model.ServerSettings;
import com.zerog.stellarserverforge.zerogmods.ZeroGModEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/**
 * Browses the externally-maintained catalog of mods the ZeroG Network org has made, and installs
 * the selected one from wherever it's actually hosted — Modrinth (public API) or CurseForge (via
 * ZeroG's own hosted proxy — no API key needed from the user; that's on a stable connection now).
 * The catalog itself is just data the app is pointed at; it never scans GitHub to "discover" mods
 * on its own. A real in-window screen (reached from the dashboard) rather than a modal dialog.
 * <p>
 * A fork of this app that wants to run its own catalog/proxy changes both endpoints from the
 * Settings screen — this screen no longer asks for a personal API key at all.
 */
public class ZeroGModsPanel extends JPanel {

    private final AppContext ctx;
    private final ServerSettings settings;

    private final DefaultListModel<ZeroGModEntry> listModel = new DefaultListModel<>();
    private final JList<ZeroGModEntry> list = new JList<>(listModel);
    private final JTextArea logArea = new JTextArea(8, 60);
    private final JTextField filterField = new JTextField(20);

    private final StellarButton installButton = new StellarButton("Install selected", StellarButton.Variant.PRIMARY);
    private final StellarButton openPageButton = new StellarButton("Open page", StellarButton.Variant.SECONDARY);
    private final StellarButton reloadButton = new StellarButton("Reload catalog", StellarButton.Variant.SECONDARY);
    private final StellarButton openModsFolderButton = new StellarButton("Open mods folder", StellarButton.Variant.SECONDARY);

    private final java.util.List<ZeroGModEntry> masterEntries = new java.util.ArrayList<>();
    private final java.util.Set<ZeroGModEntry> installedThisSession = new java.util.HashSet<>();

    private SwingWorker<java.nio.file.Path, Void> installWorker;

    public ZeroGModsPanel(AppContext ctx, ServerSettings settings, Runnable onBack) {
        this.ctx = ctx;
        this.settings = settings;

        setOpaque(false);
        setLayout(new BorderLayout(0, StellarTheme.SPACE_17));
        setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_22, StellarTheme.SPACE_22,
                StellarTheme.SPACE_22, StellarTheme.SPACE_22));

        JPanel headerRow = new JPanel(new BorderLayout(StellarTheme.SPACE_11, 0));
        headerRow.setOpaque(false);
        StellarButton back = new StellarButton("Back", StellarButton.Variant.GHOST);
        back.addActionListener(e -> onBack.run());
        headerRow.add(back, BorderLayout.WEST);
        headerRow.add(StellarLabels.title("ZeroG Network mods"), BorderLayout.CENTER);
        add(headerRow, BorderLayout.NORTH);

        StellarPanel body = new StellarPanel(new BorderLayout(0, StellarTheme.SPACE_11));
        body.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_17, StellarTheme.SPACE_17,
                StellarTheme.SPACE_17, StellarTheme.SPACE_17));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel kicker = StellarLabels.kicker("Suggested mods");
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(kicker);
        top.add(Box.createVerticalStrut(StellarTheme.SPACE_6));

        JTextArea caption = new JTextArea("Mods the ZeroG Network org has made, installed from wherever they're "
                + "actually hosted — Modrinth needs no key at all, and CurseForge goes through ZeroG's own "
                + "hosted proxy automatically.", 2, 80);
        caption.setEditable(false);
        caption.setFocusable(false);
        caption.setOpaque(false);
        caption.setLineWrap(true);
        caption.setWrapStyleWord(true);
        caption.setFont(StellarTheme.FONT_CAPTION);
        caption.setForeground(StellarTheme.TEXT_SECONDARY);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(caption);
        top.add(Box.createVerticalStrut(StellarTheme.SPACE_11));

        JPanel catalogActionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_8, 0));
        catalogActionsRow.setOpaque(false);
        catalogActionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        catalogActionsRow.add(reloadButton);
        catalogActionsRow.add(openModsFolderButton);
        top.add(catalogActionsRow);
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

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(StellarTheme.SURFACE);
        list.setForeground(StellarTheme.TEXT_PRIMARY);
        list.setCellRenderer(new EntryRenderer());
        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setBorder(BorderFactory.createLineBorder(StellarTheme.NEUTRAL_800));

        JPanel center = new JPanel(new BorderLayout(0, StellarTheme.SPACE_8));
        center.setOpaque(false);
        center.add(listScroll, BorderLayout.CENTER);

        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_8, 0));
        actionsRow.setOpaque(false);
        actionsRow.add(installButton);
        actionsRow.add(openPageButton);
        center.add(actionsRow, BorderLayout.SOUTH);
        body.add(center, BorderLayout.CENTER);

        logArea.setEditable(false);
        logArea.setBackground(StellarTheme.CONSOLE_BG);
        logArea.setForeground(StellarTheme.NEUTRAL_100);
        logArea.setFont(StellarTheme.FONT_MONO);
        logArea.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_11, StellarTheme.SPACE_11,
                StellarTheme.SPACE_11, StellarTheme.SPACE_11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        logScroll.setPreferredSize(new Dimension(100, 120));
        body.add(logScroll, BorderLayout.SOUTH);

        add(body, BorderLayout.CENTER);

        reloadButton.addActionListener(e -> reload());
        installButton.addActionListener(e -> install());
        openPageButton.addActionListener(e -> openPage());
        openModsFolderButton.addActionListener(e -> {
            try {
                java.nio.file.Path modsDir = ctx.serverDir.resolve("mods");
                java.nio.file.Files.createDirectories(modsDir);
                Desktop.getDesktop().open(modsDir.toFile());
            } catch (IOException | UnsupportedOperationException ex) {
                log("Could not open the mods folder: " + ex.getMessage());
            }
        });
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && installButton.isEnabled() && list.getSelectedValue() != null) {
                    install();
                }
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

        if (settings.getModLoader() == ModLoader.VANILLA) {
            log("This server is Vanilla — mods can't be installed until a modloader is set up.");
            installButton.setEnabled(false);
        }

        loadInitial();
    }

    private void log(String message) {
        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void loadInitial() {
        String url = catalogUrl();
        if (!url.isBlank()) {
            reload();
        } else if (ctx.zeroGModCatalogService.hasCachedCopy()) {
            try {
                applyResult(ctx.zeroGModCatalogService.loadCached());
                log("Loaded the last cached copy of the catalog.");
            } catch (IOException e) {
                log("Could not read the cached catalog: " + e.getMessage());
            }
        } else {
            log("No catalog URL configured yet — set one from Settings.");
        }
    }

    /** Falls back to ZeroG Network's own catalog if settings.json predates this field or was
     * cleared — the screen should never need manual setup for the default hosted flow. */
    private String catalogUrl() {
        String url = settings.getZeroGCatalogUrl();
        return (url == null || url.isBlank()) ? ServerSettings.DEFAULT_ZEROG_CATALOG_URL : url;
    }

    private void reload() {
        String url = catalogUrl();
        reloadButton.setEnabled(false);
        log("Fetching catalog from " + url + "...");

        new SwingWorker<com.zerog.stellarserverforge.zerogmods.ZeroGModCatalogService.FetchResult, Void>() {
            @Override
            protected com.zerog.stellarserverforge.zerogmods.ZeroGModCatalogService.FetchResult doInBackground() throws Exception {
                return ctx.zeroGModCatalogService.fetch(url);
            }

            @Override
            protected void done() {
                reloadButton.setEnabled(true);
                try {
                    applyResult(get());
                } catch (Exception ex) {
                    log("Failed to fetch catalog: " + rootMessage(ex));
                    if (ctx.zeroGModCatalogService.hasCachedCopy()) {
                        try {
                            applyResult(ctx.zeroGModCatalogService.loadCached());
                            log("Fell back to the last cached copy.");
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }.execute();
    }

    private void applyResult(com.zerog.stellarserverforge.zerogmods.ZeroGModCatalogService.FetchResult result) {
        masterEntries.clear();
        masterEntries.addAll(result.entries());
        installedThisSession.clear();
        applyFilter();
        log("Loaded " + result.entries().size() + " mod(s) from the catalog.");
        for (String warning : result.skipped()) {
            log("Skipped — " + warning);
        }
    }

    /** Rebuilds the visible list from {@link #masterEntries} using the current filter text, then
     * restores the previous selection if that entry is still visible — a plain {@code
     * DefaultListModel.clear()} would otherwise silently drop it as you type. */
    private void applyFilter() {
        ZeroGModEntry previouslySelected = list.getSelectedValue();
        String query = filterField.getText().trim().toLowerCase();
        listModel.clear();
        for (ZeroGModEntry entry : masterEntries) {
            String name = entry.getName() == null ? "" : entry.getName().toLowerCase();
            String desc = entry.getDescription() == null ? "" : entry.getDescription().toLowerCase();
            if (query.isEmpty() || name.contains(query) || desc.contains(query)) {
                listModel.addElement(entry);
            }
        }
        if (previouslySelected != null) {
            int index = listModel.indexOf(previouslySelected);
            if (index >= 0) {
                list.setSelectedIndex(index);
            }
        }
    }

    private void install() {
        ZeroGModEntry entry = list.getSelectedValue();
        if (entry == null) {
            log("Select a mod first.");
            return;
        }
        installButton.setEnabled(false);
        log("Installing " + entry.getName() + " from " + entry.getSource() + "...");

        installWorker = new SwingWorker<java.nio.file.Path, Void>() {
            @Override
            protected java.nio.file.Path doInBackground() throws Exception {
                McVersion mc = McVersion.parse(settings.getMinecraftVersion());
                ModLoader loader = settings.getModLoader();
                java.nio.file.Path modsDir = ctx.serverDir.resolve("mods");
                return switch (entry.getSource()) {
                    case MODRINTH -> ctx.modrinthInstallService.install(entry, loader, mc, modsDir);
                    case CURSEFORGE -> ctx.curseForgeInstallService.install(entry, loader, mc, modsDir,
                            settings.getCurseForgeApiKey(), settings.getZeroGProxyBaseUrl());
                };
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }
                installButton.setEnabled(true);
                try {
                    java.nio.file.Path installed = get();
                    log("Installed " + installed.getFileName() + " into mods/.");
                    installedThisSession.add(entry);
                    list.repaint();
                } catch (Exception ex) {
                    log("Install failed: " + rootMessage(ex));
                    if (entry.getPageUrl() != null && !entry.getPageUrl().isBlank()) {
                        // CurseForge returns no download URL when the author has disabled
                        // third-party downloads for the file — respecting that choice means the
                        // only legitimate path is the author's own page, so offer to jump straight
                        // there instead of leaving the user to find and click "Open page" themselves.
                        int choice = JOptionPane.showConfirmDialog(ZeroGModsPanel.this,
                                "Could not download \"" + entry.getName() + "\" automatically — its author has "
                                        + "disabled third-party downloads on " + entry.getSource() + ". "
                                        + "Open its page now to download it manually?",
                                "Manual download needed", JOptionPane.YES_NO_OPTION);
                        if (choice == JOptionPane.YES_OPTION) {
                            openPage(entry);
                        } else {
                            log("You can still install it manually — click \"Open page\".");
                        }
                    }
                }
            }
        };
        installWorker.execute();
    }

    /** Called by {@link MainFrame} before this panel instance is discarded (navigating away and
     * back re-creates it) so an in-flight install doesn't keep running against a detached panel
     * and touch UI state nobody will ever see again. */
    public void cancelPendingWork() {
        if (installWorker != null && !installWorker.isDone()) {
            installWorker.cancel(true);
        }
    }

    private void openPage() {
        ZeroGModEntry entry = list.getSelectedValue();
        if (entry == null) {
            log("Select a mod first.");
            return;
        }
        openPage(entry);
    }

    private void openPage(ZeroGModEntry entry) {
        String url = entry.getPageUrl();
        if (url == null || url.isBlank()) {
            log("This entry has no page URL in the catalog.");
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            log("Could not open the page: " + e.getMessage());
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.toString();
    }

    private final class EntryRenderer extends JPanel implements ListCellRenderer<ZeroGModEntry> {
        private final JLabel nameLabel = StellarLabels.body("");
        private final JLabel descLabel = StellarLabels.muted("");
        private final JLabel installedLabel = StellarLabels.muted("");
        private final JLabel sourceLabel = new JLabel();

        EntryRenderer() {
            setLayout(new BorderLayout(StellarTheme.SPACE_8, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            JPanel textCol = new JPanel();
            textCol.setOpaque(false);
            textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
            textCol.add(nameLabel);
            textCol.add(descLabel);
            installedLabel.setForeground(StellarTheme.STATUS_RUNNING);
            textCol.add(installedLabel);
            add(textCol, BorderLayout.CENTER);
            sourceLabel.setFont(StellarTheme.FONT_KICKER);
            add(sourceLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ZeroGModEntry> jList, ZeroGModEntry entry,
                                                        int index, boolean isSelected, boolean cellHasFocus) {
            nameLabel.setText(entry.getName());
            descLabel.setText(entry.getDescription() == null ? "" : entry.getDescription());
            installedLabel.setText(installedThisSession.contains(entry) ? "Installed this session" : "");
            sourceLabel.setText(entry.getSource() == null ? "" : entry.getSource().name());
            Color sourceColor = entry.getSource() == ZeroGModEntry.Source.MODRINTH
                    ? StellarTheme.STATUS_RUNNING : StellarTheme.STELLAR_GOLD;
            sourceLabel.setForeground(sourceColor);
            sourceLabel.setIconTextGap(5);
            if (entry.getSource() == ZeroGModEntry.Source.MODRINTH) {
                sourceLabel.setIcon(new com.zerog.stellarserverforge.gui.theme.StellarSourceIcon(
                        com.zerog.stellarserverforge.gui.theme.StellarSourceIcon.Kind.MODRINTH, 14, sourceColor));
            } else if (entry.getSource() == ZeroGModEntry.Source.CURSEFORGE) {
                sourceLabel.setIcon(new com.zerog.stellarserverforge.gui.theme.StellarSourceIcon(
                        com.zerog.stellarserverforge.gui.theme.StellarSourceIcon.Kind.CURSEFORGE, 14, sourceColor));
            } else {
                sourceLabel.setIcon(null);
            }
            setBackground(isSelected ? StellarTheme.ACCENT_900 : StellarTheme.SURFACE);
            setOpaque(true);
            return this;
        }
    }
}
