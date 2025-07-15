package com.zerog.network.stellarforge;

import com.formdev.flatlaf.FlatDarkLaf;
import com.zerog.network.stellarforge.config.SecureConfig;
import com.zerog.network.stellarforge.gui.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Main entry point for the Stellar Server Forge application
 * ZeroG Network - Space-themed Minecraft server creator
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        // Initialize secure configuration first
        try {
            SecureConfig.initialize();
        } catch (Exception e) {
            logger.error("Failed to initialize secure configuration", e);
            JOptionPane.showMessageDialog(null, 
                "Failed to initialize configuration: " + e.getMessage() + 
                "\nPlease check the config directory and ensure API keys are properly configured.",
                "Configuration Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            logger.warn("Failed to set FlatDarkLaf, using default look and feel", e);
        }
        
        // Set system properties for better UI on high-DPI displays
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        // Create and display the main window
        SwingUtilities.invokeLater(() -> {
            try {
                MainWindow mainWindow = new MainWindow();
                mainWindow.setVisible(true);
                logger.info("{} v{} started successfully", 
                    SecureConfig.getAppName(), SecureConfig.getAppVersion());
            } catch (Exception e) {
                logger.error("Failed to start {}", SecureConfig.getAppName(), e);
                JOptionPane.showMessageDialog(null, 
                    "Failed to start application: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
