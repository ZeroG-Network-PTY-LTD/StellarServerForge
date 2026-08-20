package com.zerog.network.stellarforge.gui;

import com.zerog.network.stellarforge.api.CurseForgeClient;
import com.zerog.network.stellarforge.api.ModrinthClient;
import com.zerog.network.stellarforge.config.SecureConfig;
import com.zerog.network.stellarforge.gui.components.ToastNotification;
import com.zerog.network.stellarforge.model.ModInfo;
import com.zerog.network.stellarforge.model.ServerConfig;
import com.zerog.network.stellarforge.utils.ConflictDetector;
import com.zerog.network.stellarforge.utils.DependencyResolver;
import com.zerog.network.stellarforge.utils.ModUpdateChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Desktop;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.List;

/**
 * Enhanced 3-panel Mod Installer Dialog.
 *
 * Layout:
 *   LEFT   – category & source filters + sort + install queue
 *   CENTER – results table (Name / Version / Source / Downloads / MC Ver)
 *   RIGHT  – detail pane (description, categories, author, deps, actions)
 *
 * Features:
 *   - Category checkboxes  (Technology, Magic, Adventure, Utility, Performance)
 *   - Source selector      (All / CurseForge / Modrinth)
 *   - Sort selector        (Popular / Most Downloads / Name)
 *   - Download count column
 *   - Dependency resolution before batch install
 *   - Conflict detection before batch install
 *   - Real downloads (no Thread.sleep placeholder)
 *   - Toast notifications
 */
public class ModInstallerDialog extends JDialog {

    private static final Logger logger = LoggerFactory.getLogger(ModInstallerDialog.class);

    // ── State ─────────────────────────────────────────────────────────────────
    private final ServerConfig serverConfig;
    private final CurseForgeClient curseForgeClient;
    private final ModrinthClient modrinthClient;
    private final File modsDirectory;
    private final DependencyResolver depResolver;
    private final ConflictDetector conflictDetector;

    private List<ModInfo> currentMods = new ArrayList<>();
    private final List<ModInfo> installQueue = new ArrayList<>();

    // ── Left panel controls ───────────────────────────────────────────────────
    private JComboBox<String> sourceCombo;
    private JComboBox<String> sortCombo;
    private final Map<String, JCheckBox> categoryBoxes = new LinkedHashMap<>();
    private DefaultListModel<ModInfo> queueListModel;
    private JList<ModInfo> queueList;

    // ── Center panel controls ─────────────────────────────────────────────────
    private JTextField searchField;
    private JTable modsTable;
    private DefaultTableModel tableModel;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    // ── Right panel controls ──────────────────────────────────────────────────
    private JLabel detailNameLabel;
    private JLabel detailSourceLabel;
    private JLabel detailDownloadsLabel;
    private JTextArea detailDescArea;
    private JLabel detailCategoriesLabel;
    private JLabel detailAuthorLabel;
    private JLabel detailDepsLabel;
    private JButton installSelectedBtn;
    private JButton addToQueueBtn;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ModInstallerDialog(Frame parent, ServerConfig serverConfig) {
        super(parent, "Mod Installer — Stellar Server Forge", true);
        this.serverConfig = serverConfig;
        this.curseForgeClient = new CurseForgeClient();
        this.modrinthClient   = new ModrinthClient();

        this.modsDirectory = (serverConfig.getServerPath() != null && !serverConfig.getServerPath().isEmpty())
                ? new File(serverConfig.getServerPath(), "mods")
                : new File("mods");

        this.depResolver      = new DependencyResolver(modsDirectory);
        this.conflictDetector = new ConflictDetector(modsDirectory);

        initUI();
        setSize(1150, 760);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(parent);
        logger.info("ModInstallerDialog (enhanced) initialized");
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void initUI() {
        setLayout(new BorderLayout(6, 6));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(createTopBar(),    BorderLayout.NORTH);
        add(createMainSplit(), BorderLayout.CENTER);
        add(createBottomBar(), BorderLayout.SOUTH);
    }

    // ── Top search bar ────────────────────────────────────────────────────────

    private JPanel createTopBar() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(new Color(30, 30, 38));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel title = new JLabel("🧩 Mod Installer");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        title.setForeground(Color.WHITE);

        searchField = new JTextField();
        searchField.setBackground(new Color(42, 42, 52));
        searchField.setForeground(new Color(220, 220, 230));
        searchField.setCaretColor(Color.WHITE);
        searchField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        searchField.putClientProperty("JTextField.placeholderText", "Search mods…");
        searchField.addActionListener(e -> performSearch());

        JButton searchBtn  = actionButton("🔍 Search",         new Color(0, 100, 170),  e -> performSearch());
        JButton suggestBtn = actionButton("⭐ Suggested",       new Color(60, 70, 90),    e -> loadSuggestedMods());
        JButton updBtn     = actionButton("🔄 Check Updates",   new Color(60, 70, 90),    e -> checkInstalledUpdates());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnRow.setOpaque(false);
        btnRow.add(suggestBtn); btnRow.add(updBtn);

        JPanel searchRow = new JPanel(new BorderLayout(6, 0));
        searchRow.setOpaque(false);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(searchBtn,   BorderLayout.EAST);

        panel.add(title,      BorderLayout.WEST);
        panel.add(searchRow,  BorderLayout.CENTER);
        panel.add(btnRow,     BorderLayout.EAST);
        return panel;
    }

