package com.zerog.network.stellarforge.gui;

import com.zerog.network.stellarforge.model.ConfigTemplate;
import com.zerog.network.stellarforge.model.ServerConfig;
import com.zerog.network.stellarforge.utils.ConfigValidator;
import com.zerog.network.stellarforge.utils.JavaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Enhanced server-configuration dialog (v2.0)
 * – Tabbed layout (Basic / Performance / Server Properties / Java / Advanced)
 * – Real-time validation with coloured indicators
 * – One-click config templates
 * – Preview panel
 */
public class ServerConfigDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(ServerConfigDialog.class);

    private final ServerConfig config;
    private boolean configChanged = false;

    // ── Tab 1: Basic ──────────────────────────────────────────────────────────
    private JTextField serverNameField;
    private JTextField serverPathField;
    private JComboBox<String> minecraftVersionCombo;
    private JComboBox<ServerConfig.ModLoader> modLoaderCombo;

    // ── Tab 2: Performance ────────────────────────────────────────────────────
    private JSpinner ramSpinner;
    private JSpinner portSpinner;
    private JSpinner viewDistSpinner;

    // ── Tab 3: Server Properties ──────────────────────────────────────────────
    private JComboBox<String> gameModeCombo;
    private JComboBox<String> difficultyCombo;
    private JSpinner maxPlayersSpinner;
    private JTextField motdField;
    private JCheckBox pvpCheck;
    private JCheckBox onlineModeCheck;
    private JCheckBox allowFlightCheck;
    private JCheckBox whitelistCheck;
    private JCheckBox commandBlocksCheck;

    // ── Tab 4: Java ───────────────────────────────────────────────────────────
    private JComboBox<String> javaCombo;
    private JTextField customJavaField;
    private JButton browseJavaButton;
    private JTextArea jvmArgsArea;
    private List<JavaManager.JavaInstallation> detectedJavas;

    // ── Tab 5: Advanced ───────────────────────────────────────────────────────
    private JCheckBox autoRestartCheck;
    private JCheckBox upnpCheck;

    // ── Footer validation ─────────────────────────────────────────────────────
    private JLabel validationLabel;
    private JTextArea previewArea;

    public ServerConfigDialog(Frame parent, ServerConfig config) {
        super(parent, "Configure Server — " + config.getServerName(), true);
        this.config = config;

        setLayout(new BorderLayout(10, 10));
        add(buildTemplateBar(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("⚙ Basic",             buildBasicTab());
        tabs.addTab("⚡ Performance",       buildPerformanceTab());
        tabs.addTab("🎮 Server Properties", buildServerPropsTab());
        tabs.addTab("☕ Java",              buildJavaTab());
        tabs.addTab("🔧 Advanced",          buildAdvancedTab());
        tabs.addTab("👁 Preview",           buildPreviewTab());

        tabs.addChangeListener(e -> updatePreview());
        add(tabs, BorderLayout.CENTER);

        validationLabel = new JLabel(" ");
        validationLabel.setBorder(new EmptyBorder(4, 12, 0, 12));

        add(buildButtonPanel(), BorderLayout.SOUTH);

        loadCurrentConfig();
        detectJavaInstallations();

        setSize(760, 600);
        setMinimumSize(new Dimension(680, 520));
        setLocationRelativeTo(parent);
        setResizable(true);
    }

    // ── Template bar ──────────────────────────────────────────────────────────

    private JPanel buildTemplateBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Quick Templates",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 11)));

        addTemplateButton(bar, "🏠 Vanilla",
                ConfigTemplate.TemplateType.VANILLA,     "1.20.6", ServerConfig.ModLoader.FABRIC);
        addTemplateButton(bar, "⚒ Forge Modded",
                ConfigTemplate.TemplateType.MODDED,      "1.20.1", ServerConfig.ModLoader.FORGE);
        addTemplateButton(bar, "🎨 Creative",
                ConfigTemplate.TemplateType.CREATIVE,    "1.20.4", ServerConfig.ModLoader.FABRIC);
        addTemplateButton(bar, "⚡ Performance",
                ConfigTemplate.TemplateType.PERFORMANCE, "1.20.4", ServerConfig.ModLoader.FABRIC);
        addTemplateButton(bar, "🌐 Public Server",
                ConfigTemplate.TemplateType.LARGE_SERVER,"1.20.1", ServerConfig.ModLoader.FORGE);

        return bar;
    }

    private void addTemplateButton(JPanel bar, String label,
                                   ConfigTemplate.TemplateType type,
                                   String mcVer, ServerConfig.ModLoader loader) {
        JButton btn = new JButton(label);
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        btn.setToolTipText(type.description);
        btn.addActionListener(e -> applyTemplate(type, mcVer, loader));
        bar.add(btn);
    }

    /**
     * Apply a ConfigTemplate preset and sync every UI field to match the
     * updated config values (JVM args, view distance, max players, etc. included).
     */
    private void applyTemplate(ConfigTemplate.TemplateType type,
                                String mcVer, ServerConfig.ModLoader loader) {
        ConfigTemplate.apply(type, config);               // update model
        // sync all UI widgets from the freshly updated model
        minecraftVersionCombo.setSelectedItem(mcVer);
        modLoaderCombo.setSelectedItem(loader);
        ramSpinner.setValue(config.getMaxRamGb());
        viewDistSpinner.setValue(config.getViewDistance());
        gameModeCombo.setSelectedItem(capitalize(config.getGameMode()));
        difficultyCombo.setSelectedItem(capitalize(config.getDifficulty()));
        maxPlayersSpinner.setValue(config.getMaxPlayers());
        pvpCheck.setSelected(config.isPvpEnabled());
        jvmArgsArea.setText(config.getJvmArgs() != null ? config.getJvmArgs() : "");
        updatePreview();
        showValidation("✓ Template applied: " + type.label, new Color(39, 174, 96));
    }

    // ── Tab builders ──────────────────────────────────────────────────────────

    private JPanel buildBasicTab() {
        JPanel panel = tab();
        GridBagConstraints g = gbc();

        addRow(panel, g, 0, "Server Name:",    serverNameField    = new JTextField(20));
        addRow(panel, g, 1, "Server Path:",    buildPathRow());
        addRow(panel, g, 2, "Minecraft Ver.:", minecraftVersionCombo = buildVersionCombo());
        addRow(panel, g, 3, "Mod Loader:",     modLoaderCombo     = new JComboBox<>(ServerConfig.ModLoader.values()));

        addLiveValidation(serverNameField);
        addLiveValidation(serverPathField);

        return panel;
    }

    private Component buildPathRow() {
        serverPathField = new JTextField(20);
        JButton browse = new JButton("Browse…");
        browse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (!serverPathField.getText().isEmpty())
                fc.setCurrentDirectory(new File(serverPathField.getText()));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                serverPathField.setText(fc.getSelectedFile().getAbsolutePath());
        });
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.add(serverPathField, BorderLayout.CENTER);
        p.add(browse, BorderLayout.EAST);
        return p;
    }

    private JComboBox<String> buildVersionCombo() {
        JComboBox<String> c = new JComboBox<>(new String[]{
            "1.20.6","1.20.4","1.20.2","1.20.1",
            "1.19.4","1.19.2","1.18.2","1.17.1","1.16.5","1.12.2"
        });
        c.setEditable(true);
        return c;
    }

    private JPanel buildPerformanceTab() {
        JPanel panel = tab();
        GridBagConstraints g = gbc();

        ramSpinner      = new JSpinner(new SpinnerNumberModel(4, 1, 64, 1));
        portSpinner     = new JSpinner(new SpinnerNumberModel(25565, 1, 65535, 1));
        viewDistSpinner = new JSpinner(new SpinnerNumberModel(10, 2, 32, 1));

        addRow(panel, g, 0, "Max RAM (GB):",      wrapUnit(ramSpinner,      "GB  (min = half of max)"));
        addRow(panel, g, 1, "Server Port:",        wrapUnit(portSpinner,     "(1 – 65535)"));
        addRow(panel, g, 2, "View Distance (chunks):", wrapUnit(viewDistSpinner, "(2 – 32)"));

        // RAM recommendation note
        g.gridy = 3; g.gridx = 0; g.gridwidth = 2; g.insets = new Insets(8, 10, 2, 10);
        JLabel note = new JLabel("<html><i>Recommended RAM: 2–4 GB for small servers, 6–16 GB for heavy modpacks</i></html>");
        note.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        panel.add(note, g);

        return panel;
    }

    private JPanel buildServerPropsTab() {
        JPanel panel = tab();
        GridBagConstraints g = gbc();

        gameModeCombo    = new JComboBox<>(new String[]{"Survival","Creative","Adventure","Spectator"});
        difficultyCombo  = new JComboBox<>(new String[]{"Peaceful","Easy","Normal","Hard"});
        maxPlayersSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 500, 1));
        motdField        = new JTextField("A Minecraft Server");
        motdField.setToolTipText("Minecraft MOTD — max 59 characters");
        addLiveValidation(motdField);   // validates the 59-char cap

        addRow(panel, g, 0, "Game Mode:",    gameModeCombo);
        addRow(panel, g, 1, "Difficulty:",   difficultyCombo);
        addRow(panel, g, 2, "Max Players:",  maxPlayersSpinner);
        addRow(panel, g, 3, "MOTD:",         motdField);

        // Checkboxes row
        g.gridy = 4; g.gridx = 0; g.gridwidth = 2; g.insets = new Insets(10, 10, 2, 10);
        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        pvpCheck           = checkbox("PvP",             true);
        onlineModeCheck    = checkbox("Online Mode",     true);
        allowFlightCheck   = checkbox("Allow Flight",    false);
        whitelistCheck     = checkbox("Whitelist",       false);
        commandBlocksCheck = checkbox("Command Blocks",  true);
        checks.add(pvpCheck);
        checks.add(onlineModeCheck);
        checks.add(allowFlightCheck);
        checks.add(whitelistCheck);
        checks.add(commandBlocksCheck);
        panel.add(checks, g);

        return panel;
    }

    private JPanel buildJavaTab() {
        JPanel panel = tab();
        GridBagConstraints g = gbc();

        javaCombo = new JComboBox<>();
        javaCombo.addItem("Auto-detect (Recommended)");
        javaCombo.addItem("Use System Java");
        javaCombo.addItem("Custom Path…");
        javaCombo.addActionListener(e -> handleJavaSelection());

        customJavaField = new JTextField(20);
        customJavaField.setEnabled(false);
        browseJavaButton = new JButton("Browse…");
        browseJavaButton.setEnabled(false);
        browseJavaButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                customJavaField.setText(fc.getSelectedFile().getAbsolutePath());
        });
        JPanel javaPathRow = new JPanel(new BorderLayout(4, 0));
        javaPathRow.add(customJavaField, BorderLayout.CENTER);
        javaPathRow.add(browseJavaButton, BorderLayout.EAST);

        addRow(panel, g, 0, "Java Installation:", javaCombo);
        addRow(panel, g, 1, "Custom Path:",        javaPathRow);

        g.gridy = 2; g.gridx = 0; g.gridwidth = 2;
        panel.add(new JLabel("<html><i>Required version is auto-selected based on Minecraft version.</i></html>"), g);
        g.gridy = 3;
        panel.add(new JLabel("JVM Arguments:"), g);

        g.gridy = 4; g.weighty = 1.0; g.fill = GridBagConstraints.BOTH;
        jvmArgsArea = new JTextArea(4, 40);
        jvmArgsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        jvmArgsArea.setLineWrap(true);
        panel.add(new JScrollPane(jvmArgsArea), g);
        g.weighty = 0; g.fill = GridBagConstraints.HORIZONTAL;

        g.gridy = 5;
        JButton restoreArgsBtn = new JButton("↻ Restore Default JVM Args");
        restoreArgsBtn.addActionListener(e -> setDefaultJvmArgs());
        panel.add(restoreArgsBtn, g);

        return panel;
    }

    private JPanel buildAdvancedTab() {
        JPanel panel = tab();
        GridBagConstraints g = gbc();

        autoRestartCheck = checkbox("Auto-restart server on crash", false);
        upnpCheck        = checkbox("Enable UPnP port forwarding (experimental)", false);
        upnpCheck.setEnabled(false);

        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        panel.add(autoRestartCheck, g);
        g.gridy = 1;
        panel.add(upnpCheck, g);

        return panel;
    }

    private JPanel buildPreviewTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.add(new JLabel("Generated server.properties preview:"), BorderLayout.NORTH);

        previewArea = new JTextArea();
        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        panel.add(new JScrollPane(previewArea), BorderLayout.CENTER);

        return panel;
    }

    private void updatePreview() {
        if (previewArea == null) return;
        StringBuilder sb = new StringBuilder("# server.properties (generated preview)\n\n");
        append(sb, "gamemode",       gameModeCombo   != null ? gameModeCombo.getSelectedItem().toString().toLowerCase() : "survival");
        append(sb, "difficulty",     difficultyCombo != null ? difficultyCombo.getSelectedItem().toString().toLowerCase() : "normal");
        append(sb, "max-players",    maxPlayersSpinner != null ? maxPlayersSpinner.getValue().toString() : "20");
        append(sb, "motd",           motdField != null ? motdField.getText() : "A Minecraft Server");
        append(sb, "pvp",            pvpCheck != null ? String.valueOf(pvpCheck.isSelected()) : "true");
        append(sb, "online-mode",    onlineModeCheck != null ? String.valueOf(onlineModeCheck.isSelected()) : "true");
        append(sb, "allow-flight",   allowFlightCheck != null ? String.valueOf(allowFlightCheck.isSelected()) : "false");
        append(sb, "white-list",     whitelistCheck != null ? String.valueOf(whitelistCheck.isSelected()) : "false");
        append(sb, "enable-command-block", commandBlocksCheck != null ? String.valueOf(commandBlocksCheck.isSelected()) : "true");
        append(sb, "server-port",    portSpinner != null ? portSpinner.getValue().toString() : "25565");
        append(sb, "view-distance",  viewDistSpinner != null ? viewDistSpinner.getValue().toString() : "10");
        previewArea.setText(sb.toString());
        previewArea.setCaretPosition(0);
    }

    private void append(StringBuilder sb, String k, String v) {
        sb.append(k).append("=").append(v).append("\n");
    }

    // ── Button panel ──────────────────────────────────────────────────────────

    private JPanel buildButtonPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.add(validationLabel, BorderLayout.NORTH);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton reset  = new JButton("↺ Reset Defaults");
        JButton cancel = new JButton("Cancel");
        JButton save   = new JButton("💾 Save");
        save.setBackground(new Color(39, 174, 96));
        save.setForeground(Color.WHITE);
        save.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        reset.addActionListener(e  -> resetToDefaults());
        cancel.addActionListener(e -> handleCancel());
        save.addActionListener(e   -> saveConfiguration());

        btns.add(reset);
        btns.add(cancel);
        btns.add(save);
        outer.add(btns, BorderLayout.CENTER);
        return outer;
    }

    // ── Validation & load/save ────────────────────────────────────────────────

    private void addLiveValidation(JTextField field) {
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateForm(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateForm(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateForm(); }
        });
    }

    private void validateForm() {
        if (serverNameField == null || serverPathField == null) return;
        String name = serverNameField.getText().trim();
        String path = serverPathField.getText().trim();

        if (name.isEmpty()) {
            showValidation("⚠ Server name is required", new Color(230, 126, 34));
        } else if (name.length() > 64) {
            showValidation("⚠ Server name is too long (max 64 chars)", new Color(230, 126, 34));
        } else if (path.isEmpty()) {
            showValidation("⚠ Server path is required", new Color(230, 126, 34));
        } else if (motdField != null && motdField.getText().length() > 59) {
            showValidation("⚠ MOTD is too long — Minecraft cap is 59 characters", new Color(230, 126, 34));
        } else {
            showValidation("✓ Configuration looks good", new Color(39, 174, 96));
        }
    }

    private void showValidation(String msg, Color color) {
        if (validationLabel == null) return;
        validationLabel.setText(msg);
        validationLabel.setForeground(color);
    }

    private void loadCurrentConfig() {
        serverNameField.setText(config.getServerName());
        serverPathField.setText(config.getServerPath() != null ? config.getServerPath() : "");
        minecraftVersionCombo.setSelectedItem(config.getMinecraftVersion());
        modLoaderCombo.setSelectedItem(config.getModLoader());
        ramSpinner.setValue(config.getMaxRamGb());
        portSpinner.setValue(config.getPort());
        viewDistSpinner.setValue(config.getViewDistance());
        gameModeCombo.setSelectedItem(capitalize(config.getGameMode()));
        difficultyCombo.setSelectedItem(capitalize(config.getDifficulty()));
        maxPlayersSpinner.setValue(config.getMaxPlayers());
        motdField.setText(config.getMotd());
        pvpCheck.setSelected(config.isPvpEnabled());
        onlineModeCheck.setSelected(config.isOnlineModeEnabled());
        allowFlightCheck.setSelected(config.isAllowFlight());
        whitelistCheck.setSelected(config.isWhitelistEnabled());
        commandBlocksCheck.setSelected(config.isCommandBlocksEnabled());
        if (config.getCustomJavaPath() != null && !config.getCustomJavaPath().isEmpty()) {
            javaCombo.setSelectedItem("Custom Path…");
            customJavaField.setText(config.getCustomJavaPath());
            customJavaField.setEnabled(true);
            browseJavaButton.setEnabled(true);
        }
        jvmArgsArea.setText(config.getJvmArgs() != null ? config.getJvmArgs() : "");
        autoRestartCheck.setSelected(config.isAutoRestart());
        setDefaultJvmArgs();
        updatePreview();
    }

    private void saveConfiguration() {
        // ── Validate key fields before modifying the config object ───────────
        ConfigValidator.FieldStatus ns =
                ConfigValidator.validateServerName(serverNameField.getText().trim());
        if (ns.isError()) {
            showValidation("✗ Name: " + ns.message, new Color(220, 60, 60));
            return;
        }
        ConfigValidator.FieldStatus ps =
                ConfigValidator.validateServerPath(serverPathField.getText().trim());
        if (ps.isError()) {
            showValidation("✗ Path: " + ps.message, new Color(220, 60, 60));
            return;
        }
        ConfigValidator.FieldStatus portSt =
                ConfigValidator.validatePort((Integer) portSpinner.getValue());
        if (portSt.isError()) {
            showValidation("✗ Port: " + portSt.message, new Color(220, 60, 60));
            return;
        }
        ConfigValidator.FieldStatus ramSt =
                ConfigValidator.validateRam((Integer) ramSpinner.getValue());
        if (ramSt.isError()) {
            showValidation("✗ RAM: " + ramSt.message, new Color(220, 60, 60));
            return;
        }

        // ── All hard errors cleared – apply fields to config ─────────────────
        String name = serverNameField.getText().trim();
        config.setServerName(name);
        config.setServerPath(serverPathField.getText().trim());
        config.setMinecraftVersion(minecraftVersionCombo.getSelectedItem().toString());
        config.setModLoader((ServerConfig.ModLoader) modLoaderCombo.getSelectedItem());
        config.setMaxRamGb((Integer) ramSpinner.getValue());
        config.setPort((Integer) portSpinner.getValue());
        config.setViewDistance((Integer) viewDistSpinner.getValue());
        config.setGameMode(gameModeCombo.getSelectedItem().toString().toLowerCase());
        config.setDifficulty(difficultyCombo.getSelectedItem().toString().toLowerCase());
        config.setMaxPlayers((Integer) maxPlayersSpinner.getValue());
        config.setMotd(motdField.getText().trim());
        config.setPvpEnabled(pvpCheck.isSelected());
        config.setOnlineModeEnabled(onlineModeCheck.isSelected());
        config.setAllowFlight(allowFlightCheck.isSelected());
        config.setWhitelistEnabled(whitelistCheck.isSelected());
        config.setCommandBlocksEnabled(commandBlocksCheck.isSelected());

        String javaSel = (String) javaCombo.getSelectedItem();
        if ("Custom Path…".equals(javaSel)) {
            config.setCustomJavaPath(customJavaField.getText().trim());
        } else if (detectedJavas != null) {
            for (JavaManager.JavaInstallation java : detectedJavas) {
                if (java.toString().equals(javaSel)) {
                    config.setCustomJavaPath(java.getPath());
                    break;
                }
            }
        }

        config.setJvmArgs(jvmArgsArea.getText().trim());
        config.setAutoRestart(autoRestartCheck.isSelected());
        config.setUpnpEnabled(upnpCheck.isSelected());

        configChanged = true;
        // Show any non-blocking warnings in the label before closing
        StringBuilder warnings = new StringBuilder();
        if (ns.isWarning())    warnings.append("Name: ").append(ns.message).append("  ");
        if (ramSt.isWarning()) warnings.append("RAM: ").append(ramSt.message);
        if (warnings.length() > 0) {
            showValidation("⚠ Saved with warnings: " + warnings.toString().trim(),
                    new Color(230, 150, 0));
            // brief pause so the user sees the warning before the dialog closes
            Timer t = new Timer(1500, ev -> dispose());
            t.setRepeats(false);
            t.start();
            return;
        }
        logger.info("Configuration saved: {}", name);
        dispose();
    }

    private void handleCancel() {
        boolean dirty = !serverNameField.getText().equals(config.getServerName())
                || !serverPathField.getText().equals(config.getServerPath() != null ? config.getServerPath() : "");
        if (dirty) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Discard unsaved changes?", "Unsaved Changes", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;
        }
        dispose();
    }

    private void resetToDefaults() {
        if (JOptionPane.showConfirmDialog(this, "Reset all to defaults?", "Reset",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        serverNameField.setText("ZeroG Server");
        serverPathField.setText("server");
        minecraftVersionCombo.setSelectedItem("1.20.1");
        modLoaderCombo.setSelectedItem(ServerConfig.ModLoader.FORGE);
        ramSpinner.setValue(4);
        portSpinner.setValue(25565);
        viewDistSpinner.setValue(10);
        gameModeCombo.setSelectedItem("Survival");
        difficultyCombo.setSelectedItem("Normal");
        maxPlayersSpinner.setValue(20);
        motdField.setText("A Minecraft Server");
        pvpCheck.setSelected(true);
        onlineModeCheck.setSelected(true);
        allowFlightCheck.setSelected(false);
        whitelistCheck.setSelected(false);
        commandBlocksCheck.setSelected(true);
        javaCombo.setSelectedItem("Auto-detect (Recommended)");
        customJavaField.setText("");
        customJavaField.setEnabled(false);
        browseJavaButton.setEnabled(false);
        setDefaultJvmArgs();
        autoRestartCheck.setSelected(false);
        showValidation("↺ Reset to defaults", Color.BLUE);
        updatePreview();
    }

    private void setDefaultJvmArgs() {
        jvmArgsArea.setText("-XX:+UseG1GC -Dsun.rmi.dgc.server.gcInterval=2147483646 "
                + "-XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 "
                + "-XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M");
    }

    private void detectJavaInstallations() {
        SwingWorker<List<JavaManager.JavaInstallation>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<JavaManager.JavaInstallation> doInBackground() {
                return JavaManager.detectJavaInstallations();
            }
            @Override
            protected void done() {
                try {
                    detectedJavas = get();
                    String current = (String) javaCombo.getSelectedItem();
                    javaCombo.removeAllItems();
                    javaCombo.addItem("Auto-detect (Recommended)");
                    javaCombo.addItem("Use System Java");
                    for (JavaManager.JavaInstallation j : detectedJavas) javaCombo.addItem(j.toString());
                    javaCombo.addItem("Custom Path…");
                    if (current != null) javaCombo.setSelectedItem(current);
                } catch (Exception e) {
                    logger.error("Java detection error", e);
                }
            }
        };
        worker.execute();
    }

    private void handleJavaSelection() {
        boolean custom = "Custom Path…".equals(javaCombo.getSelectedItem());
        customJavaField.setEnabled(custom);
        browseJavaButton.setEnabled(custom);
        if (custom) customJavaField.requestFocus();
    }

    public boolean isConfigurationChanged() { return configChanged; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JPanel tab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(16, 16, 16, 16));
        return p;
    }

    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets  = new Insets(6, 8, 6, 8);
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.anchor  = GridBagConstraints.WEST;
        return g;
    }

    private void addRow(JPanel p, GridBagConstraints g, int row, String label, Component comp) {
        g.gridwidth = 1;
        g.gridx = 0; g.gridy = row; g.weightx = 0.3;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        p.add(lbl, g);
        g.gridx = 1; g.weightx = 0.7;
        p.add(comp, g);
    }

    private JPanel wrapUnit(JComponent comp, String unit) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setOpaque(false);
        row.add(comp);
        row.add(new JLabel(unit));
        return row;
    }

    private JCheckBox checkbox(String text, boolean selected) {
        JCheckBox cb = new JCheckBox(text, selected);
        return cb;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

