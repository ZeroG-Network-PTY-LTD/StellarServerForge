package com.zerog.network.stellarforge;

import com.formdev.flatlaf.FlatDarkLaf;
import com.zerog.network.stellarforge.config.SecureConfig;
import com.zerog.network.stellarforge.gui.MainWindow;

import javax.swing.*;

/**
 * Main entry point for the Stellar Server Forge application
 * ZeroG Network - Space-themed Minecraft server creator
 */
public class Main {
    
    public static void main(String[] args) {
        // Initialize secure configuration first
        try {
            SecureConfig.getInstance();
        } catch (Exception e) {
            System.err.println("Failed to initialize secure configuration: " + e.getMessage());
            JOptionPane.showMessageDialog(null, 
                "Failed to initialize configuration: " + e.getMessage() + 
                "\nPlease check the application installation.",
                "Configuration Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to set FlatDarkLaf, using default look and feel: " + e.getMessage());
        }
        
        // Set system properties for better UI on high-DPI displays
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        // Create and display the main window
        SwingUtilities.invokeLater(() -> {
            try {
                MainWindow mainWindow = new MainWindow();
                mainWindow.setVisible(true);
                System.out.println(SecureConfig.getInstance().getAppName() + " v" + 
                    SecureConfig.getInstance().getAppVersion() + " started successfully");
            } catch (Exception e) {
                System.err.println("Failed to start " + SecureConfig.getInstance().getAppName() + ": " + e.getMessage());
                JOptionPane.showMessageDialog(null, 
                    "Failed to start application: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
