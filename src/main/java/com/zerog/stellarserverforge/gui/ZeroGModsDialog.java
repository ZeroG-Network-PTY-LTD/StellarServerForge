package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.gui.theme.StellarButton;
import com.zerog.stellarserverforge.gui.theme.StellarLabels;
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
import java.util.List;

/**
 * Browses the externally-maintained catalog of mods the ZeroG Network org has made, and installs
 * the selected one from wherever it's actually hosted — Modrinth (public API) or CurseForge
 * (requires the user's own API key). The catalog itself is just data the user points the app at;
 * this app never scans GitHub to "discover" mods on its own.
 */
public class ZeroGModsDialog extends JDialog {

    private final AppContext ctx;
    private final ServerSettings settings;

    private final JTextField catalogUrlField = themedField(40);
    private final JTextField apiKeyField = themedField(28);
    private final DefaultListModel<ZeroGModEntry> listModel = new DefaultListModel<>();
    private final JList<ZeroGModEntry> list = new JList<>(listModel);
    private final JTextArea logArea = new JTextArea(8, 60);

    private final StellarButton reloadButton = new StellarButton("Reload catalog", StellarButton.Variant.SECONDARY);
    private final StellarButton installButton = new StellarButton("Install selected", StellarButton.Variant.PRIMARY);
    private final StellarButton openPageButton = new StellarButton("Open page", StellarButton.Variant.SECONDARY);

    public ZeroGModsDialog(Frame owner, AppContext ctx, ServerSettings settings) {
        super(owner, "ZeroG Network mods", true);
        this.ctx = ctx;
        this.settings = settings;

        getContentPane().setBackground(StellarTheme.BG);
        setLayout(new BorderLayout(StellarTheme.SPACE_11, StellarTheme.SPACE_11));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(
                StellarTheme.SPACE_17, StellarTheme.SPACE_17, StellarTheme.SPACE_17, StellarTheme.SPACE_17));

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(StellarLabels.heading("ZeroG Network mods"));
        north.add(Box.createVerticalStrut(StellarTheme.SPACE_6));
        north.add(StellarLabels.muted("Suggests mods the ZeroG Network org has made, installed from wherever they're actually hosted."));
        north.add(Box.createVerticalStrut(StellarTheme.SPACE_11));

