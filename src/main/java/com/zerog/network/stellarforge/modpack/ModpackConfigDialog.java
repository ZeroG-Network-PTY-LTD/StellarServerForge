package com.zerog.network.stellarforge.modpack;

import com.zerog.network.stellarforge.api.CurseForgeClient;
import com.zerog.network.stellarforge.util.ModLoaderVersionFetcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for configuring modpack import settings
 */
public class ModpackConfigDialog extends JDialog {
    
    private final ModpackManifest manifest;
    private final ModLoaderVersionFetcher versionFetcher;
    private ModpackConfig config;
    private boolean confirmed = false;
    
    private JTextField serverNameField;
    private JTextField serverPathField;
    private JComboBox<String> minecraftVersionCombo;
    private JComboBox<String> modLoaderCombo;
    private JComboBox<String> modLoaderVersionCombo;
    private JButton confirmButton;
    private JButton cancelButton;
    
    public ModpackConfigDialog(JFrame parent, ModpackManifest manifest) {
        super(parent, "Configure Modpack Import", true);
        this.manifest = manifest;
        this.versionFetcher = new ModLoaderVersionFetcher();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        populateFromManifest();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        serverNameField = new JTextField(20);
        serverPathField = new JTextField(20);
        
        // Minecraft versions (common versions)
        String[] minecraftVersions = {
            "1.20.1", "1.19.4", "1.19.2", "1.18.2", "1.17.1", "1.16.5", "1.12.2"
        };
        minecraftVersionCombo = new JComboBox<>(minecraftVersions);
        minecraftVersionCombo.setEditable(true);
        
        // Mod loaders
        String[] modLoaders = {"forge", "fabric", "quilt", "neoforge"};
        modLoaderCombo = new JComboBox<>(modLoaders);
        
        // Mod loader versions (will be populated based on selection)
        modLoaderVersionCombo = new JComboBox<>();
        
        confirmButton = new JButton("Import Modpack");
        cancelButton = new JButton("Cancel");
        
        // Browse button for server path
        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> browseServerPath());
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Server Name
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Server Name:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        mainPanel.add(serverNameField, gbc);
        
