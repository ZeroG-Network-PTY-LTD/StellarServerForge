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
    private static final String CARD_SETTINGS = "settings";
    private static final String CARD_UTILITIES = "utilities";
    private static final String CARD_MODS = "mods";
    private static final String CARD_ZEROG_MODS = "zerogMods";

    private final AppContext ctx;
    private final CardLayout cardLayout = new CardLayout();
    private final StarfieldPanel cards = new StarfieldPanel(new CardLayout());

    private DashboardPanel dashboardPanel;
    private SetupWizardPanel wizardPanel;
    private SettingsPanel settingsPanel;
    private UtilitiesPanel utilitiesPanel;
    private ModsPanel modsPanel;
    private ZeroGModsPanel zeroGModsPanel;
    private ServerSettings settings;

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
                ServerSettings loaded = ctx.settingsService.load();
                showDashboard(loaded);
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

    /** When actually packaged as a jar, the server directory tracks the jar's own folder rather
     * than the process's working directory — so settings.json/mods/etc. land next to the built jar
     * regardless of how it was launched (double-click, a shortcut with a different "Start in"
     * folder, a script elsewhere). When there's no real jar to locate (running via an IDE launcher
     * or {@code gradlew run}, where the code source is a classes/ directory, not a jar file), that
     * directory is build output — falls back to the working directory instead so a `gradlew clean`
     * can't wipe test settings/mods/world data. */
    private static Path resolveServerDir() {
        try {
            Path location = Path.of(MainFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(location) && location.getParent() != null) {
                return location.getParent();
            }
        } catch (URISyntaxException | RuntimeException ignored) {
            // Fall through to the working-directory fallback below.
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
        this.settings = settings;
        if (dashboardPanel == null) {
            dashboardPanel = new DashboardPanel(ctx, settings, this::showSettings, this::showUtilities,
                    this::showMods, this::showZeroGMods);
            cards.add(dashboardPanel, CARD_DASHBOARD);
        } else {
            dashboardPanel.updateSettings(settings);
        }
        cardLayout.show(cards, CARD_DASHBOARD);
    }

    /** Reached from the dashboard's "Settings" nav link — a real settings screen, not a re-run of
     * the whole setup wizard. Rebuilt fresh each visit (cheap; mirrors the wizard's own
     * rebuild-on-reentry pattern) so it always reflects the latest settings. */
    private void showSettings() {
        if (settingsPanel != null) {
            cards.remove(settingsPanel);
        }
        settingsPanel = new SettingsPanel(ctx, settings, this::backToDashboard, this::onReenterWizard,
                dashboardPanel::refreshLabels, dashboardPanel::onOpenUpnp, dashboardPanel::onCheckFirewall,
                dashboardPanel::onChangeModLoaderVersion);
        cards.add(settingsPanel, CARD_SETTINGS);
        cardLayout.show(cards, CARD_SETTINGS);
    }

    /** Reached from the dashboard's "Utilities" nav link/toolbar button — an in-window screen
     * instead of a separate modal dialog. */
    private void showUtilities() {
        if (utilitiesPanel != null) {
            cards.remove(utilitiesPanel);
        }
        utilitiesPanel = new UtilitiesPanel(ctx, settings, this::backToDashboard);
        cards.add(utilitiesPanel, CARD_UTILITIES);
        cardLayout.show(cards, CARD_UTILITIES);
    }

    /** Reached from the dashboard's "Mods" nav link/toolbar button — an in-window screen instead
     * of a separate modal dialog. */
    private void showMods() {
        if (modsPanel != null) {
            cards.remove(modsPanel);
        }
        modsPanel = new ModsPanel(ctx, settings, this::backToDashboard);
        cards.add(modsPanel, CARD_MODS);
        cardLayout.show(cards, CARD_MODS);
    }

    /** Reached from the dashboard's "ZeroG Network mods" toolbar button — an in-window screen
     * instead of a separate modal dialog. */
    private void showZeroGMods() {
        if (zeroGModsPanel != null) {
            cards.remove(zeroGModsPanel);
        }
        zeroGModsPanel = new ZeroGModsPanel(ctx, settings, this::backToDashboard);
        cards.add(zeroGModsPanel, CARD_ZEROG_MODS);
        cardLayout.show(cards, CARD_ZEROG_MODS);
    }

    private void backToDashboard() {
        dashboardPanel.refreshLabels();
        cardLayout.show(cards, CARD_DASHBOARD);
    }

    /** The actual "start over" action — now reached via a button inside the Settings screen,
     * rather than being what clicking "Settings" itself does. */
    private void onReenterWizard() {
        cards.remove(wizardPanel);
        wizardPanel = new SetupWizardPanel(ctx, this::onSetupComplete);
        cards.add(wizardPanel, CARD_WIZARD);
        cardLayout.show(cards, CARD_WIZARD);
    }
}