        catalogUrlField.setText(settings.getZeroGCatalogUrl());
        JPanel catalogRow = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_8, 0));
        catalogRow.setOpaque(false);
        catalogRow.add(StellarLabels.body("Catalog URL:"));
        catalogRow.add(catalogUrlField);
        catalogRow.add(reloadButton);
        north.add(catalogRow);

        apiKeyField.setText(settings.getCurseForgeApiKey());
        JPanel keyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_8, 0));
        keyRow.setOpaque(false);
        keyRow.add(StellarLabels.body("CurseForge API key (optional):"));
        keyRow.add(apiKeyField);
        north.add(keyRow);
        north.add(StellarLabels.muted("CurseForge-sourced mods install automatically via ZeroG's own proxy — "
                + "no key needed. Only add your own (from console.curseforge.com) to bypass the proxy."));

        add(north, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(StellarTheme.SURFACE);
        list.setForeground(StellarTheme.TEXT_PRIMARY);
        list.setCellRenderer(new EntryRenderer());
        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, StellarTheme.SPACE_8));
        south.setOpaque(false);

        logArea.setEditable(false);
        logArea.setBackground(StellarTheme.CONSOLE_BG);
        logArea.setForeground(StellarTheme.NEUTRAL_100);
        logArea.setFont(StellarTheme.FONT_MONO);
        south.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_8, 0));
        buttons.setOpaque(false);
        buttons.add(installButton);
        buttons.add(openPageButton);
        StellarButton closeButton = new StellarButton("Close", StellarButton.Variant.GHOST);
        closeButton.addActionListener(e -> dispose());
        buttons.add(closeButton);
        south.add(buttons, BorderLayout.SOUTH);

        add(south, BorderLayout.SOUTH);

        reloadButton.addActionListener(e -> reload(true));
        installButton.addActionListener(e -> install());
        openPageButton.addActionListener(e -> openPage());

        setSize(720, 560);
        setMinimumSize(new Dimension(560, 420));
        setLocationRelativeTo(owner);

        if (settings.getModLoader() == ModLoader.VANILLA) {
            log("This server is Vanilla — mods can't be installed until a modloader is set up.");
            installButton.setEnabled(false);
        }

        loadInitial();
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

    private void log(String message) {
        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void loadInitial() {
        if (!catalogUrlField.getText().isBlank()) {
            reload(false);
        } else if (ctx.zeroGModCatalogService.hasCachedCopy()) {
            try {
                applyResult(ctx.zeroGModCatalogService.loadCached());
                log("Loaded the last cached copy of the catalog (no URL set yet).");
            } catch (IOException e) {
                log("Could not read the cached catalog: " + e.getMessage());
            }
        } else {
            log("Enter the raw GitHub URL to your mods catalog JSON and click Reload catalog.");
        }
    }

    private void reload(boolean persist) {
        String url = catalogUrlField.getText().trim();
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
                    if (persist) {
                        persistFields();
                    }
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
        listModel.clear();
        result.entries().forEach(listModel::addElement);
        log("Loaded " + result.entries().size() + " mod(s) from the catalog.");
        for (String warning : result.skipped()) {
            log("Skipped — " + warning);
        }
    }

    private void persistFields() {
        settings.setZeroGCatalogUrl(catalogUrlField.getText().trim());
        settings.setCurseForgeApiKey(apiKeyField.getText().trim());
        try {
            ctx.settingsService.save(settings);
        } catch (IOException e) {
            log("Could not save settings.json: " + e.getMessage());
        } catch (RuntimeException e) {
            // Covers SecretStore.encrypt() failing (e.g. a corrupted local encryption key file) —
            // that's a real problem worth surfacing, but shouldn't crash uncaught on the EDT.
            log("Could not save settings.json (encryption failed): " + e.getMessage());
        }
    }

    private void install() {
        ZeroGModEntry entry = list.getSelectedValue();
        if (entry == null) {
            log("Select a mod first.");
            return;
        }
        persistFields();
        installButton.setEnabled(false);
        log("Installing " + entry.getName() + " from " + entry.getSource() + "...");

        new SwingWorker<java.nio.file.Path, Void>() {
            @Override
            protected java.nio.file.Path doInBackground() throws Exception {
                McVersion mc = McVersion.parse(settings.getMinecraftVersion());
                ModLoader loader = settings.getModLoader();
                java.nio.file.Path modsDir = ctx.serverDir.resolve("mods");
                return switch (entry.getSource()) {
                    case MODRINTH -> ctx.modrinthInstallService.install(entry, loader, mc, modsDir);
                    case CURSEFORGE -> ctx.curseForgeInstallService.install(entry, loader, mc, modsDir,
                            settings.getCurseForgeApiKey());
                };
            }

            @Override
            protected void done() {
                installButton.setEnabled(true);
                try {
                    java.nio.file.Path installed = get();
                    log("Installed " + installed.getFileName() + " into mods/.");
                } catch (Exception ex) {
                    log("Install failed: " + rootMessage(ex));
                    if (entry.getPageUrl() != null && !entry.getPageUrl().isBlank()) {
                        log("You can still install it manually — click \"Open page\".");
                    }
                }
            }
        }.execute();
    }

    private void openPage() {
        ZeroGModEntry entry = list.getSelectedValue();
        if (entry == null) {
            log("Select a mod first.");
            return;
        }
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

    private static final class EntryRenderer extends JPanel implements ListCellRenderer<ZeroGModEntry> {
        private final JLabel nameLabel = StellarLabels.body("");
        private final JLabel descLabel = StellarLabels.muted("");
        private final JLabel sourceLabel = new JLabel();

        EntryRenderer() {
            setLayout(new BorderLayout(StellarTheme.SPACE_8, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            JPanel textCol = new JPanel();
            textCol.setOpaque(false);
            textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
            textCol.add(nameLabel);
            textCol.add(descLabel);
            add(textCol, BorderLayout.CENTER);
            sourceLabel.setFont(StellarTheme.FONT_KICKER);
            add(sourceLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ZeroGModEntry> jList, ZeroGModEntry entry,
                                                        int index, boolean isSelected, boolean cellHasFocus) {
            nameLabel.setText(entry.getName());
            descLabel.setText(entry.getDescription() == null ? "" : entry.getDescription());
            sourceLabel.setText(entry.getSource() == null ? "" : entry.getSource().name());
            sourceLabel.setForeground(entry.getSource() == ZeroGModEntry.Source.MODRINTH
                    ? StellarTheme.STATUS_RUNNING : StellarTheme.STELLAR_GOLD);
            setBackground(isSelected ? StellarTheme.ACCENT_900 : StellarTheme.SURFACE);
            setOpaque(true);
            return this;
        }
    }
}
