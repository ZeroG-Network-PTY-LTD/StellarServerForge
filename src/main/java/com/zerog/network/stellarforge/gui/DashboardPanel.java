package com.zerog.network.stellarforge.gui;

import com.zerog.network.stellarforge.config.SecureConfig;
import com.zerog.network.stellarforge.gui.components.ServerCard;
import com.zerog.network.stellarforge.gui.components.ToastNotification;
import com.zerog.network.stellarforge.model.ServerConfig;
import com.zerog.network.stellarforge.model.ServerProfile;
import com.zerog.network.stellarforge.utils.ProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Quick-Action Dashboard panel — replaces the basic button layout in the
 * main window with a card-based, information-rich landing page.
 */
public class DashboardPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DashboardPanel.class);

    public interface DashboardListener {
        void onLaunchServer(ServerProfile profile);
        void onInstallMods(ServerProfile profile);
        void onConfigureServer(ServerProfile profile);
        void onNewProfile();
        void onBackup(ServerProfile profile);
    }

    private final DashboardListener listener;
    private JPanel recentServersPanel;
    private JLabel cfStatusLabel;
    private JLabel mrStatusLabel;
    private JLabel javaStatusLabel;
    private JLabel diskStatusLabel;
    private JLabel memStatusLabel;

    public DashboardPanel(DashboardListener listener) {
        this.listener = listener;
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setBackground(new Color(35, 35, 40));

        add(buildCenterSection(), BorderLayout.CENTER);
        add(buildStatusPanel(),   BorderLayout.SOUTH);

        refreshRecentServers();
    }

    // ── Center (recent servers + quick actions) ───────────────────────────────

    private JPanel buildCenterSection() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 16, 0));
        panel.setOpaque(false);
        panel.add(buildRecentServersPanel());
        panel.add(buildActionsColumn());
        return panel;
    }

    // ── Recent servers ────────────────────────────────────────────────────────

    private JPanel buildRecentServersPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);

        JLabel header = sectionHeader("⏱ Recent Servers");
        wrapper.add(header, BorderLayout.NORTH);

        recentServersPanel = new JPanel();
        recentServersPanel.setLayout(new BoxLayout(recentServersPanel, BoxLayout.Y_AXIS));
        recentServersPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(recentServersPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        wrapper.add(scroll, BorderLayout.CENTER);

        // New profile button at bottom
        JButton newBtn = actionButton("+ Create New Server", new Color(52, 73, 94));
        newBtn.addActionListener(e -> listener.onNewProfile());
        wrapper.add(newBtn, BorderLayout.SOUTH);

        return wrapper;
    }

    /** Rebuilds the recent servers cards. Call after profile changes. */
    public void refreshRecentServers() {
        recentServersPanel.removeAll();

        List<ServerProfile> profiles = ProfileManager.getInstance().getRecentProfiles(4);

        if (profiles.isEmpty()) {
            JLabel empty = new JLabel("<html><center><i>No servers yet.<br>Create one to get started!</i></center></html>");
            empty.setForeground(new Color(130, 130, 140));
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            empty.setBorder(new EmptyBorder(20, 0, 20, 0));
            recentServersPanel.add(empty);
        } else {
            for (ServerProfile p : profiles) {
                ServerCard card = new ServerCard(p, new ServerCard.CardAction() {
                    @Override public void onLaunch(ServerProfile profile) {
                        listener.onLaunchServer(profile);
                    }
                    @Override public void onConfigure(ServerProfile profile) {
                        listener.onConfigureServer(profile);
                    }
                    @Override public void onBackup(ServerProfile profile) {
                        listener.onBackup(profile);
                    }
                    @Override public void onDuplicate(ServerProfile profile) {
                        String n = JOptionPane.showInputDialog(
                                DashboardPanel.this,
                                "New profile name:",
                                profile.getProfileName() + " (Copy)");
                        if (n != null && !n.trim().isEmpty()) {
                            ServerProfile dup = ProfileManager.getInstance()
                                    .duplicateProfile(profile, n.trim());
                            if (dup != null) {
                                ToastNotification.success(
                                        SwingUtilities.getWindowAncestor(DashboardPanel.this),
                                        "Duplicated as \"" + n.trim() + "\"");
                                refreshRecentServers();
                            }
                        }
                    }
                    @Override public void onToggleFavorite(ServerProfile profile) {
                        profile.setFavorite(!profile.isFavorite());
                        ProfileManager.getInstance().saveProfile(profile);
                        refreshRecentServers();
                        ToastNotification.info(
                                SwingUtilities.getWindowAncestor(DashboardPanel.this),
                                profile.isFavorite()
                                        ? "\"" + profile.getProfileName() + "\" added to favourites"
                                        : "\"" + profile.getProfileName() + "\" removed from favourites");
                    }
                    @Override public void onDelete(ServerProfile profile) {
                        int ok = JOptionPane.showConfirmDialog(
                                DashboardPanel.this,
                                "Delete profile \"" + profile.getProfileName() + "\"?\nThis cannot be undone.",
                                "Confirm Delete",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (ok == JOptionPane.YES_OPTION) {
                            ProfileManager.getInstance().deleteProfile(profile.getProfileId());
                            refreshRecentServers();
                            ToastNotification.warning(
                                    SwingUtilities.getWindowAncestor(DashboardPanel.this),
                                    "Profile \"" + profile.getProfileName() + "\" deleted.");
                        }
                    }
                });
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
                recentServersPanel.add(card);
                recentServersPanel.add(Box.createVerticalStrut(8));
            }
        }

        recentServersPanel.revalidate();
        recentServersPanel.repaint();
    }

    // ── Right column: quick actions + templates ───────────────────────────────

    private JPanel buildActionsColumn() {
        JPanel col = new JPanel(new BorderLayout(0, 16));
        col.setOpaque(false);
        col.add(buildQuickActionsPanel(),  BorderLayout.NORTH);
        col.add(buildTemplatesPanel(),     BorderLayout.CENTER);
        return col;
    }

    private JPanel buildQuickActionsPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);
        wrapper.add(sectionHeader("⚡ Quick Actions"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 8, 8));
        grid.setOpaque(false);

        JButton launchBtn = actionButton("🚀 Launch Server",  new Color(39, 174, 96));
        JButton modsBtn   = actionButton("📦 Install Mods",   new Color(142, 68, 173));
        JButton configBtn = actionButton("⚙️ Configure",       new Color(52, 152, 219));
        JButton backupBtn = actionButton("💾 Backup World",    new Color(230, 126, 34));

        launchBtn.addActionListener(e -> {
            ServerProfile active = ProfileManager.getInstance().getActiveProfile();
            if (active != null) listener.onLaunchServer(active);
        });
        modsBtn.addActionListener(e -> {
            ServerProfile active = ProfileManager.getInstance().getActiveProfile();
            if (active != null) listener.onInstallMods(active);
        });
        configBtn.addActionListener(e -> {
            ServerProfile active = ProfileManager.getInstance().getActiveProfile();
            if (active != null) listener.onConfigureServer(active);
        });
        backupBtn.addActionListener(e -> {
            ServerProfile active = ProfileManager.getInstance().getActiveProfile();
            if (active != null) listener.onBackup(active);
        });

        grid.add(launchBtn);
        grid.add(modsBtn);
        grid.add(configBtn);
        grid.add(backupBtn);

        wrapper.add(grid, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildTemplatesPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);
        wrapper.add(sectionHeader("📋 Quick Templates"), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        list.add(templateRow("⚒  Forge Modded Server", "1.20.1", ServerConfig.ModLoader.FORGE,  4));
        list.add(Box.createVerticalStrut(6));
        list.add(templateRow("🪡  Fabric Lightweight",  "1.20.6", ServerConfig.ModLoader.FABRIC,  2));
        list.add(Box.createVerticalStrut(6));
        list.add(templateRow("🌿  NeoForge Modern",     "1.20.6", ServerConfig.ModLoader.NEOFORGE, 4));
        list.add(Box.createVerticalStrut(6));
        list.add(templateRow("🧵  Quilt Experimental",  "1.20.4", ServerConfig.ModLoader.QUILT,   2));

        wrapper.add(list, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel templateRow(String label, String mcVer, ServerConfig.ModLoader loader, int ram) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(new Color(45, 45, 52));
        row.setBorder(new EmptyBorder(8, 12, 8, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JLabel lbl = new JLabel(label);
        lbl.setForeground(new Color(200, 200, 210));
        lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        JButton createBtn = new JButton("Create");
        createBtn.setBackground(new Color(52, 73, 94));
        createBtn.setForeground(Color.WHITE);
        createBtn.setFocusPainted(false);
        createBtn.setBorderPainted(false);
        createBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        createBtn.setPreferredSize(new Dimension(60, 26));
        createBtn.addActionListener(e -> createFromTemplate(label, mcVer, loader, ram));

        row.add(lbl, BorderLayout.CENTER);
        row.add(createBtn, BorderLayout.EAST);
        return row;
    }

    private void createFromTemplate(String name, String mcVer, ServerConfig.ModLoader loader, int ram) {
        String profileName = JOptionPane.showInputDialog(
                this, "Server name:", name, JOptionPane.QUESTION_MESSAGE);
        if (profileName == null || profileName.trim().isEmpty()) return;

        ServerConfig cfg = new ServerConfig();
        cfg.setServerName(profileName.trim());
        cfg.setServerPath("server-" + profileName.trim().toLowerCase().replaceAll("[^a-z0-9]", "-"));
        cfg.setMinecraftVersion(mcVer);
        cfg.setModLoader(loader);
        cfg.setMaxRamGb(ram);
        cfg.setPort(25565);

        ServerProfile profile = new ServerProfile(profileName.trim(), cfg);
        profile.setDescription("Created from template: " + name);

        ProfileManager.getInstance().saveProfile(profile);
        ProfileManager.getInstance().setActiveProfile(profile);
        refreshRecentServers();

        ToastNotification.success(SwingUtilities.getWindowAncestor(this),
                "Profile \"" + profileName + "\" created!");

        listener.onConfigureServer(profile);
        logger.info("Created template profile: {}", profileName);
    }

    // ── Status bar ────────────────────────────────────────────────────────────

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 6));
        panel.setBackground(new Color(28, 28, 33));
        panel.setBorder(new EmptyBorder(4, 8, 4, 8));

        cfStatusLabel   = statusChip("CurseForge");
        mrStatusLabel   = statusChip("Modrinth");
        javaStatusLabel = statusChip("Java");
        diskStatusLabel = statusChip("Disk");
        memStatusLabel  = statusChip("Memory");

        panel.add(cfStatusLabel);
        panel.add(mrStatusLabel);
        panel.add(javaStatusLabel);
        panel.add(diskStatusLabel);
        panel.add(memStatusLabel);

        // Refresh status every 10 seconds
        Timer refreshTimer = new Timer(10_000, e -> refreshStatus());
        refreshTimer.start();
        refreshStatus();

        return panel;
    }

    private JLabel statusChip(String name) {
        JLabel lbl = new JLabel(name + ": …");
        lbl.setForeground(new Color(150, 150, 160));
        lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        return lbl;
    }

    private void refreshStatus() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            boolean cf, mr, java;
            long freeDisk; long freeMemMb;

            @Override
            protected Void doInBackground() {
                cf   = SecureConfig.getInstance().isCurseForgeEnabled();
                mr   = SecureConfig.getInstance().isModrinthEnabled();
                java = detectJava();
                try {
                    freeDisk  = Files.getFileStore(Paths.get(".")).getUsableSpace() / (1024 * 1024 * 1024);
                } catch (Exception e) { freeDisk = -1; }
                freeMemMb = Runtime.getRuntime().freeMemory() / (1024 * 1024);
                return null;
            }

            @Override
            protected void done() {
                cfStatusLabel.setText("CurseForge: " + (cf ? "✅" : "❌"));
                cfStatusLabel.setForeground(cf ? new Color(39, 174, 96) : new Color(192, 57, 43));

                mrStatusLabel.setText("Modrinth: " + (mr ? "✅" : "⚠️"));
                mrStatusLabel.setForeground(mr ? new Color(39, 174, 96) : new Color(230, 126, 34));

                javaStatusLabel.setText("Java: " + (java ? "✅" : "❌ Not found"));
                javaStatusLabel.setForeground(java ? new Color(39, 174, 96) : new Color(192, 57, 43));

                if (freeDisk >= 0) {
                    diskStatusLabel.setText("Disk: " + freeDisk + " GB free");
                    diskStatusLabel.setForeground(freeDisk < 2
                            ? new Color(192, 57, 43)
                            : new Color(150, 150, 160));
                }

                memStatusLabel.setText("JVM Mem: " + freeMemMb + " MB free");
            }
        };
        worker.execute();
    }

    private boolean detectJava() {
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-version");
            pb.redirectErrorStream(true);
            return pb.start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JLabel sectionHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(new Color(200, 200, 215));
        lbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        lbl.setBorder(new EmptyBorder(0, 0, 6, 0));
        return lbl;
    }

    private JButton actionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

}

