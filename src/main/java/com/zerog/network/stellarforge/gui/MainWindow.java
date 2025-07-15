package com.zerog.network.stellarforge.gui;

import com.zerog.network.stellarforge.config.SecureConfig;
import com.zerog.network.stellarforge.model.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
        
        JButton aboutButton = new JButton("About");
        aboutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAboutDialog();
            }
        });
        
        buttonPanel.add(configureButton);
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
}
