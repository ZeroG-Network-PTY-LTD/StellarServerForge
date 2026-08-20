package com.zerog.network.stellarforge.gui;

import com.zerog.network.stellarforge.gui.components.PerformanceGraph;
import com.zerog.network.stellarforge.gui.components.ToastNotification;
import com.zerog.network.stellarforge.model.ServerConfig;
import com.zerog.network.stellarforge.model.ServerMetrics;
import com.zerog.network.stellarforge.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Server Launcher with real-time monitoring dashboard.
 *
 * Features:
 * - Color-coded, filterable console (ALL / INFO / WARN / ERROR)
 * - Console keyword search & highlight
 * - Performance graph (TPS + RAM%)
 * - Live metrics panel (TPS, RAM, players)
 * - Pre-launch health check via ProblemDetector
 * - Backup controls
 */
public class ServerLauncherDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(ServerLauncherDialog.class);

    // ── State ─────────────────────────────────────────────────────────────────
    private final ServerConfig  serverConfig;
    private final ServerManager serverManager;

    private Process       serverProcess;
    private PrintWriter   serverInput;
    private boolean       isRunning = false;
    private Thread        consoleReaderThread;
    private ServerMonitor serverMonitor;

    // All raw log lines kept for filter replay
    private final List<String> allLogLines = new ArrayList<>();

    // Current filter: "ALL", "INFO", "WARN", "ERROR"
    private String logFilter = "ALL";
    private String searchQuery = "";

    // ── Controls ──────────────────────────────────────────────────────────────
    private JButton     startButton, stopButton, restartButton;
    private JButton     filterAll, filterInfo, filterWarn, filterError;
    private JTextField  searchField;
    private JTextPane   consolePane;
    private StyledDocument consoleDoc;
    private JTextField  commandField;
    private JButton     sendCommandButton;
    private JLabel      statusLabel;
    private JProgressBar memBar;

    // Metrics widgets
    private JLabel      tpsLabel, ramLabel, playersLabel;
    private PerformanceGraph perfGraph;

    // Styles
    private Style styleInfo, styleWarn, styleError, styleDebug, styleSystem;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ServerLauncherDialog(Frame parent, ServerConfig serverConfig) {
        super(parent, "Server Launcher — " + serverConfig.getServerName(), false);
        this.serverConfig  = serverConfig;
        this.serverManager = new ServerManager(serverConfig);

        initStyles();
        initUI();
        setSize(1050, 720);
        setMinimumSize(new Dimension(800, 560));
        setLocationRelativeTo(parent);

        logger.info("Launcher dialog initialized for: {}", serverConfig.getServerName());
    }

    // ── Style setup ───────────────────────────────────────────────────────────

    private void initStyles() {
        consolePane = new JTextPane();
        consolePane.setEditable(false);
        consolePane.setBackground(new Color(15, 15, 20));
        consolePane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        consoleDoc = consolePane.getStyledDocument();

        styleInfo   = addStyle("INFO",   new Color(220, 220, 230));
        styleWarn   = addStyle("WARN",   new Color(255, 200, 60));
        styleError  = addStyle("ERROR",  new Color(255, 80, 80));
        styleDebug  = addStyle("DEBUG",  new Color(130, 130, 150));
        styleSystem = addStyle("SYSTEM", new Color(80, 200, 255));
    }

    private Style addStyle(String name, Color fg) {
        Style s = consolePane.addStyle(name, null);
        StyleConstants.setForeground(s, fg);
        StyleConstants.setFontFamily(s, Font.MONOSPACED);
        StyleConstants.setFontSize(s, 12);
        return s;
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void initUI() {
        setLayout(new BorderLayout(6, 6));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        add(createTopPanel(),    BorderLayout.NORTH);
        add(createCenterSplit(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        updateUIState();
    }

    // ── Top toolbar ────────────────────────────────────────────────────────

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setBackground(new Color(28, 28, 35));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // Server info
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(serverConfig.getServerName());
        nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        nameLabel.setForeground(Color.WHITE);

        JLabel detailLabel = new JLabel(
                "MC " + serverConfig.getMinecraftVersion()
                + "  |  " + serverConfig.getModLoader().getDisplayName()
                + "  |  Port " + serverConfig.getPort()
                + "  |  RAM " + serverConfig.getMaxRamGb() + " GB");
        detailLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        detailLabel.setForeground(new Color(150, 150, 170));

        infoPanel.add(nameLabel);
        infoPanel.add(detailLabel);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setOpaque(false);

        startButton   = toolButton("▶ Start",    new Color(0, 140, 60),  e -> startServer());
        stopButton    = toolButton("⏹ Stop",     new Color(180, 30, 30), e -> stopServer());
        restartButton = toolButton("↺ Restart",  new Color(20, 80, 180), e -> restartServer());

        JButton setupBtn  = toolButton("⚙ Setup",         new Color(60, 60, 80),  e -> setupServer());
        JButton loaderBtn = toolButton("🔧 Install Loader", new Color(60, 60, 80), e -> installModLoader());
        JButton clearBtn  = toolButton("🗑 Clear",         new Color(60, 60, 80),  e -> clearConsole());
        JButton exportBtn = toolButton("💾 Export Log",    new Color(60, 60, 80),  e -> exportLogs());
        JButton backupBtn = toolButton("💾 Backup",        new Color(60, 60, 80),  e -> quickBackup());
        JButton browseBtn = toolButton("📂 Backups",       new Color(60, 60, 80),  e -> openBackupBrowser());
        JButton checkBtn  = toolButton("🔍 Pre-check",     new Color(60, 60, 80),  e -> runPreLaunchCheck());

        btns.add(startButton); btns.add(stopButton); btns.add(restartButton);
        btns.add(new JSeparator(JSeparator.VERTICAL));
        btns.add(setupBtn); btns.add(loaderBtn); btns.add(clearBtn); btns.add(exportBtn);
        btns.add(new JSeparator(JSeparator.VERTICAL));
        btns.add(backupBtn); btns.add(browseBtn);
        btns.add(new JSeparator(JSeparator.VERTICAL));
        btns.add(checkBtn);

        panel.add(infoPanel, BorderLayout.WEST);
        panel.add(btns, BorderLayout.EAST);
        return panel;
    }

    private JButton toolButton(String text, Color bg, ActionListener al) {
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

    // ── Center split ──────────────────────────────────────────────────────────

    private JSplitPane createCenterSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createConsolePanel(), createMonitorPanel());
        split.setResizeWeight(0.72);
        split.setDividerSize(5);
        split.setBorder(null);
        return split;
    }

    // ── Console panel ─────────────────────────────────────────────────────────

    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(new Color(15, 15, 20));

        // Filter + search bar
        JPanel filterBar = new JPanel(new BorderLayout(6, 0));
        filterBar.setBackground(new Color(25, 25, 32));
        filterBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel filterBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filterBtns.setOpaque(false);

        filterAll   = filterButton("ALL",  true,  e -> setFilter("ALL"));
        filterInfo  = filterButton("INFO", false, e -> setFilter("INFO"));
        filterWarn  = filterButton("WARN", false, e -> setFilter("WARN"));
        filterError = filterButton("ERROR",false, e -> setFilter("ERROR"));

        JLabel filterLbl = new JLabel("Filter:");
        filterLbl.setForeground(new Color(150, 150, 170));
        filterBtns.add(filterLbl);
        filterBtns.add(filterAll);
        filterBtns.add(filterInfo);
        filterBtns.add(filterWarn);
        filterBtns.add(filterError);

        searchField = new JTextField(16);
        searchField.setBackground(new Color(40, 40, 50));
        searchField.setForeground(new Color(210, 210, 220));
        searchField.setCaretColor(Color.WHITE);
        searchField.putClientProperty("JTextField.placeholderText", "Search...");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });

        JButton exportBtn = new JButton("Export");
        exportBtn.setBackground(new Color(40, 40, 50));
        exportBtn.setForeground(new Color(180, 180, 200));
        exportBtn.setBorderPainted(false);
        exportBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        exportBtn.addActionListener(e -> exportLogs());

        filterBar.add(filterBtns, BorderLayout.WEST);
        filterBar.add(searchField, BorderLayout.CENTER);
        filterBar.add(exportBtn, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(consolePane);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setBorder(null);
        scroll.setBackground(new Color(15, 15, 20));

        // Auto-scroll
        DefaultCaret caret = (DefaultCaret) consolePane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // Right-click context menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem copyItem      = new JMenuItem("📋  Copy Selection");
        JMenuItem selectAllItem = new JMenuItem("☑  Select All");
        JMenuItem exportItem    = new JMenuItem("💾  Export Logs…");
        JMenuItem clearItem     = new JMenuItem("🗑  Clear Console");
        copyItem.addActionListener(e      -> consolePane.copy());
        selectAllItem.addActionListener(e -> consolePane.selectAll());
        exportItem.addActionListener(e    -> exportLogs());
        clearItem.addActionListener(e     -> clearConsole());
        popup.add(copyItem);
        popup.add(selectAllItem);
        popup.addSeparator();
        popup.add(exportItem);
        popup.addSeparator();
        popup.add(clearItem);
        consolePane.setComponentPopupMenu(popup);

        panel.add(filterBar, BorderLayout.NORTH);
        panel.add(scroll,    BorderLayout.CENTER);

        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(50, 50, 65), 1),
                "Server Console",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font(Font.SANS_SERIF, Font.PLAIN, 11),
                new Color(130, 130, 150)));
        return panel;
    }

    private JButton filterButton(String label, boolean active, ActionListener al) {
        JButton btn = new JButton(label);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        updateFilterButtonStyle(btn, active);
        btn.addActionListener(e -> {
            al.actionPerformed(e);
            updateFilterButtonStates(label);
        });
        return btn;
    }

    private void updateFilterButtonStyle(JButton btn, boolean active) {
        String label = btn.getText();
        Color activeBg = "WARN".equals(label)  ? new Color(140, 100, 0) :
                         "ERROR".equals(label) ? new Color(140, 30, 30) :
                         new Color(0, 100, 160);
        btn.setBackground(active ? activeBg : new Color(45, 45, 55));
        btn.setForeground(active ? Color.WHITE : new Color(150, 150, 170));
    }

    private void updateFilterButtonStates(String active) {
        for (JButton b : new JButton[]{filterAll, filterInfo, filterWarn, filterError}) {
            updateFilterButtonStyle(b, b.getText().equals(active));
        }
    }

    // ── Monitor panel ─────────────────────────────────────────────────────────

    private JPanel createMonitorPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(new Color(22, 22, 28));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Metrics row
        JPanel metricsRow = new JPanel(new GridLayout(3, 1, 0, 4));
        metricsRow.setOpaque(false);

        tpsLabel     = metricLabel("TPS: --", new Color(0, 200, 100));
        ramLabel     = metricLabel("RAM: --", new Color(60, 150, 255));
        playersLabel = metricLabel("Players: 0",  new Color(200, 160, 60));

        metricsRow.add(tpsLabel);
        metricsRow.add(ramLabel);
        metricsRow.add(playersLabel);

        // Performance graph
        perfGraph = new PerformanceGraph();

        // Memory progress bar
        memBar = new JProgressBar(0, 100);
        memBar.setStringPainted(true);
        memBar.setString("RAM: N/A");
        memBar.setPreferredSize(new Dimension(0, 18));
        memBar.setForeground(new Color(60, 150, 255));
        memBar.setBackground(new Color(35, 35, 45));

        JPanel graphSection = new JPanel(new BorderLayout(0, 4));
        graphSection.setOpaque(false);
        graphSection.add(perfGraph, BorderLayout.CENTER);
        graphSection.add(memBar,    BorderLayout.SOUTH);

        JLabel graphTitle = sectionLabel("📊 Performance");
        JLabel metricsTitle = sectionLabel("📈 Live Metrics");

        JPanel topSection = new JPanel(new BorderLayout(0, 4));
        topSection.setOpaque(false);
        topSection.add(metricsTitle, BorderLayout.NORTH);
        topSection.add(metricsRow,   BorderLayout.CENTER);

        JPanel bottomSection = new JPanel(new BorderLayout(0, 4));
        bottomSection.setOpaque(false);
        bottomSection.add(graphTitle,   BorderLayout.NORTH);
        bottomSection.add(graphSection, BorderLayout.CENTER);

        panel.add(topSection,    BorderLayout.NORTH);
        panel.add(bottomSection, BorderLayout.CENTER);

        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(50, 50, 65), 1),
                "Monitoring",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font(Font.SANS_SERIF, Font.PLAIN, 11),
                new Color(130, 130, 150)));
        return panel;
    }

    private JLabel metricLabel(String text, Color fg) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        lbl.setForeground(fg);
        return lbl;
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        lbl.setForeground(new Color(130, 130, 150));
        return lbl;
    }

    // ── Bottom panel (command + status) ───────────────────────────────────────

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 4));
        panel.setBackground(new Color(25, 25, 32));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));

        JPanel cmdRow = new JPanel(new BorderLayout(6, 0));
        cmdRow.setOpaque(false);

        JLabel cmdLbl = new JLabel("Command:");
        cmdLbl.setForeground(new Color(150, 150, 170));
        cmdLbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        commandField = new JTextField();
        commandField.setBackground(new Color(35, 35, 45));
        commandField.setForeground(new Color(220, 220, 230));
        commandField.setCaretColor(Color.WHITE);
        commandField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        commandField.addActionListener(e -> sendCommand());

        sendCommandButton = new JButton("Send");
        sendCommandButton.setBackground(new Color(0, 100, 60));
        sendCommandButton.setForeground(Color.WHITE);
        sendCommandButton.setBorderPainted(false);
        sendCommandButton.addActionListener(e -> sendCommand());

        cmdRow.add(cmdLbl,          BorderLayout.WEST);
        cmdRow.add(commandField,    BorderLayout.CENTER);
        cmdRow.add(sendCommandButton, BorderLayout.EAST);

        statusLabel = new JLabel("Status: Not running");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        statusLabel.setForeground(new Color(180, 60, 60));

        panel.add(cmdRow,      BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        return panel;
    }

    // ── Server operations ─────────────────────────────────────────────────────

    private void startServer() {
        if (isRunning) { appendSystem("Server is already running!"); return; }

        appendSystem("=== Starting Server ===");
        appendSystem("Time: " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    Path serverPath = serverManager.getServerPath();
                    Path serverJar  = serverPath.resolve("server.jar");

                    if (!Files.exists(serverJar)) {
                        SwingUtilities.invokeLater(() -> {
                            appendError("ERROR: server.jar not found at: " + serverJar);
                            SmartErrorDialog.show(ServerLauncherDialog.this,
                                    "Server JAR Missing",
                                    "server.jar was not found at:\n" + serverJar
                                    + "\n\nRun 'Setup Server' or place server.jar manually.",
                                    new java.io.FileNotFoundException(serverJar.toString()),
                                    null);
                        });
                        return false;
                    }

                    String javaPath = resolveJavaPath();
                    SwingUtilities.invokeLater(() -> appendSystem("Using Java: " + javaPath));

                    ProcessBuilder pb = buildServerProcess(serverPath, javaPath);
                    pb.redirectErrorStream(true);
                    serverProcess = pb.start();
                    serverInput   = new PrintWriter(serverProcess.getOutputStream(), true);
                    isRunning     = true;

                    // Start monitor
                    serverMonitor = new ServerMonitor(serverInput,
                            ServerLauncherDialog.this::updateMetrics);
                    serverMonitor.start();

                    SwingUtilities.invokeLater(() -> {
                        updateUIState();
                        setStatus("Status: Starting...", new Color(200, 150, 0));
                    });

                    // Console reader thread
                    consoleReaderThread = new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(serverProcess.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                final String out = line;
                                serverMonitor.feedLine(out);
                                SwingUtilities.invokeLater(() -> ingestLine(out));
                            }
                        } catch (IOException e) {
                            if (isRunning) logger.error("Console read error", e);
                        }
                        SwingUtilities.invokeLater(() -> {
                            isRunning = false;
                            if (serverMonitor != null) serverMonitor.stop();
                            updateUIState();
                            setStatus("Status: Stopped", new Color(180, 60, 60));
                            appendSystem("=== Server process ended ===");
                            if (serverConfig.isAutoRestart()) {
                                appendSystem("Auto-restart in 5 seconds...");
                                new Timer(5000, ev -> startServer()).start();
                            }
                        });
                    }, "ConsoleReader");
                    consoleReaderThread.setDaemon(true);
                    consoleReaderThread.start();
                    return true;

                } catch (Exception e) {
                    logger.error("Error starting server", e);
                    SwingUtilities.invokeLater(() -> {
                        SmartErrorDialog.show(ServerLauncherDialog.this,
                                "Starting Server", e, ServerLauncherDialog.this::startServer);
                    });
                    return false;
                }
            }
        };
        worker.execute();
    }

    private String resolveJavaPath() {
        String javaPath = serverConfig.getCustomJavaPath();
        if (javaPath == null || javaPath.isEmpty()) {
            JavaManager.JavaInstallation best =
                    JavaManager.findBestJava(serverConfig.getMinecraftVersion());
            if (best != null && !"java".equals(best.getPath())) {
                String ext = System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "";
                javaPath = Paths.get(best.getPath(), "bin", "java" + ext).toString();
            } else {
                javaPath = "java";
            }
        }
        return javaPath;
    }

    private ProcessBuilder buildServerProcess(Path serverPath, String javaPath) {
        int minRam = Math.max(1, serverConfig.getMaxRamGb() / 2);
        StringBuilder cmd = new StringBuilder(javaPath)
                .append(" -Xms").append(minRam).append("G")
                .append(" -Xmx").append(serverConfig.getMaxRamGb()).append("G");
        if (serverConfig.getJvmArgs() != null && !serverConfig.getJvmArgs().isEmpty())
            cmd.append(" ").append(serverConfig.getJvmArgs());
        cmd.append(" -jar server.jar nogui");

        final String cmdStr = cmd.toString();
        SwingUtilities.invokeLater(() -> {
            appendSystem("Command: " + cmdStr);
            appendSystem("Dir:     " + serverPath);
            appendSystem("");
        });

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(serverPath.toFile());
        boolean win = System.getProperty("os.name").toLowerCase().contains("win");
        pb.command(win ? new String[]{"cmd", "/c", cmdStr}
                       : new String[]{"/bin/sh", "-c", cmdStr});
        return pb;
    }

    private void stopServer() {
        if (!isRunning || serverProcess == null) { appendSystem("Server is not running"); return; }
        appendSystem("Stopping server...");
        try {
            serverInput.println("stop");
            serverInput.flush();
            Thread t = new Thread(() -> {
                try {
                    boolean ok = serverProcess.waitFor(30,
                            java.util.concurrent.TimeUnit.SECONDS);
                    if (!ok) SwingUtilities.invokeLater(() -> {
                        appendError("Forcibly killing server process...");
                        serverProcess.destroyForcibly();
                    });
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            logger.error("Error stopping server", e);
            serverProcess.destroyForcibly();
        }
    }

    private void restartServer() {
        if (isRunning) {
            appendSystem("Restarting...");
            Thread t = new Thread(() -> {
                stopServer();
                try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                SwingUtilities.invokeLater(this::startServer);
            });
            t.setDaemon(true);
            t.start();
        } else {
            startServer();
        }
    }

    private void sendCommand() {
        if (!isRunning || serverInput == null) { appendError("Server not running"); return; }
        String cmd = commandField.getText().trim();
        if (cmd.isEmpty()) return;
        appendSystem("> " + cmd);
        serverInput.println(cmd);
        serverInput.flush();
        commandField.setText("");
    }

    // ── Console helpers ───────────────────────────────────────────────────────

    /** Ingest a raw console line: store it, classify it, and display if passes filter */
    private void ingestLine(String line) {
        allLogLines.add(line);

        // Parse startup status changes
        if (LogParser.isStartupComplete(line)) {
            setStatus("Status: Running ●", new Color(0, 180, 80));
        } else if (line.contains("Stopping server")) {
            setStatus("Status: Stopping...", new Color(200, 150, 0));
        }

        displayLine(line);
    }

    private void displayLine(String line) {
        LogParser.LogLevel level = LogParser.classifyLine(line);

        // Apply current filter
        if (!passesFilter(level, line)) return;

        Style s;
        switch (level) {
            case WARN:   s = styleWarn;  break;
            case ERROR:  s = styleError; break;
            case DEBUG:  s = styleDebug; break;
            default:     s = styleInfo;  break;
        }

        // Highlight search term in style
        appendStyledLine(line + "\n", s);
    }

    private void appendStyledLine(String text, Style style) {
        try {
            consoleDoc.insertString(consoleDoc.getLength(), text, style);
        } catch (BadLocationException e) {
            logger.debug("Console append error", e);
        }
    }

    private void appendSystem(String text) {
        try {
            consoleDoc.insertString(consoleDoc.getLength(), text + "\n", styleSystem);
        } catch (BadLocationException e) {
            logger.debug("Console append error", e);
        }
    }

    private void appendError(String text) {
        try {
            consoleDoc.insertString(consoleDoc.getLength(), text + "\n", styleError);
        } catch (BadLocationException e) {
            logger.debug("Console append error", e);
        }
    }

    private void setFilter(String filter) {
        this.logFilter = filter;
        applyFilter();
    }

    private void applyFilter() {
        this.searchQuery = searchField.getText().trim().toLowerCase();
        // Wipe & replay
        try {
            consoleDoc.remove(0, consoleDoc.getLength());
        } catch (BadLocationException ignored) {}
        for (String line : allLogLines) {
            displayLine(line);
        }
    }

    private boolean passesFilter(LogParser.LogLevel level, String line) {
        boolean levelOk;
        switch (logFilter) {
            case "INFO":  levelOk = level == LogParser.LogLevel.INFO;  break;
            case "WARN":  levelOk = level == LogParser.LogLevel.WARN;  break;
            case "ERROR": levelOk = level == LogParser.LogLevel.ERROR; break;
            default:      levelOk = true;
        }
        if (!levelOk) return false;
        if (!searchQuery.isEmpty() && !line.toLowerCase().contains(searchQuery)) return false;
        return true;
    }

    private void clearConsole() {
        allLogLines.clear();
        try { consoleDoc.remove(0, consoleDoc.getLength()); } catch (BadLocationException ignored) {}
    }

    private void exportLogs() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("server-log-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + ".txt"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter pw = new PrintWriter(fc.getSelectedFile())) {
            allLogLines.forEach(pw::println);
            ToastNotification.success(this, "Log exported: " + fc.getSelectedFile().getName());
        } catch (Exception e) {
            SmartErrorDialog.show(this, "Exporting Logs", e, null);
        }
    }

    // ── Metrics update ────────────────────────────────────────────────────────

    private void updateMetrics(ServerMetrics metrics) {
        tpsLabel.setText(String.format("TPS: %.1f / %.1f / %.1f",
                metrics.getTps1m(), metrics.getTps5m(), metrics.getTps15m()));
        Color tpsColor = metrics.getTps1m() >= 18 ? new Color(0, 200, 100)
                : metrics.getTps1m() >= 15 ? new Color(220, 180, 0)
                : new Color(255, 80, 80);
        tpsLabel.setForeground(tpsColor);

        if (metrics.getMemMaxMb() > 0) {
            ramLabel.setText(String.format("RAM: %d / %d MB (%d%%)",
                    metrics.getMemUsedMb(), metrics.getMemMaxMb(), metrics.memPercent()));
            memBar.setValue(metrics.memPercent());
            memBar.setString(String.format("RAM %d%%  (%d/%d MB)",
                    metrics.memPercent(), metrics.getMemUsedMb(), metrics.getMemMaxMb()));
            memBar.setForeground(metrics.memPercent() > 85 ? new Color(255, 80, 80)
                    : metrics.memPercent() > 70 ? new Color(220, 180, 0)
                    : new Color(60, 150, 255));
        }

        playersLabel.setText("Players: " + metrics.getPlayerCount() + " / " + metrics.getMaxPlayers());
        perfGraph.addMetrics(metrics);
    }

    // ── Pre-launch health check ───────────────────────────────────────────────

    private void runPreLaunchCheck() {
        appendSystem("=== Pre-Launch Health Check ===");

        SwingWorker<List<ProblemDetector.Problem>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ProblemDetector.Problem> doInBackground() {
                return ProblemDetector.checkPreLaunch(
                        serverManager.getServerPath(),
                        serverConfig.getPort(),
                        serverConfig.getMaxRamGb());
            }

            @Override
            protected void done() {
                try {
                    List<ProblemDetector.Problem> problems = get();
                    if (problems.isEmpty()) {
                        appendSystem("✓ All checks passed — server is ready to launch!");
                        ToastNotification.success(ServerLauncherDialog.this,
                                "Pre-launch checks passed!");
                    } else {
                        for (ProblemDetector.Problem p : problems) {
                            String prefix = p.severity == ProblemDetector.Severity.ERROR ? "✗"
                                    : p.severity == ProblemDetector.Severity.WARNING ? "⚠" : "ℹ";
                            appendSystem(prefix + " " + p.title + ": " + p.description);
                            appendSystem("   → " + p.suggestion);
                        }
                        boolean hasError = problems.stream().anyMatch(
                                p -> p.severity == ProblemDetector.Severity.ERROR);
                        if (hasError) {
                            ToastNotification.error(ServerLauncherDialog.this,
                                    "Pre-launch check found critical issues!");
                        } else {
                            ToastNotification.warning(ServerLauncherDialog.this,
                                    "Pre-launch check found warnings. Review console.");
                        }
                    }
                } catch (Exception e) {
                    logger.error("Pre-launch check error", e);
                }
            }
        };
        worker.execute();
    }

    // ── Setup / mod loader ────────────────────────────────────────────────────

    private void setupServer() {
        appendSystem("=== Server Setup ===");
        SwingWorker<Boolean, String> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    publish("Initializing server directory...");
                    if (!serverManager.initializeServer()) { publish("ERROR: Init failed"); return false; }
                    publish("✓ Server directory initialized");

                    publish("Accepting EULA...");
                    if (!serverManager.acceptEula()) { publish("ERROR: EULA failed"); return false; }
                    publish("✓ EULA accepted");

                    publish("Generating server.properties...");
                    if (!serverManager.generateServerProperties()) {
                        publish("ERROR: server.properties failed"); return false; }
                    publish("✓ server.properties generated");

                    publish("Creating start script...");
                    if (!serverManager.createStartScript()) { publish("ERROR: Start script failed"); return false; }
                    publish("✓ Start script created");
                    return true;
                } catch (Exception e) {
                    publish("ERROR: " + e.getMessage());
                    logger.error("Setup error", e);
                    return false;
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                chunks.forEach(m -> {
                    if (m.startsWith("ERROR")) appendError(m); else appendSystem(m);
                });
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        appendSystem("✓ Setup complete! Place server.jar and install mod loader.");
                        ToastNotification.success(ServerLauncherDialog.this, "Server setup complete!");
                    } else {
                        appendError("✗ Setup failed — check console for details.");
                    }
                } catch (Exception e) { logger.error("Setup worker error", e); }
            }
        };
        worker.execute();
    }

    private void installModLoader() {
        String loaderName = serverConfig.getModLoader().getDisplayName();
        boolean supported = serverConfig.getModLoader() == ServerConfig.ModLoader.FORGE
                || serverConfig.getModLoader() == ServerConfig.ModLoader.FABRIC
                || serverConfig.getModLoader() == ServerConfig.ModLoader.QUILT
                || serverConfig.getModLoader() == ServerConfig.ModLoader.NEOFORGE;

        if (!supported) {
            JOptionPane.showMessageDialog(this,
                    loaderName + " must be installed manually.", "Manual Install Required",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int r = JOptionPane.showConfirmDialog(this,
                "Install " + loaderName + " for MC " + serverConfig.getMinecraftVersion() + "?",
                "Install Loader", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        appendSystem("=== Installing " + loaderName + " ===");

        SwingWorker<Boolean, String> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return serverManager.installModLoader((pct, msg) -> publish(msg));
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                chunks.forEach(m -> {
                    if (m.toUpperCase().contains("ERROR")) appendError(m); else appendSystem(m);
                });
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        appendSystem("✓ " + loaderName + " installed!");
                        ToastNotification.success(ServerLauncherDialog.this, loaderName + " installed!");
                    } else {
                        appendError("✗ " + loaderName + " installation failed.");
                    }
                } catch (Exception e) { logger.error("Loader install error", e); }
            }
        };
        worker.execute();
    }

    // ── Backup helpers ────────────────────────────────────────────────────────

    private void quickBackup() {
        if (isRunning) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Server is running. A live world backup may be slightly inconsistent.\nContinue?",
                    "Live Backup Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
        }
        appendSystem("=== Starting world backup ===");

        SwingWorker<java.nio.file.Path, String> worker = new SwingWorker<>() {
            @Override
            protected java.nio.file.Path doInBackground() {
                return BackupManager.createBackup(
                        serverManager.getServerPath(),
                        serverConfig.getServerName(),
                        BackupManager.BackupType.WORLD_ONLY,
                        (pct, msg) -> publish(pct + "% " + msg));
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                chunks.forEach(ServerLauncherDialog.this::appendSystem);
            }

            @Override
            protected void done() {
                try {
                    java.nio.file.Path zip = get();
                    if (zip != null) {
                        appendSystem("✓ Backup saved: " + zip.getFileName());
                        ToastNotification.success(ServerLauncherDialog.this,
                                "World backed up: " + zip.getFileName());
                    } else {
                        appendError("✗ Backup failed");
                        ToastNotification.error(ServerLauncherDialog.this, "Backup failed.");
                    }
                } catch (Exception e) { logger.error("Backup error", e); }
            }
        };
        worker.execute();
    }

    private void openBackupBrowser() {
        java.util.List<BackupManager.BackupEntry> backups =
                BackupManager.listBackups(serverConfig.getServerName());

        JDialog dlg = new JDialog(this, "Backup Browser — " + serverConfig.getServerName(), true);
        dlg.setSize(580, 380);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(8, 8));

        String[] cols = {"Backup File", "Size", "Created"};
        Object[][] rows = new Object[backups.size()][3];
        for (int i = 0; i < backups.size(); i++) {
            BackupManager.BackupEntry e = backups.get(i);
            rows[i] = new Object[]{e.getDisplayName(), e.getFormattedSize(), e.created};
        }
        javax.swing.table.DefaultTableModel model =
                new javax.swing.table.DefaultTableModel(rows, cols) {
                    @Override public boolean isCellEditable(int r, int c) { return false; }
                };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (!backups.isEmpty()) table.setRowSelectionInterval(0, 0);
        dlg.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton restoreBtn = new JButton("↩ Restore");
        restoreBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            BackupManager.BackupEntry entry = backups.get(row);
            if (JOptionPane.showConfirmDialog(dlg, "Restore \"" + entry.getDisplayName() + "\"?",
                    "Confirm Restore", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                BackupManager.restoreBackup(entry.path, serverManager.getServerPath(),
                        (pct, msg) -> SwingUtilities.invokeLater(
                                () -> appendSystem(pct + "% " + msg)));
                dlg.dispose();
                ToastNotification.success(this, "Restore complete!");
            }
        });
        JButton deleteBtn = new JButton("🗑 Delete");
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            BackupManager.BackupEntry entry = backups.get(row);
            if (JOptionPane.showConfirmDialog(dlg, "Delete \"" + entry.getDisplayName() + "\"?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                BackupManager.deleteBackup(entry.path);
                backups.remove(row);
                model.removeRow(row);
            }
        });
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dlg.dispose());
        btns.add(restoreBtn); btns.add(deleteBtn); btns.add(closeBtn);
        dlg.add(btns, BorderLayout.SOUTH);

        if (backups.isEmpty()) {
            dlg.add(new JLabel("No backups found.", SwingConstants.CENTER),
                    BorderLayout.NORTH);
        }
        dlg.setVisible(true);
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void updateUIState() {
        startButton.setEnabled(!isRunning);
        stopButton.setEnabled(isRunning);
        commandField.setEnabled(isRunning);
        sendCommandButton.setEnabled(isRunning);
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    // ── Dispose ───────────────────────────────────────────────────────────────

    @Override
    public void dispose() {
        if (isRunning && serverProcess != null) {
            if (JOptionPane.showConfirmDialog(this,
                    "Server is still running. Stop before closing?",
                    "Server Running", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                stopServer();
            }
        }
        if (serverMonitor != null) serverMonitor.stop();
        super.dispose();
    }
}


