package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.gui.theme.StarfieldPanel;
import com.zerog.stellarserverforge.gui.theme.StellarIcon;
import com.zerog.stellarserverforge.model.ServerSettings;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainFrame extends JFrame {

    private static final String CARD_WIZARD = "wizard";
    private static final String CARD_DASHBOARD = "dashboard";

    private final AppContext ctx;
    private final CardLayout cardLayout = new CardLayout();
    private final StarfieldPanel cards = new StarfieldPanel(new CardLayout());

    private DashboardPanel dashboardPanel;
    private SetupWizardPanel wizardPanel;

    public MainFrame() {
        super("StellarServerForge");
        this.ctx = new AppContext(resolveServerDir());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);
        setIconImage(StellarIcon.create(64));

        cards.setLayout(cardLayout);
        setContentPane(cards);

        wizardPanel = new SetupWizardPanel(ctx, this::onSetupComplete);
        cards.add(wizardPanel, CARD_WIZARD);

        if (ctx.settingsService.exists()) {
            try {
                ServerSettings settings = ctx.settingsService.load();
                showDashboard(settings);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Could not read settings.json (" + e.getMessage() + "). Starting setup again.",
                        "Settings error", JOptionPane.WARNING_MESSAGE);
                cardLayout.show(cards, CARD_WIZARD);
            }
        } else {
            cardLayout.show(cards, CARD_WIZARD);
        }
    }

    /** The server directory tracks the running jar's own folder, not the process's working
     * directory — so settings.json/mods/etc. land next to the built jar regardless of how it was
     * launched (double-click, a shortcut with a different "Start in" folder, a script elsewhere). */
    private static Path resolveServerDir() {
        try {
            Path location = Path.of(MainFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path dir = Files.isRegularFile(location) ? location.getParent() : location;
            if (dir != null) {
                return dir;
            }
        } catch (URISyntaxException | RuntimeException ignored) {
            // Fall through to the working-directory fallback below (e.g. running from an IDE/gradlew run).
        }
        return Path.of(System.getProperty("user.dir"));
    }

    private void onSetupComplete(ServerSettings settings) {
        try {
            ctx.settingsService.save(settings);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save settings.json: " + e.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        showDashboard(settings);
    }

    private void showDashboard(ServerSettings settings) {
        if (dashboardPanel == null) {
            dashboardPanel = new DashboardPanel(ctx, settings, this::onReenterSettings);
            cards.add(dashboardPanel, CARD_DASHBOARD);
        } else {
            dashboardPanel.updateSettings(settings);
        }
        cardLayout.show(cards, CARD_DASHBOARD);
    }

    private void onReenterSettings() {
        cards.remove(wizardPanel);
        wizardPanel = new SetupWizardPanel(ctx, this::onSetupComplete);
        cards.add(wizardPanel, CARD_WIZARD);
        cardLayout.show(cards, CARD_WIZARD);
    }
}
