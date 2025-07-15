package com.universalator.gui;

import com.universalator.api.CurseForgeClient;
import com.universalator.api.ModrinthClient;
import com.universalator.model.ModInfo;
import com.universalator.model.ServerConfig;
import com.universalator.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Mod installer dialog
 */
public class ModInstaller extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(ModInstaller.class);
    
    private final ServerConfig serverConfig;
    private final CurseForgeClient curseForgeClient;
    private final ModrinthClient modrinthClient;
    
    // GUI Components
    private JTabbedPane tabbedPane;
    private JTextField searchField;
    private JComboBox<String> platformCombo;
    private JButton searchButton;
    private JList<ModInfo> searchResultsList;
    private JList<ModInfo> suggestedModsList;
    private JTextArea modDescriptionArea;
    private JButton installButton;
    private JButton installAllSuggestedButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    
    private DefaultListModel<ModInfo> searchResultsModel;
    private DefaultListModel<ModInfo> suggestedModsModel;
    
    public ModInstaller(Frame parent, ServerConfig serverConfig) {
        super(parent, "Mod Installer", true);
        this.serverConfig = serverConfig;
        this.curseForgeClient = new CurseForgeClient();
        this.modrinthClient = new ModrinthClient();
        
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        loadSuggestedMods();
        
        setSize(800, 600);
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        // Search components
        searchField = new JTextField(20);
        platformCombo = new JComboBox<>(new String[]{"Both", "CurseForge", "Modrinth"});
        searchButton = new JButton("Search");
        
        // Lists
        searchResultsModel = new DefaultListModel<>();
        searchResultsList = new JList<>(searchResultsModel);
        searchResultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResultsList.setCellRenderer(new ModListCellRenderer());
        
        suggestedModsModel = new DefaultListModel<>();
        suggestedModsList = new JList<>(suggestedModsModel);
        suggestedModsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        suggestedModsList.setCellRenderer(new ModListCellRenderer());
        
        // Description area
        modDescriptionArea = new JTextArea(5, 30);
        modDescriptionArea.setEditable(false);
        modDescriptionArea.setLineWrap(true);
        modDescriptionArea.setWrapStyleWord(true);
        
        // Buttons
        installButton = new JButton("Install Selected Mod");
        installAllSuggestedButton = new JButton("Install All Suggested");
        
        // Progress and status
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        statusLabel = new JLabel("Ready");
        
        // Tabbed pane
        tabbedPane = new JTabbedPane();
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        // Search tab
        JPanel searchPanel = createSearchPanel();
        tabbedPane.addTab("Search Mods", searchPanel);
        
        // Suggested mods tab
        JPanel suggestedPanel = createSuggestedModsPanel();
        tabbedPane.addTab("Suggested Mods", suggestedPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(progressBar, BorderLayout.CENTER);
        statusPanel.add(statusLabel, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Search controls
        JPanel searchControls = new JPanel(new FlowLayout());
        searchControls.add(new JLabel("Search:"));
        searchControls.add(searchField);
        searchControls.add(new JLabel("Platform:"));
        searchControls.add(platformCombo);
        searchControls.add(searchButton);
        panel.add(searchControls, BorderLayout.NORTH);
        
        // Split pane with results and details
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // Results list
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(new TitledBorder("Search Results"));
        resultsPanel.add(new JScrollPane(searchResultsList), BorderLayout.CENTER);
        splitPane.setLeftComponent(resultsPanel);
        
        // Details panel
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(new TitledBorder("Mod Details"));
        detailsPanel.add(new JScrollPane(modDescriptionArea), BorderLayout.CENTER);
        detailsPanel.add(installButton, BorderLayout.SOUTH);
        splitPane.setRightComponent(detailsPanel);
        
        splitPane.setDividerLocation(400);
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createSuggestedModsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Info label
        JLabel infoLabel = new JLabel("<html><b>Popular mods for " + 
            serverConfig.getModLoader().getDisplayName() + " " + 
            serverConfig.getMinecraftVersion() + "</b></html>");
        panel.add(infoLabel, BorderLayout.NORTH);
        
        // Suggested mods list
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(new TitledBorder("Recommended Mods"));
        listPanel.add(new JScrollPane(suggestedModsList), BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(installAllSuggestedButton);
        buttonPanel.add(new JButton("Install Selected"));
        listPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        panel.add(listPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        // Search button
        searchButton.addActionListener(e -> performSearch());
        
        // Enter key in search field
        searchField.addActionListener(e -> performSearch());
        
        // Install button
        installButton.addActionListener(e -> installSelectedMod());
        
        // Install all suggested button
        installAllSuggestedButton.addActionListener(e -> installAllSuggestedMods());
        
        // Selection listeners
        searchResultsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ModInfo selectedMod = searchResultsList.getSelectedValue();
                if (selectedMod != null) {
                    updateModDetails(selectedMod);
                }
            }
        });
        
        suggestedModsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ModInfo selectedMod = suggestedModsList.getSelectedValue();
                if (selectedMod != null) {
                    updateModDetails(selectedMod);
                }
            }
        });
    }
    
    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search term", "Search Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String platform = (String) platformCombo.getSelectedItem();
        
        // Clear previous results
        searchResultsModel.clear();
        updateStatus("Searching...");
        progressBar.setIndeterminate(true);
        
        // Perform search asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                List<ModInfo> results = null;
                
                if ("CurseForge".equals(platform)) {
                    results = curseForgeClient.searchMods(query, serverConfig.getMinecraftVersion(), 
                        serverConfig.getModLoader().name(), 20);
                } else if ("Modrinth".equals(platform)) {
                    results = modrinthClient.searchMods(query, serverConfig.getMinecraftVersion(), 
                        serverConfig.getModLoader().name(), 20);
                } else {
                    // Search both platforms
                    List<ModInfo> curseResults = curseForgeClient.searchMods(query, serverConfig.getMinecraftVersion(), 
                        serverConfig.getModLoader().name(), 10);
                    List<ModInfo> modrinthResults = modrinthClient.searchMods(query, serverConfig.getMinecraftVersion(), 
                        serverConfig.getModLoader().name(), 10);
                    
                    results = curseResults;
                    results.addAll(modrinthResults);
                }
                
                // Update UI on EDT
                final List<ModInfo> finalResults = results;
                SwingUtilities.invokeLater(() -> {
                    if (finalResults != null) {
                        for (ModInfo mod : finalResults) {
                            searchResultsModel.addElement(mod);
                        }
                        updateStatus("Found " + finalResults.size() + " mods");
                    } else {
                        updateStatus("Search failed");
                    }
                    progressBar.setIndeterminate(false);
                });
                
            } catch (Exception e) {
                logger.error("Error performing search", e);
                SwingUtilities.invokeLater(() -> {
                    updateStatus("Search error: " + e.getMessage());
                    progressBar.setIndeterminate(false);
                });
            }
        });
    }
    
    private void loadSuggestedMods() {
        updateStatus("Loading suggested mods...");
        progressBar.setIndeterminate(true);
        
        CompletableFuture.runAsync(() -> {
            try {
                // Load from both platforms
                List<ModInfo> curseSuggested = curseForgeClient.getSuggestedMods(
                    serverConfig.getMinecraftVersion(), serverConfig.getModLoader().name());
                List<ModInfo> modrinthSuggested = modrinthClient.getSuggestedMods(
                    serverConfig.getMinecraftVersion(), serverConfig.getModLoader().name());
                
                SwingUtilities.invokeLater(() -> {
                    for (ModInfo mod : curseSuggested) {
                        suggestedModsModel.addElement(mod);
                    }
                    for (ModInfo mod : modrinthSuggested) {
                        suggestedModsModel.addElement(mod);
                    }
                    updateStatus("Loaded " + (curseSuggested.size() + modrinthSuggested.size()) + " suggested mods");
                    progressBar.setIndeterminate(false);
                });
                
            } catch (Exception e) {
                logger.error("Error loading suggested mods", e);
                SwingUtilities.invokeLater(() -> {
                    updateStatus("Error loading suggested mods");
                    progressBar.setIndeterminate(false);
                });
            }
        });
    }
    
    private void installSelectedMod() {
        ModInfo selectedMod = searchResultsList.getSelectedValue();
        if (selectedMod == null) {
            selectedMod = suggestedModsList.getSelectedValue();
        }
        
        if (selectedMod != null) {
            installMod(selectedMod);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a mod to install", "Installation Error", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void installAllSuggestedMods() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "This will install all suggested mods. Continue?", 
            "Confirm Installation", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            for (int i = 0; i < suggestedModsModel.getSize(); i++) {
                ModInfo mod = suggestedModsModel.getElementAt(i);
                installMod(mod);
            }
        }
    }
    
    private void installMod(ModInfo mod) {
        updateStatus("Installing " + mod.getName() + "...");
        progressBar.setIndeterminate(true);
        
        CompletableFuture.runAsync(() -> {
            try {
                String downloadUrl = mod.getUrl();
                
                // Get download URL if not available
                if (downloadUrl == null || downloadUrl.isEmpty()) {
                    if (mod.getSource() == ModInfo.ModSource.CURSEFORGE) {
                        downloadUrl = curseForgeClient.getModDownloadUrl(mod.getProjectId(), mod.getFileId());
                    } else if (mod.getSource() == ModInfo.ModSource.MODRINTH) {
                        downloadUrl = modrinthClient.getModDownloadUrl(mod.getProjectId(), mod.getFileId());
                    }
                }
                
                if (downloadUrl != null) {
                    String modsDir = serverConfig.getServerPath() + "/mods";
                    String fileName = mod.getFileName() != null ? mod.getFileName() : mod.getName() + ".jar";
                    String filePath = modsDir + "/" + fileName;
                    
                    boolean success = FileUtil.downloadFile(downloadUrl, filePath);
                    
                    SwingUtilities.invokeLater(() -> {
                        if (success) {
                            updateStatus("Successfully installed " + mod.getName());
                            // Add to installed mods list
                            serverConfig.getInstalledMods().add(mod);
                        } else {
                            updateStatus("Failed to install " + mod.getName());
                        }
                        progressBar.setIndeterminate(false);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        updateStatus("No download URL available for " + mod.getName());
                        progressBar.setIndeterminate(false);
                    });
                }
                
            } catch (Exception e) {
                logger.error("Error installing mod", e);
                SwingUtilities.invokeLater(() -> {
                    updateStatus("Error installing " + mod.getName() + ": " + e.getMessage());
                    progressBar.setIndeterminate(false);
                });
            }
        });
    }
    
    private void updateModDetails(ModInfo mod) {
        if (mod != null) {
            StringBuilder details = new StringBuilder();
            details.append("Name: ").append(mod.getName()).append("\n");
            details.append("Version: ").append(mod.getVersion() != null ? mod.getVersion() : "Unknown").append("\n");
            details.append("Source: ").append(mod.getSource().getDisplayName()).append("\n");
            details.append("Minecraft Version: ").append(mod.getMinecraftVersion() != null ? mod.getMinecraftVersion() : "Unknown").append("\n");
            details.append("Mod Loader: ").append(mod.getModLoaderType() != null ? mod.getModLoaderType() : "Unknown").append("\n");
            if (mod.getFileSize() > 0) {
                details.append("File Size: ").append(FileUtil.getHumanReadableSize(mod.getFileSize())).append("\n");
            }
            details.append("\nDescription:\n").append(mod.getDescription() != null ? mod.getDescription() : "No description available");
            
            modDescriptionArea.setText(details.toString());
        } else {
            modDescriptionArea.setText("Select a mod to view details");
        }
    }
    
    private void updateStatus(String status) {
        statusLabel.setText(status);
    }
    
    /**
     * Custom cell renderer for mod list
     */
    private static class ModListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                boolean isSelected, boolean cellHasFocus) {
            
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof ModInfo) {
                ModInfo mod = (ModInfo) value;
                setText(mod.getName() + " (" + mod.getSource().getDisplayName() + ")");
                
                // Set icon based on source
                if (mod.getSource() == ModInfo.ModSource.CURSEFORGE) {
                    setIcon(createColoredIcon(Color.ORANGE));
                } else if (mod.getSource() == ModInfo.ModSource.MODRINTH) {
                    setIcon(createColoredIcon(Color.GREEN));
                } else {
                    setIcon(createColoredIcon(Color.GRAY));
                }
            }
            
            return this;
        }
        
        private Icon createColoredIcon(Color color) {
            return new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    g.setColor(color);
                    g.fillOval(x, y, getIconWidth(), getIconHeight());
                }
                
                @Override
                public int getIconWidth() { return 12; }
                
                @Override
                public int getIconHeight() { return 12; }
            };
        }
    }
}
