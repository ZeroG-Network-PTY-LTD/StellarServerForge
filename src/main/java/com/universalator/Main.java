package com.universalator;

import com.formdev.flatlaf.FlatDarkLaf;
import com.universalator.gui.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Main entry point for the Universalator GUI application
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
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
                logger.info("Universalator GUI started successfully");
            } catch (Exception e) {
                logger.error("Failed to start Universalator GUI", e);
                JOptionPane.showMessageDialog(null, 
                    "Failed to start application: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
