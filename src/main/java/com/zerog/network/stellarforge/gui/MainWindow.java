package com.zerog.network.stellarforge.gui;

import com.zerog.network.stellarforge.config.SecureConfig;
import com.zerog.network.stellarforge.model.ServerConfig;
import com.zerog.network.stellarforge.model.ModInfo;
import com.zerog.network.stellarforge.modpack.ModpackImporter;
import com.zerog.network.stellarforge.modpack.ModpackConfig;
import com.zerog.network.stellarforge.modpack.ModpackConfigDialog;
import com.zerog.network.stellarforge.modpack.ModpackManifest;
import com.zerog.network.stellarforge.api.CurseForgeClient;
import com.zerog.network.stellarforge.util.ModLoaderVersionFetcher;
import com.zerog.network.stellarforge.util.ImprovedVersionFetcher;
import com.zerog.network.stellarforge.util.FileUtil;
import com.zerog.network.stellarforge.util.ServerManager;
import com.universalator.gui.ModInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Main application window for Stellar Server Forge
 */
public class MainWindow extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    private static final String CONFIG_FILE = "server-config.json";
    
    private ServerConfig serverConfig;
    private Process serverProcess;
    private ModLoaderVersionFetcher versionFetcher;
    private ImprovedVersionFetcher improvedVersionFetcher;
    
    // GUI Components
    private JTextField serverNameField;
    private JTextField serverPathField;
    private JComboBox<String> minecraftVersionCombo;
    private JComboBox<ServerConfig.ModLoader> modLoaderCombo;
    private JComboBox<String> modLoaderVersionCombo;
    private JSpinner ramSpinner;
    private JSpinner portSpinner;
    private JTextField javaPathField;
    private JTextArea jvmArgsArea;
    private JCheckBox autoRestartCheck;
    private JCheckBox upnpCheck;
    
    private JButton launchButton;
    private JButton stopButton;
    private JButton installModsButton;
    private JButton browsePathButton;
    private JButton browseJavaButton;
    private JButton importButton;
    private JButton aboutButton;
    
    private JTextArea logArea;
    private JScrollPane logScrollPane;
    
    public MainWindow() {
        this.versionFetcher = new ModLoaderVersionFetcher();
        this.improvedVersionFetcher = new ImprovedVersionFetcher();
        initializeConfig();
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        loadConfiguration();
        
        String appName = SecureConfig.getInstance().getAppName();
        String version = SecureConfig.getInstance().getAppVersion();
        String organization = SecureConfig.getInstance().getOrganization();
        
        setTitle(appName + " v" + version + " - " + organization);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);
        
        // Set icon (if available)
        setIconImage(createDefaultIcon());
        
        logger.info("Main window initialized");
    }
    
    private void initializeConfig() {
        serverConfig = FileUtil.loadServerConfig(CONFIG_FILE);
        if (serverConfig == null) {
            serverConfig = new ServerConfig();
        }
    }
    
    private void initializeComponents() {
        // Server configuration components
        serverNameField = new JTextField(20);
        serverPathField = new JTextField(30);
        minecraftVersionCombo = new JComboBox<>(ServerManager.getAvailableMinecraftVersions());
        modLoaderCombo = new JComboBox<>(ServerConfig.ModLoader.values());
        modLoaderVersionCombo = new JComboBox<>();
        ramSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 64, 1));
        portSpinner = new JSpinner(new SpinnerNumberModel(25565, 1024, 65535, 1));
        javaPathField = new JTextField(30);
        jvmArgsArea = new JTextArea(3, 40);
        jvmArgsArea.setLineWrap(true);
        jvmArgsArea.setWrapStyleWord(true);
        autoRestartCheck = new JCheckBox("Auto-restart on crash");
        upnpCheck = new JCheckBox("Enable UPnP port forwarding");
        
        // Buttons
        launchButton = new JButton("Launch Server");
        stopButton = new JButton("Stop Server");
        installModsButton = new JButton("Install Mods");
        browsePathButton = new JButton("Browse...");
        browseJavaButton = new JButton("Browse...");
        importButton = new JButton("Import Modpack");
        aboutButton = new JButton("About");
        
        // Log area
        logArea = new JTextArea(10, 60);
        logArea.setEditable(false);
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.WHITE);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logScrollPane = new JScrollPane(logArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // Initial state
        stopButton.setEnabled(false);
        updateModLoaderVersions();
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        // Header panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Main panel with tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Home Tab
        JPanel homePanel = createHomePanel();
        tabbedPane.addTab("🏠 Home", homePanel);
        
        // Server Configuration Tab
        JPanel configPanel = createConfigurationPanel();
        tabbedPane.addTab("⚙️ Server Configuration", configPanel);
        
        // Mod Management Tab
        JPanel modPanel = createModManagementPanel();
        tabbedPane.addTab("📦 Mod Management", modPanel);
        
        // Server Control Tab
        JPanel controlPanel = createServerControlPanel();
        tabbedPane.addTab("🚀 Server Control", controlPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Status bar
        JPanel statusPanel = createFooterPanel();
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("🚀 " + SecureConfig.getInstance().getAppName());
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel subtitleLabel = new JLabel("Space-themed Minecraft Server Creator - " + SecureConfig.getInstance().getOrganization());
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 14));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(subtitleLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createConfigurationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Server Name
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Server Name:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        panel.add(serverNameField, gbc);
        
        // Server Path
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Server Path:"), gbc);
        gbc.gridx = 1;
        panel.add(serverPathField, gbc);
        gbc.gridx = 2;
        panel.add(browsePathButton, gbc);
        
        // Minecraft Version
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Minecraft Version:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        panel.add(minecraftVersionCombo, gbc);
        
        // Mod Loader
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        panel.add(new JLabel("Mod Loader:"), gbc);
        gbc.gridx = 1;
        panel.add(modLoaderCombo, gbc);
        gbc.gridx = 2;
        panel.add(modLoaderVersionCombo, gbc);
        
        // RAM
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Max RAM (GB):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        panel.add(ramSpinner, gbc);
        
        // Port
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1;
        panel.add(new JLabel("Server Port:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        panel.add(portSpinner, gbc);
        
        // Java Path
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 1;
        panel.add(new JLabel("Java Path:"), gbc);
        gbc.gridx = 1;
        panel.add(javaPathField, gbc);
        gbc.gridx = 2;
        panel.add(browseJavaButton, gbc);
        
        // JVM Args
        gbc.gridx = 0; gbc.gridy = 7;
        panel.add(new JLabel("JVM Arguments:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(jvmArgsArea), gbc);
        
        // Checkboxes
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.NONE;
        panel.add(autoRestartCheck, gbc);
        gbc.gridy = 9;
        panel.add(upnpCheck, gbc);
        
        // Buttons
        gbc.gridy = 10; gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("Save Configuration"));
        buttonPanel.add(new JButton("Load Configuration"));
        buttonPanel.add(new JButton("Reset to Defaults"));
        panel.add(buttonPanel, gbc);
        
        return panel;
    }
    
    private JPanel createModManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Mod installation button
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(installModsButton);
        topPanel.add(new JButton("Scan Local Mods"));
        topPanel.add(new JButton("Remove Selected Mods"));
        panel.add(topPanel, BorderLayout.NORTH);
        
        // Mod list
        JList<String> modList = new JList<>();
        modList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        panel.add(new JScrollPane(modList), BorderLayout.CENTER);
        
        // Mod details
        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBorder(new TitledBorder("Mod Details"));
        detailPanel.add(new JLabel("Select a mod to view details"));
        panel.add(detailPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createServerControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(launchButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(new JButton("Restart Server"));
        buttonPanel.add(importButton);
        buttonPanel.add(aboutButton);
        panel.add(buttonPanel, BorderLayout.NORTH);
        
        // Server log
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new TitledBorder("Server Log"));
        logPanel.add(logScrollPane, BorderLayout.CENTER);
        
        // Log controls
        JPanel logControls = new JPanel(new FlowLayout());
        logControls.add(new JButton("Clear Log"));
        logControls.add(new JButton("Save Log"));
        logControls.add(new JButton("Auto-scroll"));
        logPanel.add(logControls, BorderLayout.SOUTH);
        
        panel.add(logPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JLabel statusLabel = new JLabel("Ready - " + SecureConfig.getInstance().getAppName() + " v" + SecureConfig.getInstance().getAppVersion());
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        panel.add(statusLabel);
        
        return panel;
    }
    
    private void showConfigurationDialog() {
        String message = "<html><div style='width: 400px;'>" +
            "<h3>🛠️ Server Configuration</h3>" +
            "<p><strong>Current Settings:</strong></p>" +
            "<ul>" +
            "<li>Server Name: " + serverConfig.getServerName() + "</li>" +
            "<li>Minecraft Version: " + serverConfig.getMinecraftVersion() + "</li>" +
            "<li>Mod Loader: " + serverConfig.getModLoader().getDisplayName() + "</li>" +
            "<li>RAM: " + serverConfig.getMaxRamGb() + " GB</li>" +
            "<li>Port: " + serverConfig.getPort() + "</li>" +
            "</ul>" +
            "<p><em>Full configuration interface coming soon...</em></p>" +
            "</div></html>";
        
        JOptionPane.showMessageDialog(this, message, "Server Configuration", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showAboutDialog() {
        String message = "<html><div style='width: 400px;'>" +
            "<h3>🚀 " + SecureConfig.getInstance().getAppName() + "</h3>" +
            "<p><strong>Version:</strong> " + SecureConfig.getInstance().getAppVersion() + "</p>" +
            "<p><strong>Organization:</strong> " + SecureConfig.getInstance().getOrganization() + "</p>" +
            "<br>" +
            "<p><strong>Features:</strong></p>" +
            "<ul>" +
            "<li>🔐 Secure API key management</li>" +
            "<li>🌐 CurseForge & Modrinth integration</li>" +
            "<li>⚙️ Multi-modloader support</li>" +
            "<li>🎮 Easy server creation</li>" +
            "</ul>" +
            "<br>" +
            "<p><em>A space-themed server creation tool for the Minecraft community</em></p>" +
            "</div></html>";
        
        JOptionPane.showMessageDialog(this, message, "About", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Import modpack from ZIP file
     */
    private void importModpackFromZip() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Modpack ZIP File");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".zip");
            }
            
            @Override
            public String getDescription() {
                return "ZIP Files (*.zip)";
            }
        });
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // Show progress dialog
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            progressBar.setString("Importing modpack...");
            progressBar.setStringPainted(true);
            
            JDialog progressDialog = new JDialog(this, "Importing Modpack", true);
            progressDialog.add(progressBar);
            progressDialog.setSize(300, 80);
            progressDialog.setLocationRelativeTo(this);
            
            // Import in background thread
            SwingWorker<com.zerog.network.stellarforge.modpack.ModpackImporter.ModpackImportResult, Void> worker = 
                new SwingWorker<com.zerog.network.stellarforge.modpack.ModpackImporter.ModpackImportResult, Void>() {
                
                @Override
                protected com.zerog.network.stellarforge.modpack.ModpackImporter.ModpackImportResult doInBackground() throws Exception {
                    com.zerog.network.stellarforge.modpack.ModpackImporter importer = 
                        new com.zerog.network.stellarforge.modpack.ModpackImporter();
                    return importer.importModpack(selectedFile.getAbsolutePath(), MainWindow.this);
                }
                
                @Override
                protected void done() {
                    progressDialog.dispose();
                    
                    try {
                        com.zerog.network.stellarforge.modpack.ModpackImporter.ModpackImportResult result = get();
                        
                        if (result != null) {
                            handleModpackImportResult(result);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(MainWindow.this, 
                            "Error importing modpack: " + e.getMessage(), 
                            "Import Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            
            worker.execute();
            progressDialog.setVisible(true);
        }
    }
    
    /**
     * Handle modpack import result
     */
    private void handleModpackImportResult(com.zerog.network.stellarforge.modpack.ModpackImporter.ModpackImportResult result) {
        com.zerog.network.stellarforge.modpack.ModpackConfig config = result.getConfig();
        List<ModInfo> mods = result.getMods();
        
        // Show import summary
        StringBuilder summary = new StringBuilder();
        summary.append("<html><h3>Modpack Import Summary</h3>");
        summary.append("<p><b>Server Name:</b> ").append(config.getServerName()).append("</p>");
        summary.append("<p><b>Minecraft Version:</b> ").append(config.getMinecraftVersion()).append("</p>");
        summary.append("<p><b>Mod Loader:</b> ").append(config.getModLoader()).append(" ").append(config.getModLoaderVersion()).append("</p>");
        summary.append("<p><b>Server Path:</b> ").append(config.getServerPath()).append("</p>");
        summary.append("<p><b>Server-Compatible Mods Found:</b> ").append(mods.size()).append("</p>");
        
        if (result.hasManifest()) {
            summary.append("<p><b>Source:</b> CurseForge Manifest</p>");
        } else {
            summary.append("<p><b>Source:</b> HTML Mod List</p>");
        }
        
        summary.append("<p>Would you like to proceed with installing these mods?</p></html>");
        
        int choice = JOptionPane.showConfirmDialog(this, summary.toString(), 
            "Modpack Import Complete", JOptionPane.YES_NO_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            // Install mods
            installModpackMods(config, mods);
        }
    }
    
    /**
     * Install modpack mods
     */
    private void installModpackMods(com.zerog.network.stellarforge.modpack.ModpackConfig config, List<ModInfo> mods) {
        // Create server directory
        File serverDir = new File(config.getServerPath());
        if (!serverDir.exists()) {
            serverDir.mkdirs();
        }
        
        // Create mods directory
        File modsDir = new File(serverDir, "mods");
        if (!modsDir.exists()) {
            modsDir.mkdirs();
        }
        
        // Show progress dialog
        JProgressBar progressBar = new JProgressBar(0, mods.size());
        progressBar.setString("Installing mods...");
        progressBar.setStringPainted(true);
        
        JDialog progressDialog = new JDialog(this, "Installing Mods", true);
        progressDialog.add(progressBar);
        progressDialog.setSize(400, 80);
        progressDialog.setLocationRelativeTo(this);
        
        // Install mods in background thread
        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (int i = 0; i < mods.size(); i++) {
                    ModInfo mod = mods.get(i);
                    
                    try {
                        // Download mod file
                        if (mod.getUrl() != null && !mod.getUrl().isEmpty()) {
                            downloadModFile(mod, modsDir);
                        }
                        
                        publish(i + 1);
                        Thread.sleep(100); // Small delay to show progress
                        
                    } catch (Exception e) {
                        System.err.println("Error installing mod " + mod.getName() + ": " + e.getMessage());
                    }
                }
                return null;
            }
            
            @Override
            protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    int progress = chunks.get(chunks.size() - 1);
                    progressBar.setValue(progress);
                    progressBar.setString("Installing mods... (" + progress + "/" + mods.size() + ")");
                }
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
                
                JOptionPane.showMessageDialog(MainWindow.this, 
                    "Modpack installation complete!\n" + 
                    "Server: " + config.getServerName() + "\n" + 
                    "Location: " + config.getServerPath() + "\n" + 
                    "Mods installed: " + mods.size(), 
                    "Installation Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    /**
     * Download mod file to mods directory
     */
    private void downloadModFile(ModInfo mod, File modsDir) throws Exception {
        java.net.URL url = new java.net.URL(mod.getUrl());
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        
        try (java.io.InputStream in = connection.getInputStream();
             java.io.FileOutputStream out = new java.io.FileOutputStream(new File(modsDir, mod.getFileName()))) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
    
    private void setupEventHandlers() {
        // Launch button
        launchButton.addActionListener(e -> launchServer());
        
        // Stop button
        stopButton.addActionListener(e -> stopServer());
        
        // Install mods button
        installModsButton.addActionListener(e -> openModInstaller());
        
        // Browse path button
        browsePathButton.addActionListener(e -> browseServerPath());
        
        // Browse Java button
        browseJavaButton.addActionListener(e -> browseJavaPath());
        
        // Import button
        importButton.addActionListener(e -> importModpackFromZip());
        
        // About button
        aboutButton.addActionListener(e -> showAboutDialog());
        
        // Configuration change listeners
        serverNameField.addActionListener(e -> updateConfiguration());
        serverPathField.addActionListener(e -> updateConfiguration());
        minecraftVersionCombo.addActionListener(e -> {
            updateConfiguration();
            updateModLoaderVersions();
        });
        modLoaderCombo.addActionListener(e -> {
            loadVersionsAsync(); // Load versions when mod loader changes
            updateConfiguration();
            updateModLoaderVersions();
        });
        modLoaderVersionCombo.addActionListener(e -> updateConfiguration());
        ramSpinner.addChangeListener(e -> updateConfiguration());
        portSpinner.addChangeListener(e -> updateConfiguration());
        javaPathField.addActionListener(e -> updateConfiguration());
        autoRestartCheck.addActionListener(e -> updateConfiguration());
        upnpCheck.addActionListener(e -> updateConfiguration());
    }
    
    private void loadConfiguration() {
        serverNameField.setText(serverConfig.getServerName());
        serverPathField.setText(serverConfig.getServerPath() != null ? serverConfig.getServerPath() : "");
        minecraftVersionCombo.setSelectedItem(serverConfig.getMinecraftVersion());
        modLoaderCombo.setSelectedItem(serverConfig.getModLoader());
        ramSpinner.setValue(serverConfig.getMaxRamGb());
        portSpinner.setValue(serverConfig.getPort());
        javaPathField.setText(serverConfig.getCustomJavaPath() != null ? serverConfig.getCustomJavaPath() : "");
        jvmArgsArea.setText(serverConfig.getJvmArgs());
        autoRestartCheck.setSelected(serverConfig.isAutoRestart());
        upnpCheck.setSelected(serverConfig.isUpnpEnabled());
        
        // Load mod loader versions and set the current one
        updateModLoaderVersions();
    }
    
    private void updateModLoaderVersions() {
        ServerConfig.ModLoader selectedLoader = (ServerConfig.ModLoader) modLoaderCombo.getSelectedItem();
        String selectedVersion = (String) minecraftVersionCombo.getSelectedItem();
        
        if (selectedLoader == null || selectedVersion == null) return;
        
        // Save current selection
        String currentVersion = serverConfig.getModLoaderVersion();
        
        modLoaderVersionCombo.removeAllItems();
        modLoaderVersionCombo.addItem("Loading...");
        modLoaderVersionCombo.setEnabled(false);
        
        // Fetch versions asynchronously to avoid blocking the UI
        SwingWorker<java.util.List<String>, Void> worker = new SwingWorker<java.util.List<String>, Void>() {
            @Override
            protected java.util.List<String> doInBackground() throws Exception {
                return versionFetcher.getModLoaderVersions(selectedLoader.name().toLowerCase(), selectedVersion);
            }
            
            @Override
            protected void done() {
                try {
                    java.util.List<String> versions = get();
                    
                    modLoaderVersionCombo.removeAllItems();
                    for (String version : versions) {
                        modLoaderVersionCombo.addItem(version);
                    }
                    
                    // Try to restore previous selection
                    if (currentVersion != null && !currentVersion.isEmpty()) {
                        modLoaderVersionCombo.setSelectedItem(currentVersion);
                    }
                    
                    modLoaderVersionCombo.setEnabled(true);
                    
                } catch (Exception e) {
                    logger.error("Error fetching mod loader versions", e);
                    modLoaderVersionCombo.removeAllItems();
                    modLoaderVersionCombo.addItem("Latest");
                    modLoaderVersionCombo.setEnabled(true);
                }
            }
        };
        
        worker.execute();
    }
    
    private void loadVersionsAsync() {
        if (modLoaderVersionCombo == null) return;
        
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                ServerConfig.ModLoader selectedLoader = (ServerConfig.ModLoader) modLoaderCombo.getSelectedItem();
                if (selectedLoader == null) {
                    selectedLoader = ServerConfig.ModLoader.NEOFORGE; // Default
                }
                
                return improvedVersionFetcher.getAllVersions(selectedLoader.name().toLowerCase());
            }
            
            @Override
            protected void done() {
                try {
                    List<String> versions = get();
                    modLoaderVersionCombo.removeAllItems();
                    
                    for (String version : versions) {
                        modLoaderVersionCombo.addItem(version);
                    }
                    
                    if (!versions.isEmpty()) {
                        modLoaderVersionCombo.setSelectedIndex(0); // Select latest
                    }
                } catch (Exception e) {
                    logger.error("Error loading versions: {}", e.getMessage());
                    modLoaderVersionCombo.removeAllItems();
                    modLoaderVersionCombo.addItem("Error loading versions");
                }
            }
        };
        
        worker.execute();
    }
    
    private void updateConfiguration() {
        serverConfig.setServerName(serverNameField.getText());
        serverConfig.setServerPath(serverPathField.getText());
        serverConfig.setMinecraftVersion((String) minecraftVersionCombo.getSelectedItem());
        serverConfig.setModLoader((ServerConfig.ModLoader) modLoaderCombo.getSelectedItem());
        serverConfig.setModLoaderVersion((String) modLoaderVersionCombo.getSelectedItem());
        serverConfig.setMaxRamGb((Integer) ramSpinner.getValue());
        serverConfig.setPort((Integer) portSpinner.getValue());
        serverConfig.setCustomJavaPath(javaPathField.getText());
        serverConfig.setJvmArgs(jvmArgsArea.getText());
        serverConfig.setAutoRestart(autoRestartCheck.isSelected());
        serverConfig.setUpnpEnabled(upnpCheck.isSelected());
        
        // Save configuration
        FileUtil.saveServerConfig(serverConfig, CONFIG_FILE);
    }
    
    private void launchServer() {
        if (!validateConfiguration()) {
            return;
        }
        
        try {
            // Create server directory if it doesn't exist
            File serverDir = new File(serverConfig.getServerPath());
            if (!serverDir.exists() && !serverDir.mkdirs()) {
                showError("Failed to create server directory");
                return;
            }
            
            // Download server if needed
            if (!ServerManager.isServerDownloaded(serverConfig)) {
                logArea.append("Downloading server files...\n");
                if (!ServerManager.downloadServer(serverConfig)) {
                    showError("Failed to download server files");
                    return;
                }
            }
            
            // Launch server
            logArea.append("Starting server...\n");
            serverProcess = ServerManager.launchServer(serverConfig);
            
            if (serverProcess != null) {
                launchButton.setEnabled(false);
                stopButton.setEnabled(true);
                
                // Start log monitoring
                startLogMonitoring();
                
                logArea.append("Server started successfully!\n");
            } else {
                showError("Failed to start server");
            }
            
        } catch (Exception e) {
            logger.error("Error launching server", e);
            showError("Error launching server: " + e.getMessage());
        }
    }
    
    private void stopServer() {
        if (serverProcess != null) {
            logArea.append("Stopping server...\n");
            serverProcess.destroy();
            serverProcess = null;
            
            launchButton.setEnabled(true);
            stopButton.setEnabled(false);
            
            logArea.append("Server stopped.\n");
        }
    }
    
    private void openModInstaller() {
        // Convert ServerConfig to universalator format
        com.universalator.model.ServerConfig universalatorConfig = new com.universalator.model.ServerConfig();
        universalatorConfig.setServerName(serverConfig.getServerName());
        universalatorConfig.setServerPath(serverConfig.getServerPath());
        universalatorConfig.setMinecraftVersion(serverConfig.getMinecraftVersion());
        universalatorConfig.setModLoader(com.universalator.model.ServerConfig.ModLoader.valueOf(serverConfig.getModLoader().name()));
        universalatorConfig.setModLoaderVersion(serverConfig.getModLoaderVersion());
        universalatorConfig.setMaxRamGb(serverConfig.getMaxRamGb());
        universalatorConfig.setPort(serverConfig.getPort());
        universalatorConfig.setCustomJavaPath(serverConfig.getCustomJavaPath());
        universalatorConfig.setJvmArgs(serverConfig.getJvmArgs());
        universalatorConfig.setAutoRestart(serverConfig.isAutoRestart());
        universalatorConfig.setUpnpEnabled(serverConfig.isUpnpEnabled());
        
        // Create and show mod installer dialog
        ModInstaller modInstaller = new ModInstaller(this, universalatorConfig);
        modInstaller.setVisible(true);
    }
    
    private void browseServerPath() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Server Directory");
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            serverPathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            updateConfiguration();
        }
    }
    
    private void browseJavaPath() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Select Java Executable");
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            javaPathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            updateConfiguration();
        }
    }
    
    private boolean validateConfiguration() {
        if (serverConfig.getServerPath() == null || serverConfig.getServerPath().trim().isEmpty()) {
            showError("Please select a server directory");
            return false;
        }
        
        if (!ServerManager.validateServerConfig(serverConfig)) {
            showError("Invalid server configuration");
            return false;
        }
        
        return true;
    }
    
    private void startLogMonitoring() {
        // This would monitor the server log file and update the log area
        // For now, just show a placeholder
        SwingUtilities.invokeLater(() -> {
            logArea.append("Server log monitoring started...\n");
        });
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private Image createDefaultIcon() {
        // Create a simple default icon
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, 32, 32);
        g2d.setColor(Color.WHITE);
        g2d.drawString("S", 12, 20);
        g2d.dispose();
        return icon;
    }
    
    private JPanel createHomePanel() {
        JPanel homePanel = new JPanel(new BorderLayout());
        homePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Welcome header
        JPanel welcomePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        welcomePanel.setBackground(new Color(240, 248, 255));
        welcomePanel.setBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2));
        
        JLabel welcomeLabel = new JLabel("🌟 Welcome to Stellar Server Forge 🌟");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        welcomeLabel.setForeground(new Color(25, 25, 112));
        welcomePanel.add(welcomeLabel);
        
        homePanel.add(welcomePanel, BorderLayout.NORTH);
        
        // Main content area
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        
        // Quick Stats Panel
        JPanel statsPanel = createQuickStatsPanel();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.5; gbc.weighty = 0.3;
        contentPanel.add(statsPanel, gbc);
        
        // Recent Activity Panel
        JPanel activityPanel = createRecentActivityPanel();
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.5; gbc.weighty = 0.3;
        contentPanel.add(activityPanel, gbc);
        
        // Quick Actions Panel
        JPanel actionsPanel = createQuickActionsPanel();
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0.7;
        contentPanel.add(actionsPanel, gbc);
        
        homePanel.add(contentPanel, BorderLayout.CENTER);
        
        return homePanel;
    }
    
    private JPanel createQuickStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("📊 Quick Stats"));
        panel.setBackground(Color.WHITE);
        
        JPanel statsContent = new JPanel(new GridLayout(4, 1, 5, 5));
        statsContent.setBackground(Color.WHITE);
        
        // Server Status
        JLabel serverStatusLabel = new JLabel("Server Status: Offline");
        serverStatusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        serverStatusLabel.setForeground(Color.RED);
        statsContent.add(serverStatusLabel);
        
        // Mod Count
        JLabel modCountLabel = new JLabel("Installed Mods: 0");
        modCountLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statsContent.add(modCountLabel);
        
        // Last Launch
        JLabel lastLaunchLabel = new JLabel("Last Launch: Never");
        lastLaunchLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statsContent.add(lastLaunchLabel);
        
        // Version Info
        JLabel versionLabel = new JLabel("Version: Loading...");
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statsContent.add(versionLabel);
        
        panel.add(statsContent, BorderLayout.CENTER);
        
        // Update version info asynchronously
        SwingWorker<String, Void> versionWorker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return improvedVersionFetcher.getLatestVersion("neoforge");
            }
            
            @Override
            protected void done() {
                try {
                    String version = get();
                    versionLabel.setText("Latest NeoForge: " + version);
                } catch (Exception e) {
                    versionLabel.setText("Latest NeoForge: Error loading");
                }
            }
        };
        versionWorker.execute();
        
        return panel;
    }
    
    private JPanel createRecentActivityPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("📋 Recent Activity"));
        panel.setBackground(Color.WHITE);
        
        JTextArea activityArea = new JTextArea();
        activityArea.setEditable(false);
        activityArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        activityArea.setText("Welcome to Stellar Server Forge!\n\n" +
                           "• Application started successfully\n" +
                           "• Ready to create your server\n" +
                           "• Check the configuration tab to begin\n");
        
        JScrollPane scrollPane = new JScrollPane(activityArea);
        scrollPane.setPreferredSize(new Dimension(200, 100));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createQuickActionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("⚡ Quick Actions"));
        panel.setBackground(Color.WHITE);
        
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        buttonsPanel.setBackground(Color.WHITE);
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Quick action buttons
        JButton newServerBtn = new JButton("🆕 New Server");
        newServerBtn.setToolTipText("Create a new server configuration");
        newServerBtn.addActionListener(e -> {
            // Switch to configuration tab
            Container parent = newServerBtn.getParent();
            while (parent != null && !(parent instanceof JTabbedPane)) {
                parent = parent.getParent();
            }
            if (parent instanceof JTabbedPane) {
                ((JTabbedPane) parent).setSelectedIndex(1);
            }
        });
        
        JButton loadConfigBtn = new JButton("📁 Load Config");
        loadConfigBtn.setToolTipText("Load an existing server configuration");
        loadConfigBtn.addActionListener(e -> loadConfiguration());
        
        JButton downloadModsBtn = new JButton("📥 Download Mods");
        downloadModsBtn.setToolTipText("Browse and download mods");
        downloadModsBtn.addActionListener(e -> {
            // Switch to mod management tab
            Container parent = downloadModsBtn.getParent();
            while (parent != null && !(parent instanceof JTabbedPane)) {
                parent = parent.getParent();
            }
            if (parent instanceof JTabbedPane) {
                ((JTabbedPane) parent).setSelectedIndex(2);
            }
        });
        
        JButton startServerBtn = new JButton("🚀 Start Server");
        startServerBtn.setToolTipText("Start the configured server");
        startServerBtn.addActionListener(e -> startServer());
        
        JButton refreshBtn = new JButton("🔄 Refresh Versions");
        refreshBtn.setToolTipText("Refresh mod loader versions");
        refreshBtn.addActionListener(e -> refreshVersions());
        
        JButton helpBtn = new JButton("❓ Help");
        helpBtn.setToolTipText("View help and documentation");
        helpBtn.addActionListener(e -> showHelp());
        
        buttonsPanel.add(newServerBtn);
        buttonsPanel.add(loadConfigBtn);
        buttonsPanel.add(downloadModsBtn);
        buttonsPanel.add(startServerBtn);
        buttonsPanel.add(refreshBtn);
        buttonsPanel.add(helpBtn);
        
        panel.add(buttonsPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void refreshVersions() {
        // Clear existing version data
        if (modLoaderVersionCombo != null) {
            modLoaderVersionCombo.removeAllItems();
            modLoaderVersionCombo.addItem("Loading...");
        }
        
        SwingWorker<Void, Void> refreshWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Force refresh of version cache
                versionFetcher = new ModLoaderVersionFetcher(); // Recreate to clear cache
                improvedVersionFetcher = new ImprovedVersionFetcher(); // Recreate improved fetcher
                return null;
            }
            
            @Override
            protected void done() {
                loadVersionsAsync();
                JOptionPane.showMessageDialog(MainWindow.this, 
                    "Versions refreshed successfully!", 
                    "Refresh Complete", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        };
        refreshWorker.execute();
    }
    
    private void showHelp() {
        String helpText = "🌟 Stellar Server Forge Help 🌟\n\n" +
            "Getting Started:\n" +
            "1. Go to 'Server Configuration' tab\n" +
            "2. Select your Minecraft version\n" +
            "3. Choose a mod loader (Forge, Fabric, NeoForge, etc.)\n" +
            "4. Configure server settings\n" +
            "5. Use 'Mod Management' to add mods\n" +
            "6. Start your server from 'Server Control'\n\n" +
            "Features:\n" +
            "• Auto-detection of latest mod loader versions\n" +
            "• Mod installation and management\n" +
            "• Server configuration templates\n" +
            "• Real-time server monitoring\n\n" +
            "Support:\n" +
            "• Check logs in the application folder\n" +
            "• Ensure Java 11+ is installed\n" +
            "• Verify internet connection for downloads\n";
        
        JTextArea helpArea = new JTextArea(helpText);
        helpArea.setEditable(false);
        helpArea.setFont(new Font("Arial", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(helpArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Help", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void startServer() {
        // Quick start server implementation
        try {
            ServerConfig config = getCurrentServerConfig();
            if (config != null) {
                if (!ServerManager.isServerDownloaded(config)) {
                    if (!ServerManager.downloadServer(config)) {
                        JOptionPane.showMessageDialog(this, 
                            "Failed to download server!", 
                            "Download Error", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                
                Process serverProcess = ServerManager.launchServer(config);
                if (serverProcess != null) {
                    JOptionPane.showMessageDialog(this, 
                        "Server started successfully!", 
                        "Server Started", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Please configure your server first!", 
                    "Configuration Required", 
                    JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) {
            logger.error("Error starting server: {}", e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Error starting server: " + e.getMessage(), 
                "Server Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
