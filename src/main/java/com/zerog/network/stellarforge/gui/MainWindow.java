package com.zerog.network.stellarforge.gui;

import com.zerog.network.stellarforge.config.SecureConfig;
import com.zerog.network.stellarforge.model.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

/**
 * Main application window for Stellar Server Forge
 */
public class MainWindow extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    
    private ServerConfig serverConfig;
    
    public MainWindow() {
        initializeConfig();
        initializeComponents();
        
        String appName = SecureConfig.getInstance().getAppName();
        String version = SecureConfig.getInstance().getAppVersion();
        String organization = SecureConfig.getInstance().getOrganization();
        
        setTitle(appName + " v" + version + " - " + organization);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        logger.info("Main window initialized");
    }
    
    private void initializeConfig() {
        serverConfig = new ServerConfig();
    }
    
    private void initializeComponents() {
        setLayout(new BorderLayout());
        
        // Header panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Main content panel
        JPanel mainPanel = createMainPanel();
        add(mainPanel, BorderLayout.CENTER);
        
        // Footer panel
        JPanel footerPanel = createFooterPanel();
        add(footerPanel, BorderLayout.SOUTH);
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
    
    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Welcome message
        JLabel welcomeLabel = new JLabel("<html><div style='text-align: center;'>" +
            "<h2>Welcome to Stellar Server Forge!</h2>" +
            "<p>Your space-themed Minecraft server creation tool</p>" +
            "<br>" +
            "<p>🔐 <strong>Security Status:</strong></p>" +
            "<p>CurseForge API: " + (SecureConfig.getInstance().isCurseForgeEnabled() ? "✅ Configured" : "❌ Not Configured") + "</p>" +
            "<p>Modrinth API: " + (SecureConfig.getInstance().isModrinthEnabled() ? "✅ Enabled" : "⚠️ Disabled") + "</p>" +
            "</div></html>");
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(welcomeLabel, gbc);
        
        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton configureButton = new JButton("Configure Server");
        configureButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showConfigurationDialog();
            }
        });
        
        JButton importButton = new JButton("Import Modpack");
        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importModpackFromZip();
            }
        });
        
        JButton aboutButton = new JButton("About");
        aboutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAboutDialog();
            }
        });
        
        buttonPanel.add(configureButton);
        buttonPanel.add(importButton);
        buttonPanel.add(aboutButton);
        
        gbc.gridy = 1;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gbc);
        
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
}
