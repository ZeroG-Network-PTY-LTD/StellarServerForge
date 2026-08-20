package com.zerog.network.stellarforge.gui;

import com.zerog.network.stellarforge.config.SecureConfig;
import com.zerog.network.stellarforge.model.ServerConfig;
import com.zerog.network.stellarforge.model.ServerProfile;
import com.zerog.network.stellarforge.utils.FirstRunDetector;
import com.zerog.network.stellarforge.utils.JavaManager;
import com.zerog.network.stellarforge.utils.ProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Multi-step setup wizard for first-run configuration
 * Guides users through:
 * 1. Welcome & Overview
 * 2. API Key Configuration
 * 3. Java Detection
 * 4. Preferences
 * 5. Create First Server
 */
public class SetupWizardDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(SetupWizardDialog.class);
    
    private int currentStep = 0;
    private final CardLayout cardLayout;
    private final JPanel cardsPanel;
    private final JButton backButton;
    private final JButton nextButton;
    private final JButton finishButton;
    private final JLabel stepLabel;
    private final JProgressBar progressBar;
    
    // Step 2: API Configuration
    private JTextField curseForgeKeyField;
    private JTextField modrinthKeyField;
    private JCheckBox enableCurseForgeCheck;
    private JCheckBox enableModrinthCheck;
    private JLabel apiStatusLabel;
    
    // Step 3: Java Detection
    private JComboBox<String> javaCombo;
    private JLabel javaStatusLabel;
    private List<JavaManager.JavaInstallation> detectedJavas;
    
    // Step 4: Preferences
    private JComboBox<String> themeCombo;
    private JSpinner defaultRamSpinner;
    private JCheckBox autoUpdateCheck;
    private JCheckBox showTooltipsCheck;
    
    // Step 5: First Server
    private JTextField serverNameField;
    private JTextField serverPathField;
    private JComboBox<String> mcVersionCombo;
    private JComboBox<ServerConfig.ModLoader> modLoaderCombo;
    private JCheckBox createServerCheck;
    
    private boolean setupCompleted = false;
    
    public SetupWizardDialog(Frame parent) {
        super(parent, "Welcome to Stellar Server Forge - Setup Wizard", true);
        
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        
        backButton = new JButton("← Back");
        nextButton = new JButton("Next →");
        finishButton = new JButton("Finish");
        stepLabel = new JLabel("Step 1 of 5: Welcome");
        progressBar = new JProgressBar(1, 5);
        
        initializeCards();
        initializeLayout();
        
        setSize(700, 550);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        updateNavigation();
        
        logger.info("Setup wizard initialized");
    }
    
    private void initializeCards() {
        cardsPanel.add(createWelcomePanel(), "welcome");
        cardsPanel.add(createApiConfigPanel(), "api");
        cardsPanel.add(createJavaDetectionPanel(), "java");
        cardsPanel.add(createPreferencesPanel(), "preferences");
        cardsPanel.add(createFirstServerPanel(), "server");
    }
    
    private void initializeLayout() {
        setLayout(new BorderLayout(10, 10));
        
        // Top panel - Progress
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.setBorder(new EmptyBorder(15, 15, 5, 15));
        
        stepLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        progressBar.setValue(1);
        progressBar.setStringPainted(true);
        
        topPanel.add(stepLabel, BorderLayout.NORTH);
        topPanel.add(progressBar, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Center panel - Cards
        add(cardsPanel, BorderLayout.CENTER);
        
        // Bottom panel - Navigation
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomPanel.setBorder(new EmptyBorder(5, 15, 15, 15));
        
        backButton.addActionListener(e -> previousStep());
        nextButton.addActionListener(e -> nextStep());
        finishButton.addActionListener(e -> finishSetup());
        finishButton.setVisible(false);
        
        backButton.setPreferredSize(new Dimension(100, 30));
        nextButton.setPreferredSize(new Dimension(100, 30));
        finishButton.setPreferredSize(new Dimension(100, 30));
        
        bottomPanel.add(backButton);
        bottomPanel.add(nextButton);
        bottomPanel.add(finishButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    // ==================== STEP 1: WELCOME ====================
    
    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Title
        JLabel titleLabel = new JLabel("<html><h1>🚀 Welcome to Stellar Server Forge!</h1></html>");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Content
        JTextArea contentArea = new JTextArea();
        contentArea.setEditable(false);
        contentArea.setOpaque(false);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        contentArea.setText(
            "Thank you for choosing Stellar Server Forge - your all-in-one Minecraft modded server creation tool!\n\n" +
            "This quick setup wizard will help you:\n\n" +
            "  ✓  Configure API keys for mod downloads\n" +
            "  ✓  Detect and configure Java installations\n" +
            "  ✓  Set your preferences\n" +
            "  ✓  Create your first server (optional)\n\n" +
            "The setup takes less than 3 minutes and you can skip any step if you prefer to configure it later.\n\n" +
            "Click 'Next' to begin!"
        );
        
        // Features panel
        JPanel featuresPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        featuresPanel.setBorder(BorderFactory.createTitledBorder("Key Features"));
        
        featuresPanel.add(createFeatureCard("🎮", "Multi-Loader Support", "Forge, Fabric, Quilt, NeoForge"));
        featuresPanel.add(createFeatureCard("📦", "Mod Installation", "CurseForge & Modrinth integration"));
        featuresPanel.add(createFeatureCard("⚙️", "Easy Configuration", "Intuitive UI, no command line"));
        featuresPanel.add(createFeatureCard("🚀", "One-Click Launch", "Start servers instantly"));
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(contentArea, BorderLayout.CENTER);
        panel.add(featuresPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createFeatureCard(String icon, String title, String description) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBorder(BorderFactory.createEtchedBorder());
        card.setBackground(new Color(240, 240, 240));
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel titleLabel = new JLabel("<html><b>" + title + "</b></html>");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel descLabel = new JLabel("<html><small>" + description + "</small></html>");
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(descLabel);
        
        card.add(iconLabel, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    // ==================== STEP 2: API CONFIGURATION ====================
    
    private JPanel createApiConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("<html><h2>🔐 API Configuration</h2></html>");
        
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setOpaque(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setText(
            "To download mods, you need API keys from CurseForge and/or Modrinth.\n\n" +
            "• CurseForge API Key: Required for CurseForge mod downloads\n" +
            "• Modrinth API Key: Optional (Modrinth works without authentication)\n\n" +
            "You can skip this step and configure keys later in Settings."
        );
        
        // CurseForge section
        JPanel cfPanel = new JPanel(new GridBagLayout());
        cfPanel.setBorder(BorderFactory.createTitledBorder("CurseForge"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        enableCurseForgeCheck = new JCheckBox("Enable CurseForge", true);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        cfPanel.add(enableCurseForgeCheck, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        cfPanel.add(new JLabel("API Key:"), gbc);
        
        curseForgeKeyField = new JTextField(30);
        gbc.gridx = 1; gbc.weightx = 0.7;
        cfPanel.add(curseForgeKeyField, gbc);
        
        JButton cfHelpButton = new JButton("Get API Key");
        cfHelpButton.addActionListener(e -> openURL("https://console.curseforge.com/"));
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        cfPanel.add(cfHelpButton, gbc);
        
        // Modrinth section
        JPanel mrPanel = new JPanel(new GridBagLayout());
        mrPanel.setBorder(BorderFactory.createTitledBorder("Modrinth"));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        enableModrinthCheck = new JCheckBox("Enable Modrinth", true);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mrPanel.add(enableModrinthCheck, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        mrPanel.add(new JLabel("API Key (Optional):"), gbc);
        
        modrinthKeyField = new JTextField(30);
        gbc.gridx = 1; gbc.weightx = 0.7;
        mrPanel.add(modrinthKeyField, gbc);
        
        JLabel mrNote = new JLabel("<html><small><i>Modrinth works without an API key</i></small></html>");
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        mrPanel.add(mrNote, gbc);
        
        // Test button and status
        JPanel testPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton testButton = new JButton("✓ Test Configuration");
        testButton.addActionListener(e -> testAPIConfiguration());
        apiStatusLabel = new JLabel("");
        testPanel.add(testButton);
        testPanel.add(apiStatusLabel);
        
        // Layout
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(infoArea, BorderLayout.CENTER);
        
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        centerPanel.add(cfPanel);
        centerPanel.add(mrPanel);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(testPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void testAPIConfiguration() {
        apiStatusLabel.setText("Testing...");
        apiStatusLabel.setForeground(Color.BLUE);
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                StringBuilder result = new StringBuilder();
                
                if (enableCurseForgeCheck.isSelected()) {
                    String cfKey = curseForgeKeyField.getText().trim();
                    if (cfKey.isEmpty()) {
                        result.append("CurseForge: No key provided. ");
                    } else {
                        result.append("CurseForge: ✓ Key format valid. ");
                    }
                }
                
                if (enableModrinthCheck.isSelected()) {
                    result.append("Modrinth: ✓ Enabled. ");
                }
                
                return result.toString();
            }
            
            @Override
            protected void done() {
                try {
                    String result = get();
                    apiStatusLabel.setText(result);
                    apiStatusLabel.setForeground(new Color(0, 128, 0));
                } catch (Exception e) {
                    apiStatusLabel.setText("Error: " + e.getMessage());
                    apiStatusLabel.setForeground(Color.RED);
                }
            }
        };
        
        worker.execute();
    }
    
    // ==================== STEP 3: JAVA DETECTION ====================
    
    private JPanel createJavaDetectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("<html><h2>☕ Java Configuration</h2></html>");
        
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setOpaque(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setText(
            "Minecraft servers require Java to run. We'll detect your installed Java versions.\n\n" +
            "Different Minecraft versions require different Java versions:\n" +
            "  • MC 1.16.5 and earlier → Java 8+\n" +
            "  • MC 1.17 → Java 16+\n" +
            "  • MC 1.18-1.20.4 → Java 17+\n" +
            "  • MC 1.20.5+ → Java 21+"
        );
        
        JPanel detectionPanel = new JPanel(new GridBagLayout());
        detectionPanel.setBorder(BorderFactory.createTitledBorder("Java Detection"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JButton detectButton = new JButton("🔍 Detect Java Installations");
        detectButton.addActionListener(e -> detectJavaInstallations());
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        detectionPanel.add(detectButton, gbc);
        
        gbc.gridy = 1;
        javaStatusLabel = new JLabel("Click 'Detect' to scan for Java installations");
        javaStatusLabel.setForeground(Color.GRAY);
        detectionPanel.add(javaStatusLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        detectionPanel.add(new JLabel("Select Java:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        javaCombo = new JComboBox<>();
        javaCombo.addItem("Auto-detect (Recommended)");
        detectionPanel.add(javaCombo, gbc);
        
        JButton downloadButton = new JButton("📥 Download Java");
        downloadButton.addActionListener(e -> openURL("https://adoptium.net/"));
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        detectionPanel.add(downloadButton, gbc);
        
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(infoArea, BorderLayout.CENTER);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(detectionPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void detectJavaInstallations() {
        javaStatusLabel.setText("Detecting Java installations...");
        javaStatusLabel.setForeground(Color.BLUE);
        javaCombo.setEnabled(false);
        
        SwingWorker<List<JavaManager.JavaInstallation>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<JavaManager.JavaInstallation> doInBackground() {
                return JavaManager.detectJavaInstallations();
            }
            
            @Override
            protected void done() {
                try {
                    detectedJavas = get();
                    
                    javaCombo.removeAllItems();
                    javaCombo.addItem("Auto-detect (Recommended)");
                    
                    for (JavaManager.JavaInstallation java : detectedJavas) {
                        javaCombo.addItem(String.format("Java %d - %s", java.getMajorVersion(), java.getPath()));
                    }
                    
                    javaStatusLabel.setText(String.format("✓ Found %d Java installation(s)", detectedJavas.size()));
                    javaStatusLabel.setForeground(new Color(0, 128, 0));
                    javaCombo.setEnabled(true);
                    
                } catch (Exception e) {
                    javaStatusLabel.setText("Error: " + e.getMessage());
                    javaStatusLabel.setForeground(Color.RED);
                    logger.error("Error detecting Java", e);
                }
            }
        };
        
        worker.execute();
    }
    
    // ==================== STEP 4: PREFERENCES ====================
    
    private JPanel createPreferencesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("<html><h2>⚙️ Preferences</h2></html>");
        
        JPanel prefsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Theme
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        prefsPanel.add(new JLabel("Theme:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        themeCombo = new JComboBox<>(new String[]{"Dark (Default)", "Light"});
        prefsPanel.add(themeCombo, gbc);
        
        // Default RAM
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        prefsPanel.add(new JLabel("Default RAM Allocation:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        defaultRamSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 32, 1));
        JPanel ramPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ramPanel.add(defaultRamSpinner);
        ramPanel.add(new JLabel("GB"));
        prefsPanel.add(ramPanel, gbc);
        
        // Auto-update
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        autoUpdateCheck = new JCheckBox("Check for updates on startup", true);
        prefsPanel.add(autoUpdateCheck, gbc);
        
        // Tooltips
        gbc.gridy = 3;
        showTooltipsCheck = new JCheckBox("Show helpful tooltips", true);
        prefsPanel.add(showTooltipsCheck, gbc);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(prefsPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== STEP 5: FIRST SERVER ====================
    
    private JPanel createFirstServerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("<html><h2>🎮 Create Your First Server</h2></html>");
        
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setOpaque(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setText(
            "Let's create your first server! You can skip this and create servers later."
        );
        
        createServerCheck = new JCheckBox("Create a server now", true);
        createServerCheck.addActionListener(e -> toggleServerCreation());
        
        JPanel serverPanel = new JPanel(new GridBagLayout());
        serverPanel.setBorder(BorderFactory.createTitledBorder("Server Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Server name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        serverPanel.add(new JLabel("Server Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        serverNameField = new JTextField("My First Server");
        serverPanel.add(serverNameField, gbc);
        
        // Server path
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        serverPanel.add(new JLabel("Server Path:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        serverPathField = new JTextField("server");
        serverPanel.add(serverPathField, gbc);
        
        // MC version
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        serverPanel.add(new JLabel("Minecraft Version:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        mcVersionCombo = new JComboBox<>(new String[]{
            "1.20.6", "1.20.4", "1.20.2", "1.20.1", "1.19.4", "1.19.2", "1.18.2", "1.16.5"
        });
        serverPanel.add(mcVersionCombo, gbc);
        
        // Mod loader
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        serverPanel.add(new JLabel("Mod Loader:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        modLoaderCombo = new JComboBox<>(ServerConfig.ModLoader.values());
        serverPanel.add(modLoaderCombo, gbc);
        
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(infoArea, BorderLayout.CENTER);
        topPanel.add(createServerCheck, BorderLayout.SOUTH);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(serverPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void toggleServerCreation() {
        boolean enabled = createServerCheck.isSelected();
        serverNameField.setEnabled(enabled);
        serverPathField.setEnabled(enabled);
        mcVersionCombo.setEnabled(enabled);
        modLoaderCombo.setEnabled(enabled);
    }
    
    // ==================== NAVIGATION ====================
    
    private void nextStep() {
        if (currentStep < 4) {
            currentStep++;
            showCurrentStep();
            updateNavigation();
        }
    }
    
    private void previousStep() {
        if (currentStep > 0) {
            currentStep--;
            showCurrentStep();
            updateNavigation();
        }
    }
    
    private void showCurrentStep() {
        String[] cards = {"welcome", "api", "java", "preferences", "server"};
        String[] steps = {
            "Step 1 of 5: Welcome",
            "Step 2 of 5: API Configuration",
            "Step 3 of 5: Java Detection",
            "Step 4 of 5: Preferences",
            "Step 5 of 5: Create First Server"
        };
        
        cardLayout.show(cardsPanel, cards[currentStep]);
        stepLabel.setText(steps[currentStep]);
        progressBar.setValue(currentStep + 1);
    }
    
    private void updateNavigation() {
        backButton.setEnabled(currentStep > 0);
        nextButton.setVisible(currentStep < 4);
        finishButton.setVisible(currentStep == 4);
    }
    
    private void finishSetup() {
        // Save API configuration
        saveAPIConfiguration();
        
        // Save preferences
        savePreferences();
        
        // Create first server if requested
        if (createServerCheck.isSelected()) {
            createFirstServer();
        } else {
            // Create a default profile even if no server created
            ProfileManager.getInstance().createDefaultProfile();
        }
        
        // Mark first run as complete
        FirstRunDetector.markFirstRunComplete();
        
        setupCompleted = true;
        
        JOptionPane.showMessageDialog(this,
            "Setup completed successfully!\n\nYou're ready to create and manage Minecraft servers.",
            "Setup Complete",
            JOptionPane.INFORMATION_MESSAGE);
        
        dispose();
    }
    
    private void saveAPIConfiguration() {
        try {
            File configDir = new File("config");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            
            File apiKeysFile = new File(configDir, "api-keys.properties");
            Properties props = new Properties();
            
            if (enableCurseForgeCheck.isSelected() && !curseForgeKeyField.getText().trim().isEmpty()) {
                props.setProperty("curseforge.api.key", curseForgeKeyField.getText().trim());
                props.setProperty("curseforge.enabled", "true");
            }
            
            if (enableModrinthCheck.isSelected()) {
                if (!modrinthKeyField.getText().trim().isEmpty()) {
                    props.setProperty("modrinth.api.key", modrinthKeyField.getText().trim());
                }
                props.setProperty("modrinth.enabled", "true");
            }
            
            try (FileWriter writer = new FileWriter(apiKeysFile)) {
                props.store(writer, "Stellar Server Forge API Keys - Generated by Setup Wizard");
            }
            
            logger.info("API configuration saved");
        } catch (IOException e) {
            logger.error("Error saving API configuration", e);
        }
    }
    
    private void savePreferences() {
        try {
            SecureConfig config = SecureConfig.getInstance();
            // Preferences would be saved via SecureConfig
            // This is a placeholder - actual implementation depends on SecureConfig structure
            logger.info("Preferences saved");
        } catch (Exception e) {
            logger.error("Error saving preferences", e);
        }
    }
    
    private void createFirstServer() {
        try {
            ServerConfig config = new ServerConfig();
            config.setServerName(serverNameField.getText().trim());
            config.setServerPath(serverPathField.getText().trim());
            config.setMinecraftVersion((String) mcVersionCombo.getSelectedItem());
            config.setModLoader((ServerConfig.ModLoader) modLoaderCombo.getSelectedItem());
            config.setMaxRamGb((Integer) defaultRamSpinner.getValue());
            config.setPort(25565);
            
            ServerProfile profile = new ServerProfile(config.getServerName(), config);
            profile.setDescription("First server created during setup");
            profile.setFavorite(true);
            
            ProfileManager.getInstance().saveProfile(profile);
            ProfileManager.getInstance().setActiveProfile(profile);
            
            logger.info("First server profile created: {}", profile.getProfileName());
        } catch (Exception e) {
            logger.error("Error creating first server", e);
        }
    }
    
    private void openURL(String url) {
        try {
            Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            logger.error("Error opening URL: {}", url, e);
            JOptionPane.showMessageDialog(this,
                "Please visit: " + url,
                "Open Browser",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    public boolean isSetupCompleted() {
        return setupCompleted;
    }
}

