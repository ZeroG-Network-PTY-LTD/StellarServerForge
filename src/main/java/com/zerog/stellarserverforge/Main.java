package com.zerog.stellarserverforge;

import com.zerog.stellarserverforge.gui.MainFrame;
import com.zerog.stellarserverforge.gui.theme.StellarTheme;

import javax.swing.SwingUtilities;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StellarTheme.install();
            new MainFrame().setVisible(true);
        });
    }
}
