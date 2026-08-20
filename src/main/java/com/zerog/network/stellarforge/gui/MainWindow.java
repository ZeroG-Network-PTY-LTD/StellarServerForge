package com.zerog.network.stellarforge.gui;

import com.zerog.network.stellarforge.config.SecureConfig;
import com.zerog.network.stellarforge.gui.components.ProfileListCellRenderer;
import com.zerog.network.stellarforge.gui.components.ToastNotification;
import com.zerog.network.stellarforge.model.ServerConfig;
import com.zerog.network.stellarforge.model.ServerProfile;
import com.zerog.network.stellarforge.utils.ConnectionPool;
import com.zerog.network.stellarforge.utils.DatabaseManager;
import com.zerog.network.stellarforge.utils.FirstRunDetector;
import com.zerog.network.stellarforge.utils.ProfileManager;
import com.zerog.network.stellarforge.utils.ProgressManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Main application window for Stellar Server Forge – v2.0
 * Features: profile management, dashboard, keyboard shortcuts, toast alerts.
 */
public class MainWindow extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);

    private ServerConfig serverConfig;
    private ServerProfile currentProfile;
    private JComboBox<ServerProfile> profileSelector;
    private JLabel profileStatusLabel;
    private DashboardPanel dashboardPanel;

    public MainWindow() {
        // Initialise DB early so it's available throughout the session
        DatabaseManager.getInstance().open();

        // Persist every completed background operation to the database
        ProgressManager.getInstance().addListener(op -> {
            if (op.isCompleted()) {
                DatabaseManager.getInstance().saveOperation(op);
            }
        });

        if (FirstRunDetector.isFirstRun()) {
            showSetupWizard();
        }

        initializeConfig();
        initializeComponents();
        registerKeyboardShortcuts();

        updateTitle();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                DatabaseManager.getInstance().close();
                ConnectionPool.shutdown();
            }
        });
        setSize(960, 680);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);

        logger.info("Main window initialized (v2.0)");
    }

    // ── Config & wizard ───────────────────────────────────────────────────────

    private void initializeConfig() {
        ProfileManager pm = ProfileManager.getInstance();
        currentProfile = pm.getActiveProfile();

        if (currentProfile == null) {
            currentProfile = pm.createDefaultProfile();
            pm.setActiveProfile(currentProfile);
        }

        serverConfig = (currentProfile != null && currentProfile.getConfig() != null)
                ? currentProfile.getConfig()
                : new ServerConfig();

        logger.info("Active profile: {}",
                currentProfile != null ? currentProfile.getProfileName() : "none");
    }

    private void showSetupWizard() {
        // Run after construction so the parent window exists
        SwingUtilities.invokeLater(() -> {
            SetupWizardDialog wizard = new SetupWizardDialog(MainWindow.this);
            wizard.setVisible(true);
            if (wizard.isSetupCompleted()) {
                initializeConfig();
            }
        });
    }

    // ── UI assembly ───────────────────────────────────────────────────────────

    private void initializeComponents() {
        setLayout(new BorderLayout(0, 0));
        add(createHeaderPanel(),  BorderLayout.NORTH);
        add(createDashboard(),    BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 6));
        panel.setBackground(new Color(28, 28, 33));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        // Title
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel("🚀 " + SecureConfig.getInstance().getAppName());
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel(SecureConfig.getInstance().getOrganization()
                + "  •  v" + SecureConfig.getInstance().getAppVersion());
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(150, 150, 165));

        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.CENTER);

        // Profile bar (right side)
        JPanel profileBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        profileBar.setOpaque(false);

        JLabel profileLbl = new JLabel("Profile:");
        profileLbl.setForeground(new Color(150, 150, 165));
        profileLbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        profileSelector = new JComboBox<>();
        profileSelector.setRenderer(new ProfileListCellRenderer());
        profileSelector.setPreferredSize(new Dimension(210, 28));
        profileSelector.setBackground(new Color(45, 45, 52));
        profileSelector.setForeground(Color.WHITE);
        profileSelector.addActionListener(e -> switchProfile());
        loadProfilesIntoSelector();

        JButton newBtn = headerButton("+ New", new Color(52, 73, 94));
        newBtn.setToolTipText("Create new profile  [Ctrl+N]");
        newBtn.addActionListener(e -> createNewProfile());

        JButton manageBtn = headerButton("⚙ Manage", new Color(44, 62, 80));
        manageBtn.setToolTipText("Manage profiles");
        manageBtn.addActionListener(e -> manageProfiles());

        profileStatusLabel = new JLabel();
        profileStatusLabel.setForeground(new Color(120, 120, 135));
        profileStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        updateProfileStatus();

        profileBar.add(profileLbl);
        profileBar.add(profileSelector);
        profileBar.add(newBtn);
        profileBar.add(manageBtn);

        panel.add(titlePanel, BorderLayout.CENTER);
        panel.add(profileBar, BorderLayout.EAST);
        panel.add(profileStatusLabel, BorderLayout.SOUTH);

        return panel;
    }

    private DashboardPanel createDashboard() {
        dashboardPanel = new DashboardPanel(new DashboardPanel.DashboardListener() {
            @Override public void onLaunchServer(ServerProfile profile) {
                switchToProfile(profile);
                openServerLauncher();
            }
            @Override public void onInstallMods(ServerProfile profile) {
                switchToProfile(profile);
                openModInstaller();
            }
            @Override public void onConfigureServer(ServerProfile profile) {
                switchToProfile(profile);
                showConfigurationDialog();
            }
            @Override public void onNewProfile() {
                createNewProfile();
            }
            @Override public void onBackup(ServerProfile profile) {
                switchToProfile(profile);
                openBackupDialog();
            }
        });
        return dashboardPanel;
    }

    // ── Keyboard shortcuts ────────────────────────────────────────────────────

    private void registerKeyboardShortcuts() {
        JRootPane root = getRootPane();
        InputMap  im   = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am   = root.getActionMap();

        bindKey(im, am, "launch",    KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK,  e -> openServerLauncher());
        bindKey(im, am, "mods",      KeyEvent.VK_M, KeyEvent.CTRL_DOWN_MASK,  e -> openModInstaller());
        bindKey(im, am, "configure", KeyEvent.VK_COMMA, KeyEvent.CTRL_DOWN_MASK, e -> showConfigurationDialog());
        bindKey(im, am, "save",      KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK,  e -> saveCurrentProfile());
        bindKey(im, am, "newProf",   KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK,  e -> createNewProfile());
        bindKey(im, am, "backup",    KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK,  e -> openBackupDialog());
        bindKey(im, am, "history",   KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK,  e -> openOperationHistory());
        bindKey(im, am, "help",      KeyEvent.VK_F1, 0,                        e -> showAboutDialog());
        bindKey(im, am, "refresh",   KeyEvent.VK_F5, 0,                        e -> {
            dashboardPanel.refreshRecentServers();
            ToastNotification.info(this, "Dashboard refreshed");
        });
    }

    private void bindKey(InputMap im, ActionMap am, String key, int keyCode, int mods,
                         java.util.function.Consumer<ActionEvent> action) {
        im.put(KeyStroke.getKeyStroke(keyCode, mods), key);
        am.put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.accept(e); }
        });
    }

    // ── Profile helpers ───────────────────────────────────────────────────────

    private void loadProfilesIntoSelector() {
        profileSelector.removeAllItems();
        for (ServerProfile p : ProfileManager.getInstance().getAllProfiles()) {
            profileSelector.addItem(p);
        }
        if (currentProfile != null) {
            profileSelector.setSelectedItem(currentProfile);
        }
    }

    private void switchProfile() {
        ServerProfile sel = (ServerProfile) profileSelector.getSelectedItem();
        if (sel != null && !sel.equals(currentProfile)) {
            switchToProfile(sel);
        }
    }

    /** Internal – switch to a profile without showing the combo-selector dialog */
    private void switchToProfile(ServerProfile profile) {
        if (profile == null || profile.equals(currentProfile)) return;
        currentProfile = profile;
        serverConfig   = profile.getConfig();
        ProfileManager.getInstance().setActiveProfile(profile);
        profileSelector.setSelectedItem(profile);
        updateProfileStatus();
        updateTitle();
        dashboardPanel.refreshRecentServers();
        logger.info("Switched to profile: {}", profile.getProfileName());
    }

    private void createNewProfile() {
        String name = JOptionPane.showInputDialog(this,
                "Enter a name for the new profile:", "New Profile",
                JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        ServerConfig cfg = new ServerConfig();
        cfg.setServerName(name.trim());
        cfg.setServerPath("server-" + name.trim().toLowerCase().replaceAll("[^a-z0-9]", "-"));

        ServerProfile p = new ServerProfile(name.trim(), cfg);
        if (ProfileManager.getInstance().saveProfile(p)) {
            loadProfilesIntoSelector();
            switchToProfile(p);
            ToastNotification.success(this, "Profile \"" + name.trim() + "\" created!");
            logger.info("Created profile: {}", name.trim());
            showConfigurationDialog();
        }
    }

    private void manageProfiles() {
        ProfileManager pm = ProfileManager.getInstance();
        List<ServerProfile> profiles = pm.getAllProfiles();
        if (profiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No profiles found.", "Manage Profiles",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Build a simple dialog with a list
        JDialog dlg = new JDialog(this, "Manage Profiles", true);
        dlg.setSize(400, 320);
        dlg.setLocationRelativeTo(this);

        DefaultListModel<ServerProfile> model = new DefaultListModel<>();
        profiles.forEach(model::addElement);

        JList<ServerProfile> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new ProfileListCellRenderer());
        list.setSelectedValue(currentProfile, true);

        JButton renameBtn    = new JButton("Rename");
        JButton duplicateBtn = new JButton("Duplicate");
        JButton deleteBtn    = new JButton("Delete");
        JButton closeBtn     = new JButton("Close");

        renameBtn.addActionListener(e -> {
            ServerProfile sel = list.getSelectedValue();
            if (sel == null) return;
            String n = JOptionPane.showInputDialog(dlg, "New name:", sel.getProfileName());
            if (n != null && !n.trim().isEmpty()) {
                sel.setProfileName(n.trim());
                pm.saveProfile(sel);
                list.repaint();
                loadProfilesIntoSelector();
                updateTitle();
            }
        });

        duplicateBtn.addActionListener(e -> {
            ServerProfile sel = list.getSelectedValue();
            if (sel == null) return;
            String n = JOptionPane.showInputDialog(dlg, "Duplicate name:", sel.getProfileName() + " (Copy)");
            if (n != null && !n.trim().isEmpty()) {
                ServerProfile dup = pm.duplicateProfile(sel, n.trim());
                if (dup != null) {
                    model.addElement(dup);
                    loadProfilesIntoSelector();
                    ToastNotification.success(this, "Duplicated as \"" + n.trim() + "\"");
                }
            }
        });

        deleteBtn.addActionListener(e -> {
            ServerProfile sel = list.getSelectedValue();
            if (sel == null) return;
            int confirm = JOptionPane.showConfirmDialog(dlg,
                    "Delete \"" + sel.getProfileName() + "\"?", "Confirm Delete",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                pm.deleteProfile(sel.getProfileId());
                model.removeElement(sel);
                loadProfilesIntoSelector();
                if (sel.equals(currentProfile)) {
                    List<ServerProfile> remaining = pm.getAllProfiles();
                    if (!remaining.isEmpty()) switchToProfile(remaining.get(0));
                    else { currentProfile = pm.createDefaultProfile(); initializeConfig(); }
                }
                ToastNotification.warning(this, "Profile \"" + sel.getProfileName() + "\" deleted.");
            }
        });

        closeBtn.addActionListener(e -> dlg.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnPanel.add(renameBtn);
        btnPanel.add(duplicateBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(closeBtn);

        dlg.add(new JScrollPane(list), BorderLayout.CENTER);
        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);

        dashboardPanel.refreshRecentServers();
    }

    private void updateProfileStatus() {
        if (currentProfile != null && serverConfig != null) {
            profileStatusLabel.setText(
                    currentProfile.getProfileName() + "  •  MC "
                    + serverConfig.getMinecraftVersion() + "  •  "
                    + serverConfig.getModLoader().getDisplayName()
                    + "  •  " + serverConfig.getMaxRamGb() + " GB RAM");
        }
    }

    private void updateTitle() {
        setTitle(SecureConfig.getInstance().getAppName()
                + " v" + SecureConfig.getInstance().getAppVersion()
                + " — " + SecureConfig.getInstance().getOrganization()
                + (currentProfile != null ? "   [" + currentProfile.getProfileName() + "]" : ""));
    }

    // ── Dialog launchers ──────────────────────────────────────────────────────

    private void showConfigurationDialog() {
        try {
            ServerConfigDialog dlg = new ServerConfigDialog(this, serverConfig);
            dlg.setVisible(true);
            if (dlg.isConfigurationChanged()) {
                if (currentProfile != null) ProfileManager.getInstance().saveProfile(currentProfile);
                updateTitle();
                updateProfileStatus();
                dashboardPanel.refreshRecentServers();
                ToastNotification.success(this, "Configuration saved for \""
                        + serverConfig.getServerName() + "\"");
                logger.info("Config updated: {}", serverConfig.getServerName());
            }
        } catch (Exception e) {
            logger.error("Error opening config dialog", e);
            showError("Configuration Error", e);
        }
    }

    private void openModInstaller() {
        if (serverConfig.getServerPath() == null || serverConfig.getServerPath().isEmpty()) {
            if (JOptionPane.showConfirmDialog(this,
                    "No server path set. Use default 'server' directory?",
                    "Server Path Required", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                serverConfig.setServerPath("server");
            } else return;
        }
        try {
            new ModInstallerDialog(this, serverConfig).setVisible(true);
        } catch (Exception e) {
            logger.error("Error opening mod installer", e);
            showError("Mod Installer Error", e);
        }
    }

    private void openServerLauncher() {
        if (serverConfig.getServerPath() == null || serverConfig.getServerPath().isEmpty()) {
            if (JOptionPane.showConfirmDialog(this,
                    "No server path set. Use default 'server' directory?",
                    "Server Path Required", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                serverConfig.setServerPath("server");
            } else return;
        }
        try {
            new ServerLauncherDialog(this, serverConfig).setVisible(true);
        } catch (Exception e) {
            logger.error("Error opening server launcher", e);
            showError("Launcher Error", e);
        }
    }

    private void saveCurrentProfile() {
        if (currentProfile != null) {
            ProfileManager.getInstance().saveProfile(currentProfile);
            ToastNotification.success(this, "Profile \"" + currentProfile.getProfileName() + "\" saved");
        }
    }

    private void openBackupDialog() {
        try {
            BackupDialog dlg = new BackupDialog(this, serverConfig);
            dlg.setVisible(true);
        } catch (Exception e) {
            logger.error("Error opening backup dialog", e);
            showError("Backup Error", e);
        }
    }

    private void openOperationHistory() {
        new OperationHistoryDialog(this).setVisible(true);
    }

    private void showAboutDialog() {
        String msg = "<html><div style='width:400px'>"
                + "<h3>🚀 " + SecureConfig.getInstance().getAppName() + "</h3>"
                + "<p><b>Version:</b> " + SecureConfig.getInstance().getAppVersion() + "</p>"
                + "<p><b>Organisation:</b> " + SecureConfig.getInstance().getOrganization() + "</p><br>"
                + "<b>Keyboard Shortcuts:</b><br>"
                + "<table>"
                + "<tr><td><tt>Ctrl+L</tt></td><td>Launch Server</td></tr>"
                + "<tr><td><tt>Ctrl+M</tt></td><td>Install Mods</td></tr>"
                + "<tr><td><tt>Ctrl+,</tt></td><td>Configure Server</td></tr>"
                + "<tr><td><tt>Ctrl+S</tt></td><td>Save Profile</td></tr>"
                + "<tr><td><tt>Ctrl+N</tt></td><td>New Profile</td></tr>"
                + "<tr><td><tt>Ctrl+B</tt></td><td>Backup Server</td></tr>"
                + "<tr><td><tt>Ctrl+H</tt></td><td>Operation History</td></tr>"
                + "<tr><td><tt>F1</tt></td><td>About</td></tr>"
                + "<tr><td><tt>F5</tt></td><td>Refresh Dashboard</td></tr>"
                + "</table>"
                + "</div></html>";
        JOptionPane.showMessageDialog(this, msg, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Shared error handler ──────────────────────────────────────────────────

    private void showError(String title, Exception e) {
        ToastNotification.error(this, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        SmartErrorDialog.show(this, title, e, null);
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private JButton headerButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btn.setPreferredSize(new Dimension(90, 28));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