    // ── Main 3-column split ───────────────────────────────────────────────────

    private JSplitPane createMainSplit() {
        JSplitPane leftCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createFilterPanel(), createCenterPanel());
        leftCenter.setDividerLocation(190);
        leftCenter.setDividerSize(4);
        leftCenter.setBorder(null);

        JSplitPane full = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftCenter, createRightPanel());
        full.setDividerLocation(680);
        full.setDividerSize(4);
        full.setResizeWeight(0.7);
        full.setBorder(null);
        return full;
    }

    // ── LEFT: filter + queue panel ────────────────────────────────────────────

    private JScrollPane createFilterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(23, 23, 30));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 8));

        panel.add(sectionLabel("SOURCE"));
        panel.add(Box.createVerticalStrut(4));
        sourceCombo = new JComboBox<>(new String[]{"All Sources", "CurseForge", "Modrinth"});
        styleCombo(sourceCombo);
        sourceCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 27));
        sourceCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sourceCombo);
        panel.add(Box.createVerticalStrut(12));

        panel.add(sectionLabel("SORT BY"));
        panel.add(Box.createVerticalStrut(4));
        sortCombo = new JComboBox<>(new String[]{"Most Downloads", "Popular", "Name (A-Z)"});
        styleCombo(sortCombo);
        sortCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 27));
        sortCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sortCombo);
        panel.add(Box.createVerticalStrut(12));

        panel.add(sectionLabel("CATEGORIES"));
        panel.add(Box.createVerticalStrut(6));
        String[] cats = {"Technology", "Magic", "Adventure", "Utility", "Performance", "Library", "World Gen"};
        for (String cat : cats) {
            JCheckBox cb = new JCheckBox(cat);
            cb.setOpaque(false);
            cb.setForeground(new Color(185, 185, 210));
            cb.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            cb.setAlignmentX(Component.LEFT_ALIGNMENT);
            cb.addActionListener(e -> applyClientFilter());
            categoryBoxes.put(cat.toLowerCase(), cb);
            panel.add(cb);
            panel.add(Box.createVerticalStrut(2));
        }

        panel.add(Box.createVerticalStrut(14));
        panel.add(sectionLabel("INSTALL QUEUE"));
        panel.add(Box.createVerticalStrut(4));

        queueListModel = new DefaultListModel<>();
        queueList = new JList<>(queueListModel);
        queueList.setBackground(new Color(18, 18, 24));
        queueList.setForeground(new Color(155, 190, 255));
        queueList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        queueList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ModInfo) setText(((ModInfo) value).getName());
                setBackground(isSelected ? new Color(45, 75, 130) : new Color(18, 18, 24));
                setForeground(new Color(155, 190, 255));
                setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                return this;
            }
        });

        // Right-click on queue → remove item
        JPopupMenu queueMenu = new JPopupMenu();
        JMenuItem removeFromQueueItem = new JMenuItem("✖  Remove from Queue");
        removeFromQueueItem.setBackground(new Color(28, 28, 38));
        removeFromQueueItem.setForeground(new Color(220, 80, 80));
        removeFromQueueItem.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        removeFromQueueItem.addActionListener(e -> {
            ModInfo sel = queueList.getSelectedValue();
            if (sel != null) {
                installQueue.remove(sel);
                queueListModel.removeElement(sel);
            }
        });
        queueMenu.add(removeFromQueueItem);
        queueList.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
            private void maybeShow(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int idx = queueList.locationToIndex(e.getPoint());
                if (idx >= 0) queueList.setSelectedIndex(idx);
                removeFromQueueItem.setEnabled(queueList.getSelectedValue() != null);
                queueMenu.show(queueList, e.getX(), e.getY());
            }
        });

        JScrollPane qs = new JScrollPane(queueList);
        qs.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 62)));
        qs.setPreferredSize(new Dimension(168, 100));
        qs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        qs.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(qs);
        panel.add(Box.createVerticalStrut(4));

        JButton installQueueBtn = actionButton("📦 Install Queue (" + installQueue.size() + ")",
                new Color(0, 110, 55), e -> installQueue());
        JButton clearQueueBtn   = actionButton("✖ Clear Queue", new Color(90, 30, 30), e -> clearQueue());
        for (JButton b : new JButton[]{installQueueBtn, clearQueueBtn}) {
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        panel.add(installQueueBtn);
        panel.add(Box.createVerticalStrut(3));
        panel.add(clearQueueBtn);
        panel.add(Box.createVerticalGlue());

        JScrollPane sp = new JScrollPane(panel);
        sp.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(50, 50, 65)));
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(10);
        return sp;
    }

    // ── CENTER: results table ─────────────────────────────────────────────────

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(new Color(20, 20, 26));

        String[] cols = {"  Name", "Version", "Source", "⬇ Downloads", "MC Ver"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        modsTable = new JTable(tableModel);
        modsTable.setBackground(new Color(18, 18, 24));
        modsTable.setForeground(new Color(210, 210, 225));
        modsTable.setGridColor(new Color(38, 38, 50));
        modsTable.setSelectionBackground(new Color(38, 75, 140));
        modsTable.setSelectionForeground(Color.WHITE);
        modsTable.setRowHeight(28);
        modsTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        modsTable.getTableHeader().setBackground(new Color(26, 26, 34));
        modsTable.getTableHeader().setForeground(new Color(130, 130, 160));
        modsTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        modsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        modsTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        modsTable.getColumnModel().getColumn(1).setPreferredWidth(85);
        modsTable.getColumnModel().getColumn(2).setPreferredWidth(85);
        modsTable.getColumnModel().getColumn(3).setPreferredWidth(85);
        modsTable.getColumnModel().getColumn(4).setPreferredWidth(65);

        // Downloads column — right-aligned with blue tint
        DefaultTableCellRenderer dlRender = new DefaultTableCellRenderer() {
            { setHorizontalAlignment(SwingConstants.RIGHT); setOpaque(true); }
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(sel ? new Color(38, 75, 140) : new Color(18, 18, 24));
                setForeground(sel ? Color.WHITE : new Color(90, 175, 255));
                return this;
            }
        };
        modsTable.getColumnModel().getColumn(3).setCellRenderer(dlRender);

        modsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateDetailPanel();
        });

        // ── Mouse listener: double-click installs; right-click shows context menu ──
        JPopupMenu tableContextMenu = buildModTableContextMenu();
        modsTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
                    installSelectedMod();
            }
            @Override public void mousePressed(MouseEvent e)  { maybeShowPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }
            private void maybeShowPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                // Select the row under the cursor before showing the menu
                int row = modsTable.rowAtPoint(e.getPoint());
                if (row >= 0 && !modsTable.isRowSelected(row))
                    modsTable.setRowSelectionInterval(row, row);
                tableContextMenu.show(modsTable, e.getX(), e.getY());
            }
        });

        JScrollPane scroll = new JScrollPane(modsTable);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(18, 18, 24));

        JPanel tableBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        tableBar.setBackground(new Color(22, 22, 28));
        JButton installSelBtn = actionButton("📥 Install Selected", new Color(0, 115, 55), e -> installSelectedMod());
        JButton addQBtn       = actionButton("➕ Add to Queue",      new Color(45, 75, 130), e -> addSelectionToQueue());
        tableBar.add(addQBtn);
        tableBar.add(installSelBtn);

        panel.add(scroll,   BorderLayout.CENTER);
        panel.add(tableBar, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(50, 50, 65)),
                "Search Results",
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                new Font(Font.SANS_SERIF, Font.PLAIN, 11), new Color(105, 105, 135)));
        return panel;
    }

    // ── RIGHT: detail panel ───────────────────────────────────────────────────

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(new Color(20, 20, 28));

        detailNameLabel      = detailLabel("Select a mod…", new Font(Font.SANS_SERIF, Font.BOLD, 14), new Color(220, 220, 240));
        detailSourceLabel    = detailLabel("",               null,                                        new Color(110, 155, 230));
        detailAuthorLabel    = detailLabel("",               new Font(Font.SANS_SERIF, Font.ITALIC, 11), new Color(130, 130, 160));
        detailDownloadsLabel = detailLabel("",               new Font(Font.MONOSPACED, Font.PLAIN,  11), new Color(90, 175, 255));
        detailCategoriesLabel= detailLabel("",               new Font(Font.SANS_SERIF, Font.PLAIN,  11), new Color(140, 200, 100));
        detailDepsLabel      = detailLabel("",               new Font(Font.SANS_SERIF, Font.PLAIN,  11), new Color(225, 155, 50));

        JPanel metaPanel = new JPanel(new GridLayout(0, 1, 0, 3));
        metaPanel.setOpaque(false);
        for (JLabel l : new JLabel[]{detailNameLabel, detailSourceLabel, detailAuthorLabel,
                                     detailDownloadsLabel, detailCategoriesLabel, detailDepsLabel}) {
            metaPanel.add(l);
        }

        detailDescArea = new JTextArea();
        detailDescArea.setEditable(false);
        detailDescArea.setLineWrap(true);
        detailDescArea.setWrapStyleWord(true);
        detailDescArea.setBackground(new Color(16, 16, 22));
        detailDescArea.setForeground(new Color(185, 185, 210));
        detailDescArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        detailDescArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane descScroll = new JScrollPane(detailDescArea);
        descScroll.setBorder(BorderFactory.createLineBorder(new Color(44, 44, 58)));

        installSelectedBtn = actionButton("📥 Install This",  new Color(0, 120, 55),  e -> installSelectedMod());
        addToQueueBtn      = actionButton("➕ Add to Queue",   new Color(45, 75, 130), e -> addSelectionToQueue());
        installSelectedBtn.setEnabled(false);
        addToQueueBtn.setEnabled(false);

        JPanel actionRow = new JPanel(new GridLayout(1, 2, 6, 0));
        actionRow.setOpaque(false);
        actionRow.add(addToQueueBtn);
        actionRow.add(installSelectedBtn);

        panel.add(metaPanel,  BorderLayout.NORTH);
        panel.add(descScroll, BorderLayout.CENTER);
        panel.add(actionRow,  BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(50, 50, 65)),
                        "Mod Details",
                        TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                        new Font(Font.SANS_SERIF, Font.PLAIN, 11), new Color(105, 105, 135)),
                BorderFactory.createEmptyBorder(4, 8, 8, 8)));
        return panel;
    }

    // ── Bottom bar ────────────────────────────────────────────────────────────

    private JPanel createBottomBar() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(new Color(22, 22, 30));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 12, 8, 12));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setBackground(new Color(32, 32, 42));
        progressBar.setForeground(new Color(0, 180, 100));
        progressBar.setVisible(false);

        statusLabel = new JLabel("Ready — search or load suggested mods");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        statusLabel.setForeground(new Color(125, 125, 160));

        String info = String.format("Server: %s  |  MC %s  |  %s  |  Mods dir: %s",
                serverConfig.getServerName(),
                serverConfig.getMinecraftVersion(),
                serverConfig.getModLoader().getDisplayName(),
                modsDirectory.getAbsolutePath());
        JLabel infoLabel = new JLabel(info);
        infoLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 10));
        infoLabel.setForeground(new Color(80, 80, 110));

        JPanel textCol = new JPanel(new GridLayout(2, 1));
        textCol.setOpaque(false);
        textCol.add(statusLabel);
        textCol.add(infoLabel);

        panel.add(progressBar, BorderLayout.NORTH);
        panel.add(textCol,     BorderLayout.CENTER);
        return panel;
    }

    // ── Search & data ─────────────────────────────────────────────────────────

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) { ToastNotification.warning(this, "Enter a search term."); return; }
        setUIBusy(true);
        statusLabel.setText("Searching for \"" + query + "\"...");

        SwingWorker<List<ModInfo>, Void> worker = new SwingWorker<>() {
            @Override protected List<ModInfo> doInBackground() {
                List<ModInfo> results = new ArrayList<>();
                String src = (String) sourceCombo.getSelectedItem();
                try {
                    if (!"Modrinth".equals(src) && curseForgeClient.isAvailable())
                        results.addAll(curseForgeClient.searchMods(query,
                                serverConfig.getMinecraftVersion(),
                                serverConfig.getModLoader().name().toLowerCase(), 25));
                    if (!"CurseForge".equals(src) && modrinthClient.isAvailable())
                        results.addAll(modrinthClient.searchMods(query,
                                serverConfig.getMinecraftVersion(),
                                serverConfig.getModLoader().name().toLowerCase(), 25));
                } catch (Exception e) { logger.error("Search error", e); }
                return sortMods(results);
            }
            @Override protected void done() {
                try {
                    currentMods = get();
                    populateTable(currentMods);
                    statusLabel.setText("Found " + currentMods.size() + " results for \"" + query + "\"");
                } catch (Exception e) { statusLabel.setText("Search failed: " + e.getMessage()); }
                setUIBusy(false);
            }
        };
        worker.execute();
    }

    private void loadSuggestedMods() {
        setUIBusy(true);
        statusLabel.setText("Loading suggested mods…");

        SwingWorker<List<ModInfo>, Void> worker = new SwingWorker<>() {
            @Override protected List<ModInfo> doInBackground() {
                List<ModInfo> r = new ArrayList<>();
                try {
                    if (curseForgeClient.isAvailable())
                        r.addAll(curseForgeClient.getSuggestedMods(serverConfig.getMinecraftVersion(),
                                serverConfig.getModLoader().name().toLowerCase()));
                    if (modrinthClient.isAvailable())
                        r.addAll(modrinthClient.getSuggestedMods(serverConfig.getMinecraftVersion(),
                                serverConfig.getModLoader().name().toLowerCase()));
                } catch (Exception e) { logger.error("Suggested mods error", e); }
                return sortMods(r);
            }
            @Override protected void done() {
                try {
                    currentMods = get();
                    populateTable(currentMods);
                    statusLabel.setText("Loaded " + currentMods.size() + " suggested mods");
                } catch (Exception e) { statusLabel.setText("Error: " + e.getMessage()); }
                setUIBusy(false);
            }
        };
        worker.execute();
    }

    private void checkInstalledUpdates() {
        if (!modsDirectory.exists()) {
            ToastNotification.warning(this, "Mods directory not found: " + modsDirectory.getPath());
            return;
        }

        statusLabel.setText("Scanning installed mods…");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        setUIBusy(true);

        final String mcVer  = serverConfig.getMinecraftVersion();
        final String loader = serverConfig.getModLoader().name().toLowerCase();
        final ModUpdateChecker checker = new ModUpdateChecker(modsDirectory, mcVer, loader);

        SwingWorker<Void, String> worker = new SwingWorker<>() {

            /** Pairs: [0] = local ModInfo, [1] = remote Modrinth match (may be null) */
            private final java.util.List<ModInfo[]> pairs = new ArrayList<>();
            /** Mods that have a known projectId — use ModUpdateChecker for these. */
            private final java.util.List<ModUpdateChecker.UpdateInfo> updates = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                publish("Scanning mods folder…");
                java.util.List<ModInfo> installed = checker.scanInstalledMods();

                if (installed.isEmpty()) {
                    publish("No .jar files found in mods folder.");
                    return null;
                }

                publish("Found " + installed.size() + " installed mod(s)…");

                // ── Pass 1: API-tracked mods (have projectId) — version comparison ──
                java.util.List<ModInfo> trackedMods = new ArrayList<>();
                java.util.List<ModInfo> untrackedMods = new ArrayList<>();
                for (ModInfo m : installed) {
                    if (m.getProjectId() != null && !m.getProjectId().isEmpty()
                            && m.getVersion() != null) {
                        trackedMods.add(m);
                    } else {
                        untrackedMods.add(m);
                    }
                }

                if (!trackedMods.isEmpty()) {
                    publish("Checking " + trackedMods.size() + " tracked mod(s) for version updates…");
                    java.util.List<ModUpdateChecker.UpdateInfo> found =
                            checker.checkForUpdates(trackedMods, name ->
                                    publish("Checking " + name + "…"));
                    updates.addAll(found);
                }

                // ── Pass 2: Un-tracked mods — name-search on Modrinth ─────────────
                if (!untrackedMods.isEmpty()) {
                    publish("Searching Modrinth for " + untrackedMods.size() + " local mod(s)…");
                    for (int i = 0; i < untrackedMods.size(); i++) {
                        ModInfo local = untrackedMods.get(i);
                        publish("Searching for " + local.getName()
                                + " (" + (i + 1) + "/" + untrackedMods.size() + ")…");
                        try {
                            java.util.List<ModInfo> results =
                                    modrinthClient.searchMods(local.getName(), mcVer, loader, 1);
                            ModInfo remote = results.isEmpty() ? null : results.get(0);
                            if (remote != null) remote.setInstalled(true);
                            pairs.add(new ModInfo[]{local, remote});
                            Thread.sleep(110); // brief pause to respect rate-limit
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception ex) {
                            logger.warn("Search failed for {}: {}", local.getName(), ex.getMessage());
                            pairs.add(new ModInfo[]{local, null});
                        }
                    }
                }

                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) statusLabel.setText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                setUIBusy(false);

                if (pairs.isEmpty() && updates.isEmpty()) {
                    statusLabel.setText("No installed mods found in: " + modsDirectory.getPath());
                    ToastNotification.info(ModInstallerDialog.this, "Mods folder is empty.");
                    return;
                }

                tableModel.setRowCount(0);
                currentMods = new ArrayList<>();
                int updatesFound = 0;
                int modrinthFound = 0;

                // ── Show mods that have version updates available ──────────────
                for (ModUpdateChecker.UpdateInfo upd : updates) {
                    updatesFound++;
                    currentMods.add(upd.mod);
                    tableModel.addRow(new Object[]{
                        "  ⬆ " + upd.mod.getName(),
                        upd.currentVersion + " → " + upd.latestVersion,
                        upd.mod.getSource() != null ? upd.mod.getSource().getDisplayName() : "API",
                        upd.mod.getFormattedDownloads(),
                        mcVer
                    });
                }

                // ── Show name-search results for untracked mods ──────────────
                for (ModInfo[] pair : pairs) {
                    ModInfo local  = pair[0];
                    ModInfo remote = pair[1];
                    if (remote != null) {
                        modrinthFound++;
                        currentMods.add(remote);
                        tableModel.addRow(new Object[]{
                            "  ✓ " + local.getName(),
                            remote.getVersion() != null ? remote.getVersion() : "latest",
                            "Modrinth",
                            remote.getFormattedDownloads(),
                            remote.getMinecraftVersion() != null ? remote.getMinecraftVersion() : mcVer
                        });
                    } else {
                        currentMods.add(local);
                        tableModel.addRow(new Object[]{
                            "  · " + local.getName(),
                            "installed",
                            "Local only",
                            "—",
                            mcVer
                        });
                    }
                }

                int totalScanned = updates.size() + pairs.size();
                int localOnly = pairs.size() - modrinthFound;
                String summary = totalScanned + " mods scanned";
                if (updatesFound > 0) summary += "  |  ⬆ " + updatesFound + " update(s) available";
                if (modrinthFound > 0) summary += "  |  ✓ " + modrinthFound + " on Modrinth";
                if (localOnly > 0) summary += "  |  · " + localOnly + " local-only";
                statusLabel.setText(summary);

                String toastMsg = updatesFound > 0
                        ? updatesFound + " update(s) available!"
                        : "All mods up-to-date (" + totalScanned + " scanned).";
                if (updatesFound > 0) {
                    ToastNotification.warning(ModInstallerDialog.this, toastMsg);
                } else {
                    ToastNotification.success(ModInstallerDialog.this, toastMsg);
                }
            }
        };
        worker.execute();
    }

    // ── Mod-table right-click context menu ────────────────────────────────────

    private JPopupMenu buildModTableContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem installItem  = new JMenuItem("📥  Install Now");
        JMenuItem queueItem    = new JMenuItem("➕  Add to Queue");
        JMenuItem copyItem     = new JMenuItem("📋  Copy Name");
        JMenuItem openWebItem  = new JMenuItem("🌐  Open on Web");

        styleContextItem(installItem,  new Color(39, 174, 96));
        styleContextItem(queueItem,    new Color(90, 140, 220));
        styleContextItem(copyItem,     null);
        styleContextItem(openWebItem,  null);

        installItem.addActionListener(e -> installSelectedMod());
        queueItem.addActionListener(e   -> addSelectionToQueue());
        copyItem.addActionListener(e -> {
            int row = modsTable.getSelectedRow();
            if (row < 0) return;
            ModInfo mod = row < currentMods.size() ? currentMods.get(row) : null;
            if (mod != null) {
                java.awt.Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(mod.getName()), null);
                ToastNotification.info(ModInstallerDialog.this, "Copied: " + mod.getName());
            }
        });
        openWebItem.addActionListener(e -> {
            int row = modsTable.getSelectedRow();
            if (row < 0) return;
            ModInfo mod = row < currentMods.size() ? currentMods.get(row) : null;
            if (mod == null) return;
            String url = buildModUrl(mod);
            if (url != null) {
                try {
                    Desktop.getDesktop().browse(java.net.URI.create(url));
                } catch (Exception ex) {
                    ToastNotification.warning(ModInstallerDialog.this, "Cannot open browser: " + ex.getMessage());
                }
            } else {
                ToastNotification.info(ModInstallerDialog.this, "No URL available for this mod.");
            }
        });

        // Enable/disable items based on selection before the menu becomes visible
        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                boolean hasSelection = modsTable.getSelectedRow() >= 0;
                installItem.setEnabled(hasSelection);
                queueItem.setEnabled(hasSelection);
                copyItem.setEnabled(hasSelection);
                openWebItem.setEnabled(hasSelection);
            }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });

        menu.add(installItem);
        menu.add(queueItem);
        menu.addSeparator();
        menu.add(copyItem);
        menu.add(openWebItem);
        return menu;
    }

    private void styleContextItem(JMenuItem item, Color fg) {
        item.setBackground(new Color(28, 28, 38));
        item.setForeground(fg != null ? fg : new Color(210, 210, 225));
        item.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
    }

    private String buildModUrl(ModInfo mod) {
        if (mod.getUrl() != null && !mod.getUrl().isEmpty()) return mod.getUrl();
        if (mod.getSource() == ModInfo.ModSource.MODRINTH && mod.getSlug() != null)
            return "https://modrinth.com/mod/" + mod.getSlug();
        if (mod.getSource() == ModInfo.ModSource.CURSEFORGE && mod.getProjectId() != null)
            return "https://www.curseforge.com/minecraft/mc-mods/" + mod.getProjectId();
        return null;
    }

    // ── Table population + filtering ──────────────────────────────────────────

    private void populateTable(List<ModInfo> mods) {
        tableModel.setRowCount(0);
        List<String> activeCats = getActiveCategoryFilters();
        for (ModInfo mod : mods) {
            if (!activeCats.isEmpty() && !matchesCategory(mod, activeCats)) continue;
            tableModel.addRow(new Object[]{
                "  " + mod.getName(),
                mod.getVersion()          != null ? mod.getVersion()                  : "Latest",
                mod.getSource()           != null ? mod.getSource().getDisplayName()  : "",
                mod.getFormattedDownloads(),
                mod.getMinecraftVersion() != null ? mod.getMinecraftVersion()         : "N/A"
            });
        }
    }

    private void applyClientFilter() { populateTable(currentMods); }

    private List<String> getActiveCategoryFilters() {
        List<String> active = new ArrayList<>();
        categoryBoxes.forEach((k, cb) -> { if (cb.isSelected()) active.add(k); });
        return active;
    }

    private boolean matchesCategory(ModInfo mod, List<String> activeCats) {
        if (mod.getCategories() == null || mod.getCategories().isEmpty()) return true;
        for (String cat : mod.getCategories())
            for (String active : activeCats)
                if (cat.toLowerCase().contains(active)) return true;
        return false;
    }

    private List<ModInfo> sortMods(List<ModInfo> mods) {
        String sort = sortCombo != null ? (String) sortCombo.getSelectedItem() : "Most Downloads";
        List<ModInfo> sorted = new ArrayList<>(mods);
        if ("Name (A-Z)".equals(sort)) {
            sorted.sort(Comparator.comparing(m -> m.getName() != null ? m.getName() : ""));
        } else {
            sorted.sort((a, b) -> Long.compare(b.getDownloadCount(), a.getDownloadCount()));
        }
        return sorted;
    }

    // ── Detail panel ──────────────────────────────────────────────────────────

    private void updateDetailPanel() {
        int row = modsTable.getSelectedRow();
        if (row < 0) { clearDetailPanel(); return; }
        ModInfo mod = getModAtRow(row);
        if (mod == null) { clearDetailPanel(); return; }

        detailNameLabel.setText(mod.getName());
        detailSourceLabel.setText("Source: " + (mod.getSource() != null ? mod.getSource().getDisplayName() : "?"));
        detailAuthorLabel.setText(mod.getAuthor() != null ? "by " + mod.getAuthor() : "");
        detailDownloadsLabel.setText("⬇ " + mod.getFormattedDownloads() + " downloads");
        detailCategoriesLabel.setText(mod.getCategories().isEmpty() ? ""
                : "🏷 " + String.join(", ", mod.getCategories()));
        detailDepsLabel.setText(mod.getDependencies().isEmpty() ? ""
                : "🔗 " + mod.getDependencies().size() + " dependenc"
                  + (mod.getDependencies().size() == 1 ? "y" : "ies"));
        detailDescArea.setText(mod.getDescription() != null ? mod.getDescription() : "(No description)");
        detailDescArea.setCaretPosition(0);
        installSelectedBtn.setEnabled(true);
        addToQueueBtn.setEnabled(true);
    }

    private void clearDetailPanel() {
        detailNameLabel.setText("Select a mod");
        detailSourceLabel.setText(""); detailAuthorLabel.setText("");
        detailDownloadsLabel.setText(""); detailCategoriesLabel.setText("");
        detailDepsLabel.setText(""); detailDescArea.setText("");
        installSelectedBtn.setEnabled(false); addToQueueBtn.setEnabled(false);
    }

    // ── Install logic ─────────────────────────────────────────────────────────

    private void installSelectedMod() {
        int row = modsTable.getSelectedRow();
        if (row < 0) { ToastNotification.warning(this, "Select a mod first."); return; }
        ModInfo mod = getModAtRow(row);
        if (mod != null) installModList(Collections.singletonList(mod), false);
    }

    private void addSelectionToQueue() {
        int[] rows = modsTable.getSelectedRows();
        int added = 0;
        for (int r : rows) {
            ModInfo mod = getModAtRow(r);
            if (mod != null && !queueContains(mod)) {
                installQueue.add(mod);
                queueListModel.addElement(mod);
                added++;
            }
        }
        if (added > 0)
            statusLabel.setText("Added " + added + " mod(s) to queue (" + installQueue.size() + " total)");
    }

    private void installQueue() {
        if (installQueue.isEmpty()) { ToastNotification.warning(this, "Queue is empty."); return; }

        // Conflict check
        List<ConflictDetector.Conflict> conflicts = conflictDetector.detect(installQueue);
        if (!conflicts.isEmpty()) {
            StringBuilder msg = new StringBuilder("Issues found:\n\n");
            conflicts.forEach(c -> msg.append("• ").append(c).append("\n"));
            boolean blocking = conflicts.stream().anyMatch(ConflictDetector.Conflict::isBlocking);
            if (blocking) {
                JOptionPane.showMessageDialog(this, msg.toString(),
                        "Blocking Conflicts", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (JOptionPane.showConfirmDialog(this, msg.append("\nContinue anyway?").toString(),
                    "Warnings", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
                return;
        }

        // Dependency check
        DependencyResolver.Resolution res = depResolver.resolve(installQueue);
        if (!res.getMissing().isEmpty()) {
            StringBuilder msg = new StringBuilder("Missing required dependencies:\n\n");
            res.getMissing().forEach(d -> msg.append("• ").append(d.getTargetName()).append("\n"));
            msg.append("\nContinue anyway?");
            if (JOptionPane.showConfirmDialog(this, msg.toString(),
                    "Missing Dependencies", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                    != JOptionPane.YES_OPTION) return;
        }

        installModList(new ArrayList<>(installQueue), true);
    }

    private void clearQueue() {
        installQueue.clear();
        queueListModel.clear();
        statusLabel.setText("Queue cleared.");
    }

    private void installModList(List<ModInfo> mods, boolean fromQueue) {
        setUIBusy(true);
        progressBar.setVisible(true);
        progressBar.setValue(0);
        progressBar.setIndeterminate(mods.size() == 1);

        SwingWorker<int[], String> worker = new SwingWorker<>() {
            @Override protected int[] doInBackground() {
                int ok = 0, fail = 0;
                if (!modsDirectory.exists()) modsDirectory.mkdirs();
                for (int i = 0; i < mods.size(); i++) {
                    ModInfo mod = mods.get(i);
                    publish("Installing " + mod.getName() + " (" + (i+1) + "/" + mods.size() + ")…");
                    final int pct = (int)((i / (double) mods.size()) * 100);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(pct);
                    });
                    if (downloadMod(mod)) ok++; else fail++;
                }
                return new int[]{ok, fail};
            }
            @Override protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) statusLabel.setText(chunks.get(chunks.size()-1));
            }
            @Override protected void done() {
                try {
                    int[] r = get();
                    statusLabel.setText(r[0] + " installed, " + r[1] + " failed.");
                    progressBar.setValue(100);
                    if (r[1] == 0) {
                        ToastNotification.success(ModInstallerDialog.this, r[0] + " mod(s) installed!");
                        if (fromQueue) clearQueue();
                    } else {
                        ToastNotification.warning(ModInstallerDialog.this,
                                r[1] + " mod(s) failed — check logs.");
                    }
                } catch (Exception e) { logger.error("Install error", e); }
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                setUIBusy(false);
            }
        };
        worker.execute();
    }

    private boolean downloadMod(ModInfo mod) {
        try {
            String dlUrl = mod.getUrl();
            if (dlUrl == null || dlUrl.isEmpty()) {
                if (mod.getSource() == ModInfo.ModSource.CURSEFORGE)
                    dlUrl = curseForgeClient.getModDownloadUrl(mod.getProjectId(), mod.getFileId());
                else if (mod.getSource() == ModInfo.ModSource.MODRINTH)
                    dlUrl = modrinthClient.getModDownloadUrl(mod.getProjectId(), mod.getFileId());
            }
            if (dlUrl == null || dlUrl.isEmpty()) {
                logger.warn("No download URL for: {}", mod.getName());
                return false;
            }
            String fn = mod.getFileName();
            if (fn == null || fn.isEmpty())
                fn = mod.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + ".jar";

            File target = new File(modsDirectory, fn);
            URLConnection conn = new URL(dlUrl).openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(60_000);
            conn.setRequestProperty("User-Agent", "StellarServerForge/1.0.0");
            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(target)) {
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            logger.info("Downloaded: {}", target.getName());
            return true;
        } catch (Exception e) {
            logger.error("Failed to download: {}", mod.getName(), e);
            return false;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ModInfo getModAtRow(int tableRow) {
        List<String> activeCats = getActiveCategoryFilters();
        int idx = 0;
        for (ModInfo m : currentMods) {
            if (!activeCats.isEmpty() && !matchesCategory(m, activeCats)) continue;
            if (idx == tableRow) return m;
            idx++;
        }
        return null;
    }

    private boolean queueContains(ModInfo mod) {
        for (ModInfo q : installQueue)
            if (q.getProjectId() != null && q.getProjectId().equals(mod.getProjectId())) return true;
        return false;
    }

    private void setUIBusy(boolean busy) {
        searchField.setEnabled(!busy);
        sourceCombo.setEnabled(!busy);
        sortCombo.setEnabled(!busy);
        modsTable.setEnabled(!busy);
    }

    // ── Style factories ───────────────────────────────────────────────────────

    private JButton actionButton(String text, Color bg, ActionListener al) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(al);
        return btn;
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        lbl.setForeground(new Color(95, 95, 125));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel detailLabel(String text, Font font, Color fg) {
        JLabel lbl = new JLabel(text);
        if (font != null) lbl.setFont(font);
        lbl.setForeground(fg);
        return lbl;
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setBackground(new Color(32, 32, 42));
        combo.setForeground(new Color(195, 195, 215));
        combo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
    }
}