        // Server Path
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        mainPanel.add(new JLabel("Server Path:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(serverPathField, gbc);
        gbc.gridx = 2;
        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> browseServerPath());
        mainPanel.add(browseButton, gbc);
        
        // Minecraft Version
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(new JLabel("Minecraft Version:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        mainPanel.add(minecraftVersionCombo, gbc);
        
        // Mod Loader
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        mainPanel.add(new JLabel("Mod Loader:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        mainPanel.add(modLoaderCombo, gbc);
        
        // Mod Loader Version
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        mainPanel.add(new JLabel("Mod Loader Version:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        mainPanel.add(modLoaderVersionCombo, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Add info panel at top if manifest exists
        if (manifest != null) {
            JPanel infoPanel = new JPanel(new BorderLayout());
            infoPanel.setBorder(BorderFactory.createTitledBorder("Modpack Information"));
            
            StringBuilder info = new StringBuilder("<html>");
            info.append("<b>Name:</b> ").append(manifest.name != null ? manifest.name : "Unknown").append("<br>");
            info.append("<b>Version:</b> ").append(manifest.version != null ? manifest.version : "Unknown").append("<br>");
            info.append("<b>Author:</b> ").append(manifest.author != null ? manifest.author : "Unknown").append("<br>");
            if (manifest.files != null) {
                info.append("<b>Mods:</b> ").append(manifest.files.size()).append(" total");
            }
            info.append("</html>");
            
            JLabel infoLabel = new JLabel(info.toString());
            infoPanel.add(infoLabel, BorderLayout.CENTER);
            add(infoPanel, BorderLayout.NORTH);
        }
    }
    
    private void setupEventHandlers() {
        confirmButton.addActionListener(e -> {
            if (validateInput()) {
                createConfig();
                confirmed = true;
                dispose();
            }
        });
        
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });
        
        // Update mod loader versions when mod loader changes
        modLoaderCombo.addActionListener(e -> updateModLoaderVersions());
        minecraftVersionCombo.addActionListener(e -> updateModLoaderVersions());
    }
    
    private void populateFromManifest() {
        if (manifest != null) {
            if (manifest.name != null) {
                serverNameField.setText(manifest.name + " Server");
            }
            
            if (manifest.minecraft != null) {
                if (manifest.minecraft.version != null) {
                    minecraftVersionCombo.setSelectedItem(manifest.minecraft.version);
                }
                
                if (manifest.minecraft.modLoaders != null && !manifest.minecraft.modLoaders.isEmpty()) {
                    ModpackManifest.ModLoader primaryLoader = manifest.minecraft.modLoaders.stream()
                        .filter(ml -> ml.primary)
                        .findFirst()
                        .orElse(manifest.minecraft.modLoaders.get(0));
                    
                    modLoaderCombo.setSelectedItem(primaryLoader.id);
                }
            }
        }
        
        // Set default server path
        serverPathField.setText(System.getProperty("user.home") + "/Desktop/MinecraftServer");
        
        // Update mod loader versions
        updateModLoaderVersions();
    }
    
    private void updateModLoaderVersions() {
        String selectedLoader = (String) modLoaderCombo.getSelectedItem();
        String selectedVersion = (String) minecraftVersionCombo.getSelectedItem();
        
        if (selectedLoader == null || selectedVersion == null) return;
        
        modLoaderVersionCombo.removeAllItems();
        modLoaderVersionCombo.addItem("Loading...");
        modLoaderVersionCombo.setEnabled(false);
        
        // Fetch versions asynchronously to avoid blocking the UI
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return getModLoaderVersions(selectedLoader, selectedVersion);
            }
            
            @Override
            protected void done() {
                try {
                    List<String> versions = get();
                    
                    modLoaderVersionCombo.removeAllItems();
                    for (String version : versions) {
                        modLoaderVersionCombo.addItem(version);
                    }
                    modLoaderVersionCombo.setEnabled(true);
                    
                    // Pre-select from manifest if available
                    if (manifest != null && manifest.minecraft != null && manifest.minecraft.modLoaders != null) {
                        for (ModpackManifest.ModLoader loader : manifest.minecraft.modLoaders) {
                            if (loader.id.equals(selectedLoader)) {
                                // Try to find version in manifest (this would need enhancement)
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    modLoaderVersionCombo.removeAllItems();
                    modLoaderVersionCombo.addItem("Latest");
                    modLoaderVersionCombo.setEnabled(true);
                }
            }
        };
        
        worker.execute();
    }
    
    private List<String> getModLoaderVersions(String loader, String mcVersion) {
        // Use the new version fetcher for dynamic version retrieval
        List<String> versions = versionFetcher.getModLoaderVersions(loader, mcVersion);
        
        if (versions.isEmpty()) {
            versions.add("Latest");
        }
        
        return versions;
    }
    
    private void browseServerPath() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Server Directory");
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            serverPathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private boolean validateInput() {
        if (serverNameField.getText().trim().isEmpty()) {
            showError("Server name is required");
            return false;
        }
        
        if (serverPathField.getText().trim().isEmpty()) {
            showError("Server path is required");
            return false;
        }
        
        if (minecraftVersionCombo.getSelectedItem() == null) {
            showError("Minecraft version is required");
            return false;
        }
        
        if (modLoaderCombo.getSelectedItem() == null) {
            showError("Mod loader is required");
            return false;
        }
        
        if (modLoaderVersionCombo.getSelectedItem() == null) {
            showError("Mod loader version is required");
            return false;
        }
        
        return true;
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void createConfig() {
        config = new ModpackConfig(
            (String) minecraftVersionCombo.getSelectedItem(),
            (String) modLoaderCombo.getSelectedItem(),
            (String) modLoaderVersionCombo.getSelectedItem(),
            serverPathField.getText().trim(),
            serverNameField.getText().trim()
        );
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public ModpackConfig getConfig() {
        return config;
    }
}
