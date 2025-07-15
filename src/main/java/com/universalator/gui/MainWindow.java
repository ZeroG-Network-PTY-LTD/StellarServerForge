package com.universalator.gui;

import com.universalator.model.ServerConfig;
import com.universalator.utils.FileUtil;
import com.universalator.utils.ServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Main application window
 */
public class MainWindow extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    private static final String CONFIG_FILE = "server-config.json";
    
    private ServerConfig serverConfig;
    private Process serverProcess;
    
    // GUI Components
    private JTextField serverNameField;
    private JTextField serverPathField;
    private JComboBox<String> minecraftVersionCombo;
    private JComboBox<ServerConfig.ModLoader> modLoaderCombo;
    private JTextField modLoaderVersionField;
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
    
    private JTextArea logArea;
    private JScrollPane logScrollPane;
    
    public MainWindow() {
        initializeConfig();
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        loadConfiguration();
        
        setTitle("Universalator GUI - Minecraft Server Creator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        
        // Set icon (if available)
        setIconImage(createDefaultIcon());
    }
    
    private void initializeConfig() {
        serverConfig = FileUtil.loadServerConfig(CONFIG_FILE);
    }
    
    private void initializeComponents() {
        // Server configuration components
        serverNameField = new JTextField(20);
        serverPathField = new JTextField(30);
        minecraftVersionCombo = new JComboBox<>(ServerManager.getAvailableMinecraftVersions());
        modLoaderCombo = new JComboBox<>(ServerConfig.ModLoader.values());
        modLoaderVersionField = new JTextField(15);
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
        
        // Log area
        logArea = new JTextArea(10, 60);
        logArea.setEditable(false);
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.WHITE);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logScrollPane = new JScrollPane(logArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // Initial button states
        stopButton.setEnabled(false);
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        // Main panel with tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Server Configuration Tab
        JPanel configPanel = createConfigurationPanel();
        tabbedPane.addTab("Server Configuration", configPanel);
        
        // Mod Management Tab
        JPanel modPanel = createModManagementPanel();
        tabbedPane.addTab("Mod Management", modPanel);
        
        // Server Control Tab
        JPanel controlPanel = createServerControlPanel();
        tabbedPane.addTab("Server Control", controlPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Status bar
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Ready"));
        add(statusPanel, BorderLayout.SOUTH);
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
        panel.add(modLoaderVersionField, gbc);
        
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
        buttonPanel.add(new JButton("View Server Files"));
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
        
        // Configuration change listeners
        serverNameField.addActionListener(e -> updateConfiguration());
        serverPathField.addActionListener(e -> updateConfiguration());
        minecraftVersionCombo.addActionListener(e -> updateConfiguration());
        modLoaderCombo.addActionListener(e -> updateConfiguration());
        modLoaderVersionField.addActionListener(e -> updateConfiguration());
        ramSpinner.addChangeListener(e -> updateConfiguration());
        portSpinner.addChangeListener(e -> updateConfiguration());
        javaPathField.addActionListener(e -> updateConfiguration());
        autoRestartCheck.addActionListener(e -> updateConfiguration());
        upnpCheck.addActionListener(e -> updateConfiguration());
    }
    
    private void launchServer() {
        if (!validateConfiguration()) {
            return;
        }
        
        try {
            // Create server directories
            FileUtil.createServerDirectories(serverConfig.getServerPath());
            
            // Create server files
            FileUtil.createServerProperties(serverConfig.getServerPath(), serverConfig);
            FileUtil.createEulaFile(serverConfig.getServerPath());
            FileUtil.createStartScript(serverConfig.getServerPath(), serverConfig);
            
            // Install server jar if needed
            ServerManager.installServerJar(serverConfig, serverConfig.getServerPath())
                .thenAccept(success -> {
                    if (success) {
                        SwingUtilities.invokeLater(() -> {
                            serverProcess = ServerManager.startServer(serverConfig.getServerPath(), serverConfig);
                            if (serverProcess != null) {
                                launchButton.setEnabled(false);
                                stopButton.setEnabled(true);
                                logArea.append("Server launched successfully!\\n");
                                
                                // Start log monitoring
                                startLogMonitoring();
                            } else {
                                JOptionPane.showMessageDialog(this, "Failed to start server", "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "Failed to install server jar", "Error", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                });
                
        } catch (Exception e) {
            logger.error("Error launching server", e);
            JOptionPane.showMessageDialog(this, "Error launching server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void stopServer() {
        if (serverProcess != null) {
            ServerManager.stopServer(serverProcess);
            serverProcess = null;
            launchButton.setEnabled(true);
            stopButton.setEnabled(false);
            logArea.append("Server stopped!\\n");
        }
    }
    
    private void openModInstaller() {
        ModInstaller installer = new ModInstaller(this, serverConfig);
        installer.setVisible(true);
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
    
    private void updateConfiguration() {
        serverConfig.setServerName(serverNameField.getText());
        serverConfig.setServerPath(serverPathField.getText());
        serverConfig.setMinecraftVersion((String) minecraftVersionCombo.getSelectedItem());
        serverConfig.setModLoader((ServerConfig.ModLoader) modLoaderCombo.getSelectedItem());
        serverConfig.setModLoaderVersion(modLoaderVersionField.getText());
        serverConfig.setMaxRamGb((Integer) ramSpinner.getValue());
        serverConfig.setPort((Integer) portSpinner.getValue());
        serverConfig.setCustomJavaPath(javaPathField.getText());
        serverConfig.setJvmArgs(jvmArgsArea.getText());
        serverConfig.setAutoRestart(autoRestartCheck.isSelected());
        serverConfig.setUpnpEnabled(upnpCheck.isSelected());
        
        // Save configuration
        FileUtil.saveServerConfig(serverConfig, CONFIG_FILE);
    }
    
    private void loadConfiguration() {
        serverNameField.setText(serverConfig.getServerName());
        serverPathField.setText(serverConfig.getServerPath() != null ? serverConfig.getServerPath() : "");
        minecraftVersionCombo.setSelectedItem(serverConfig.getMinecraftVersion());
        modLoaderCombo.setSelectedItem(serverConfig.getModLoader());
        modLoaderVersionField.setText(serverConfig.getModLoaderVersion());
        ramSpinner.setValue(serverConfig.getMaxRamGb());
        portSpinner.setValue(serverConfig.getPort());
        javaPathField.setText(serverConfig.getCustomJavaPath() != null ? serverConfig.getCustomJavaPath() : "");
        jvmArgsArea.setText(serverConfig.getJvmArgs());
        autoRestartCheck.setSelected(serverConfig.isAutoRestart());
        upnpCheck.setSelected(serverConfig.isUpnpEnabled());
    }
    
    private boolean validateConfiguration() {
        if (serverConfig.getServerPath() == null || serverConfig.getServerPath().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a server directory", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (!ServerManager.validateServerConfig(serverConfig)) {
            JOptionPane.showMessageDialog(this, "Invalid server configuration", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    private void startLogMonitoring() {
        // This would monitor the server log file and update the log area
        // For now, just show a placeholder
        SwingUtilities.invokeLater(() -> {
            logArea.append("Server log monitoring started...\\n");
        });
    }
    
    private Image createDefaultIcon() {
        // Create a simple default icon
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, 32, 32);
        g2d.setColor(Color.WHITE);
        g2d.drawString("U", 12, 20);
        g2d.dispose();
        return icon;
    }
}
