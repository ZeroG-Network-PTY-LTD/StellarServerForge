package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.gui.theme.StellarButton;
import com.zerog.stellarserverforge.gui.theme.StellarLabels;
import com.zerog.stellarserverforge.gui.theme.StellarPanel;
import com.zerog.stellarserverforge.gui.theme.StellarTheme;
import com.zerog.stellarserverforge.javamanaged.JavaProvisioningService;
import com.zerog.stellarserverforge.launch.LaunchArgsBuilder;
import com.zerog.stellarserverforge.launch.LogDiagnostics;
import com.zerog.stellarserverforge.launch.ServerProcessRunner;
import com.zerog.stellarserverforge.model.JavaOverrideMode;
import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.model.ServerSettings;
import com.zerog.stellarserverforge.modloader.ModLoaderLaunchLine;
import com.zerog.stellarserverforge.mojang.MojangManifestService;
import com.zerog.stellarserverforge.settings.EulaService;
import com.zerog.stellarserverforge.settings.ServerPropertiesService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The main-menu equivalent (spec §3.1): current settings summary, Launch/Stop, and a live
 * console output view.
 */
public class DashboardPanel extends JPanel {

    private final AppContext ctx;
    private final Runnable onOpenSettingsScreen;
    private final Runnable onOpenUtilitiesScreen;
    private final Runnable onOpenModsScreen;
    private final Runnable onOpenZeroGModsScreen;

    private ServerSettings settings;
    private final ServerProcessRunner runner;

    private final JLabel mcVersionLabel = StellarLabels.value("");
    private final JLabel modLoaderLabel = StellarLabels.value("");
    private final JLabel javaVersionLabel = StellarLabels.value("");
    private final JLabel ramLabel = StellarLabels.value("");
    private final JLabel portLabel = StellarLabels.value("");
    private final JLabel javaOverrideLabel = StellarLabels.value("");
    private final com.zerog.stellarserverforge.gui.theme.StellarTag statusTag =
            new com.zerog.stellarserverforge.gui.theme.StellarTag("Idle",
                    com.zerog.stellarserverforge.gui.theme.StellarTag.Variant.NEUTRAL, StellarTheme.STATUS_IDLE);

    private final StellarButton launchButton = new StellarButton("Launch server", StellarButton.Variant.PRIMARY);
    private final StellarButton stopButton = new StellarButton("Stop server", StellarButton.Variant.DANGER);
    private final StellarButton restartButton = new StellarButton("Restart server", StellarButton.Variant.SECONDARY);
    private final StellarButton modsButton = new StellarButton("Mods", StellarButton.Variant.SECONDARY);
    private final StellarButton utilitiesButton = new StellarButton("Utilities", StellarButton.Variant.SECONDARY);
    private final StellarButton curseForgeButton = new StellarButton("Import CurseForge profile", StellarButton.Variant.SECONDARY);
    private final StellarButton zeroGModsButton = new StellarButton("ZeroG Network mods", StellarButton.Variant.SECONDARY);
    private final JCheckBox autoRestartCheckbox = new JCheckBox("Auto-restart on crash (up to 5x)");

    private final JTextArea console = new JTextArea();
    private volatile String lastResolvedJavaCommand;
    private volatile boolean stopRequested;
    private volatile boolean restartRequested;
    private int restartCount;

    public DashboardPanel(AppContext ctx, ServerSettings settings, Runnable onOpenSettingsScreen,
                           Runnable onOpenUtilitiesScreen, Runnable onOpenModsScreen, Runnable onOpenZeroGModsScreen) {
        this.ctx = ctx;
        this.settings = settings;
        this.onOpenSettingsScreen = onOpenSettingsScreen;
        this.onOpenUtilitiesScreen = onOpenUtilitiesScreen;
        this.onOpenModsScreen = onOpenModsScreen;
        this.onOpenZeroGModsScreen = onOpenZeroGModsScreen;
        this.runner = new ServerProcessRunner(ctx.serverDir);

        setOpaque(false);
        setLayout(new BorderLayout(0, StellarTheme.SPACE_17));
        setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_22, StellarTheme.SPACE_22,
                StellarTheme.SPACE_22, StellarTheme.SPACE_22));

        JPanel north = new JPanel(new BorderLayout(0, StellarTheme.SPACE_17));
        north.setOpaque(false);
        north.add(buildNavBar(), BorderLayout.NORTH);
        north.add(buildHeader(), BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(buildConsole(), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, StellarTheme.SPACE_8));
        south.setOpaque(false);
        south.add(buildFooter(), BorderLayout.NORTH);
        south.add(buildLinkBar(), BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        refreshLabels();
        stopButton.setEnabled(false);
        restartButton.setEnabled(false);
    }

    private JComponent buildNavBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, StellarTheme.SPACE_11, 0),
                BorderFactory.createMatteBorder(0, 0, 1, 0, StellarTheme.NEUTRAL_800)));

        JLabel wordmark = new JLabel("StellarServerForge");
        wordmark.setFont(StellarTheme.FONT_HEADING);
        wordmark.setForeground(StellarTheme.TEXT_PRIMARY);
        wordmark.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, StellarTheme.SPACE_11));

        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_6, 0));
        links.setOpaque(false);
        links.add(new com.zerog.stellarserverforge.gui.theme.StellarNavLink(
                "Mission Control", true, null));
        links.add(new com.zerog.stellarserverforge.gui.theme.StellarNavLink(
                "Mods", false, onOpenModsScreen));
        links.add(new com.zerog.stellarserverforge.gui.theme.StellarNavLink(
                "Utilities", false, onOpenUtilitiesScreen));
        links.add(new com.zerog.stellarserverforge.gui.theme.StellarNavLink(
                "Settings", false, onOpenSettingsScreen));
        links.add(new com.zerog.stellarserverforge.gui.theme.StellarNavLink(
                "Changelog", false, this::onOpenChangelog));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(wordmark);
        left.add(links);

        bar.add(left, BorderLayout.WEST);
        bar.add(statusTag, BorderLayout.EAST);
        return bar;
    }

    private JComponent buildHeader() {
        StellarPanel card = new StellarPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_17, StellarTheme.SPACE_17,
                StellarTheme.SPACE_17, StellarTheme.SPACE_17));

        JPanel titleRow = new JPanel();
        titleRow.setOpaque(false);
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.Y_AXIS));
        titleRow.add(StellarLabels.kicker("Working directory"));
        JLabel dirLabel = new JLabel(ctx.serverDir.toString());
        dirLabel.setFont(StellarTheme.FONT_MONO);
        dirLabel.setForeground(StellarTheme.TEXT_PRIMARY);
        titleRow.add(dirLabel);
        card.add(titleRow, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 3, 18, 12));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        addStat(grid, "Minecraft", mcVersionLabel);
        addStat(grid, "Modloader", modLoaderLabel);
        addStat(grid, "Java", javaVersionLabel);
        addStat(grid, "Max RAM", ramLabel);
        addStat(grid, "Port", portLabel);
        addStat(grid, "Java Mode", javaOverrideLabel);
        card.add(grid, BorderLayout.CENTER);

        return card;
    }

    private void addStat(JPanel grid, String label, JLabel valueLabel) {
        JPanel cell = new JPanel();
        cell.setOpaque(false);
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.add(StellarLabels.kicker(label));
        cell.add(valueLabel);
        grid.add(cell);
    }

    private JComponent buildConsole() {
        console.setEditable(false);
        console.setFont(StellarTheme.FONT_MONO);
        console.setBackground(StellarTheme.CONSOLE_BG);
        console.setForeground(StellarTheme.NEUTRAL_100);
        console.setCaretColor(StellarTheme.ACCENT);
        console.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_17, StellarTheme.SPACE_17,
                StellarTheme.SPACE_17, StellarTheme.SPACE_17));

        JScrollPane scroll = new JScrollPane(console);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(StellarTheme.CONSOLE_BG);

        StellarPanel card = new StellarPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JLabel heading = StellarLabels.heading("  Console");
        heading.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 0));
        card.add(heading, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildLinkBar() {
        String repo = "https://github.com/ZeroG-Network-PTY-LTD/StellarServerForge";

        JPanel wrap = new JPanel(new BorderLayout(0, StellarTheme.SPACE_3));
        wrap.setOpaque(false);
        wrap.add(bugReportLink(repo + "/issues/new"), BorderLayout.NORTH);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_8, 0));
        bar.setOpaque(false);
        wrap.add(bar, BorderLayout.SOUTH);
        bar.add(new com.zerog.stellarserverforge.gui.theme.StellarLinkIcon(
                com.zerog.stellarserverforge.gui.theme.StellarLinkIcon.Kind.DISCORD, "Discord",
                "https://discord.gg/dUGAQF2Mga"));
        bar.add(new com.zerog.stellarserverforge.gui.theme.StellarLinkIcon(
                com.zerog.stellarserverforge.gui.theme.StellarLinkIcon.Kind.GITHUB, "GitHub", repo));
        bar.add(new com.zerog.stellarserverforge.gui.theme.StellarLinkIcon(
                com.zerog.stellarserverforge.gui.theme.StellarLinkIcon.Kind.WIKI, "Wiki", repo + "/wiki"));
        bar.add(new com.zerog.stellarserverforge.gui.theme.StellarLinkIcon(
                com.zerog.stellarserverforge.gui.theme.StellarLinkIcon.Kind.ISSUES, "Issue tracker", repo + "/issues"));
        bar.add(new com.zerog.stellarserverforge.gui.theme.StellarLinkIcon(
                com.zerog.stellarserverforge.gui.theme.StellarLinkIcon.Kind.WEBSITE, "Website",
                "https://zerognetwork.co.za"));
        bar.add(new com.zerog.stellarserverforge.gui.theme.StellarLinkIcon(
                com.zerog.stellarserverforge.gui.theme.StellarLinkIcon.Kind.KOFI, "Ko-fi",
                "https://ko-fi.com/mrwhiteflamesyt"));
        return wrap;
    }

    private JComponent bugReportLink(String url) {
        JLabel link = new JLabel("<html><u>Found a bug? Report it here</u></html>");
        link.setFont(StellarTheme.FONT_CAPTION);
        link.setForeground(StellarTheme.TEXT_SECONDARY);
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                com.zerog.stellarserverforge.gui.theme.StellarLinkIcon.openUrl(url);
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                link.setForeground(StellarTheme.ACCENT_400);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                link.setForeground(StellarTheme.TEXT_SECONDARY);
            }
        });
        return link;
    }

    private JComponent buildFooter() {
        StellarPanel card = new StellarPanel(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JPanel primaryRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        primaryRow.setOpaque(false);
        primaryRow.add(launchButton);
        primaryRow.add(stopButton);
        primaryRow.add(restartButton);
        primaryRow.add(autoRestartCheckbox);
        autoRestartCheckbox.setOpaque(false);
        autoRestartCheckbox.setForeground(StellarTheme.TEXT_SECONDARY);
        autoRestartCheckbox.setFont(StellarTheme.FONT_BODY);

        JPanel toolRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolRow.setOpaque(false);
        toolRow.add(modsButton);
        toolRow.add(zeroGModsButton);
        toolRow.add(utilitiesButton);
        toolRow.add(curseForgeButton);

        card.add(primaryRow, BorderLayout.NORTH);
        card.add(toolRow, BorderLayout.SOUTH);

        launchButton.addActionListener(this::onLaunch);
        stopButton.addActionListener(this::onStop);
        restartButton.addActionListener(this::onRestart);
        modsButton.addActionListener(e -> onOpenModsScreen.run());
        utilitiesButton.addActionListener(e -> onOpenUtilitiesScreen.run());
        curseForgeButton.addActionListener(e -> onOpenCurseForgeImport());
        zeroGModsButton.addActionListener(e -> onOpenZeroGModsScreen.run());
        return card;
    }

    private Frame ownerFrame() {
        Window window = SwingUtilities.getWindowAncestor(this);
        return window instanceof Frame ? (Frame) window : null;
    }


    private void onOpenChangelog() {
        new ChangelogDialog(ownerFrame()).setVisible(true);
    }

    private void onOpenCurseForgeImport() {
        new CurseForgeImportDialog(ownerFrame(), ctx, imported -> {
            this.settings = imported;
            persistSettings();
            refreshLabels();
        }).setVisible(true);
    }

    /** Package-visible so SettingsPanel (a sibling screen, not a child of this one) can reuse the
     * same UPnP dialog flow instead of duplicating it. */
    void onOpenUpnp() {
        new UpnpDialog(ownerFrame(), ctx, settings, this::refreshLabels).setVisible(true);
    }

    /** Package-visible for the same reason as {@link #onOpenUpnp()} — reused from SettingsPanel. */
    void onCheckFirewall() {
        if (lastResolvedJavaCommand == null) {
            JOptionPane.showMessageDialog(this, "Launch the server at least once first, so the Java executable "
                    + "being used is known.", "Firewall check", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new SwingWorker<com.zerog.stellarserverforge.net_port.FirewallCheckService.Result, Void>() {
            @Override
            protected com.zerog.stellarserverforge.net_port.FirewallCheckService.Result doInBackground() {
                return ctx.firewallCheckService.check(settings.getPort(), lastResolvedJavaCommand);
            }

            @Override
            protected void done() {
                try {
                    var result = get();
                    JOptionPane.showMessageDialog(DashboardPanel.this, result.message(), "Firewall check",
                            result.pass() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardPanel.this, "Firewall check failed: " + ex.getMessage(),
                            "Firewall check", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /** Package-visible for the same reason as {@link #onOpenUpnp()} — reused from SettingsPanel. */
    void onChangeModLoaderVersion() {
        new ModLoaderVersionDialog(ownerFrame(), ctx, settings, this::refreshLabels).setVisible(true);
    }

    private void persistSettings() {
        try {
            ctx.settingsService.save(settings);
        } catch (java.io.IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save settings.json: " + e.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Package-visible so MainFrame can pass this as the "settings changed elsewhere" callback to
     * SettingsPanel, which mutates the same ServerSettings instance this panel displays. */
    void refreshLabels() {
        mcVersionLabel.setText(settings.getMinecraftVersion());
        modLoaderLabel.setText(settings.getModLoader() == ModLoader.VANILLA
                ? "VANILLA"
                : settings.getModLoader().name() + " " + settings.getModLoaderVersion());
        javaVersionLabel.setText(String.valueOf(settings.getJavaVersion()));
        ramLabel.setText(settings.getMaxRamGigs() + " GB");
        portLabel.setText(String.valueOf(settings.getPort()));
        javaOverrideLabel.setText(switch (settings.getJavaOverrideMode()) {
            case AUTOMATIC -> "Automatic";
            case SYSTEM_PATH -> "System PATH";
            case FORCE_MANAGED -> "Forced managed";
        });
    }

    private void appendConsole(String line) {
        SwingUtilities.invokeLater(() -> {
            console.append(line + System.lineSeparator());
            console.setCaretPosition(console.getDocument().getLength());
        });
    }

    private void setStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            statusTag.setText(status);
            String s = status.toLowerCase();
            Color dot;
            var variant = com.zerog.stellarserverforge.gui.theme.StellarTag.Variant.NEUTRAL;
            if (s.contains("running")) {
                dot = StellarTheme.STATUS_RUNNING;
                variant = com.zerog.stellarserverforge.gui.theme.StellarTag.Variant.ACCENT;
            } else if (s.contains("fail") || s.contains("error")) {
                dot = StellarTheme.STATUS_FAILED;
            } else if (s.equals("idle")) {
                dot = StellarTheme.STATUS_IDLE;
            } else {
                dot = StellarTheme.STATUS_WARNING;
            }
            statusTag.setDotColor(dot);
            statusTag.setVariant(variant);
        });
    }

    private void onLaunch(ActionEvent e) {
        launchButton.setEnabled(false);
        stopButton.setEnabled(true);
        restartButton.setEnabled(false);
        console.setText("");
        restartCount = 0;
        stopRequested = false;
        restartRequested = false;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    runLaunchSequence();
                } catch (LaunchAborted ignored) {
                    appendConsole("Launch cancelled.");
                    setStatus("Idle");
                } catch (Exception ex) {
                    appendConsole("ERROR: " + ex.getMessage());
                    setStatus("Failed");
                }
                return null;
            }

            @Override
            protected void done() {
                if (!runner.isRunning()) {
                    launchButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    restartButton.setEnabled(false);
                }
            }
        }.execute();
    }

    private static final class LaunchAborted extends RuntimeException {
    }

    /** Checked between each phase of the launch sequence (and before each auto-restart) so Stop
     * takes effect at the next opportunity even when clicked before the server process exists yet
     * to signal, or during the crash/auto-restart gap between two attempts. */
    private void checkStopRequested() {
        if (stopRequested) {
            throw new LaunchAborted();
        }
    }

    private void runLaunchSequence() throws Exception {
        setStatus("Resolving Java...");
        appendConsole("Resolving Java " + settings.getJavaVersion() + "...");
        JavaProvisioningService.ResolvedJava java = ctx.javaProvisioningService.resolve(
                settings.getJavaVersion(), settings.getJavaOverrideMode());
        appendConsole("Using Java from: " + java.source() + " (" + java.command() + ")");
        checkStopRequested();

        setStatus("Checking server.properties...");
        ServerPropertiesService.RepairResult repair =
                ctx.serverPropertiesService.ensureValidAndSynced(settings.getPort());
        if (repair.created()) {
            appendConsole("Created a default server.properties.");
        }
        if (repair.serverIpNonBlank()) {
            appendConsole("Note: server.properties has server-ip set to '" + repair.serverIpValue()
                    + "' — this restricts connections to that address. Clear it in server.properties if unintended.");
        }
        checkStopRequested();

        setStatus("Checking port availability...");
        if (!ctx.portConflictService.isPortFree(settings.getPort())) {
            handlePortConflict();
        }
        checkStopRequested();

        setStatus("Checking EULA...");
        if (!ctx.eulaService.isAccepted()) {
            handleEulaPrompt();
        }
        checkStopRequested();

        McVersion mc = McVersion.parse(settings.getMinecraftVersion());
        List<String> tailArgs = ensureModLoaderInstalledAndBuildTailArgs(mc, java.command());
        checkStopRequested();

        List<String> jvmArgs = LaunchArgsBuilder.buildJvmArgs(settings, settings.getJavaVersion());
        lastResolvedJavaCommand = java.command();
        java.nio.file.Path logFile = ctx.serverDir.resolve("logs").resolve("latest.log");

        while (true) {
            checkStopRequested();
            setStatus("Running");
            SwingUtilities.invokeLater(() -> {
                stopButton.setEnabled(true);
                restartButton.setEnabled(true);
            });
            appendConsole("Launching server...");
            runner.start(java.command(), jvmArgs, tailArgs, this::appendConsole);

            int exitCode = runner.waitForExit();
            appendConsole("Server process exited with code " + exitCode + ".");

            if (restartRequested) {
                restartRequested = false;
                appendConsole("Restarting server...");
                continue;
            }

            boolean graceful = LogDiagnostics.isGracefulStop(logFile);
            if (!graceful && !stopRequested && autoRestartCheckbox.isSelected() && restartCount < 5) {
                restartCount++;
                appendConsole("Server stopped unexpectedly — auto-restarting (attempt " + restartCount + "/5)...");
                continue;
            }
            if (!graceful && !stopRequested) {
                for (String guidance : LogDiagnostics.diagnose(logFile)) {
                    appendConsole(guidance);
                }
            }
            break;
        }

        setStatus("Idle");
        SwingUtilities.invokeLater(() -> {
            launchButton.setEnabled(true);
            stopButton.setEnabled(false);
            restartButton.setEnabled(false);
        });
    }

    private List<String> ensureModLoaderInstalledAndBuildTailArgs(McVersion mc, String javaCommand) throws Exception {
        ModLoader loader = settings.getModLoader();
        String loaderVersion = settings.getModLoaderVersion();

        switch (loader) {
            case VANILLA -> {
                setStatus("Locating server jar...");
                MojangManifestService.VersionEntry version = ctx.mojangManifestService.findVersion(settings.getMinecraftVersion());
                if (version == null) {
                    throw new IllegalStateException("Minecraft version " + settings.getMinecraftVersion() + " not found in the version list.");
                }
                if (!ctx.vanillaInstallService.serverJarPath(version.id()).toFile().exists()) {
                    appendConsole("Downloading vanilla server jar for " + version.id() + "...");
                }
                var jarPath = ctx.vanillaInstallService.ensureInstalled(version);
                appendConsole("Server jar ready: " + jarPath.getFileName());
            }
            case FORGE, NEOFORGE -> {
                setStatus("Checking " + loader + " installation...");
                if (ctx.forgeNeoForgeInstaller.isInstalled(loader, mc, loaderVersion)) {
                    appendConsole(loader + " " + loaderVersion + " is already installed.");
                } else {
                    appendConsole("Installing " + loader + " " + loaderVersion + " — this can take a while...");
                    setStatus("Installing " + loader + "...");
                    ctx.forgeNeoForgeInstaller.install(loader, mc, loaderVersion, javaCommand,
                            ctx.mojangManifestService, ctx.vanillaInstallService, this::appendConsole);
                    appendConsole(loader + " " + loaderVersion + " installed.");
                }
            }
            case FABRIC, QUILT -> {
                setStatus("Checking " + loader + " installation...");
                if (ctx.fabricQuiltInstaller.isInstalled(loader, mc, loaderVersion)) {
                    appendConsole(loader + " " + loaderVersion + " is already installed.");
                } else {
                    appendConsole("Installing " + loader + " " + loaderVersion + " — this can take a while...");
                    setStatus("Installing " + loader + "...");
                    ctx.fabricQuiltInstaller.install(loader, mc, loaderVersion, javaCommand, this::appendConsole);
                    appendConsole(loader + " " + loaderVersion + " installed.");
                }
            }
        }

        return ModLoaderLaunchLine.buildTailArgs(loader, mc, loaderVersion, ctx.serverDir);
    }

    private void handlePortConflict() throws Exception {
        var owner = ctx.portConflictService.findOwningProcess(settings.getPort());
        String message = "Port " + settings.getPort() + " is already in use"
                + owner.map(p -> " by process '" + p.name() + "' (PID " + p.pid() + ")").orElse("") + ".";

        AtomicInteger choice = new AtomicInteger(JOptionPane.CANCEL_OPTION);
        runOnEdtAndWait(() -> choice.set(JOptionPane.showConfirmDialog(
                this, message + "\nEnd that process and continue?", "Port in use",
                owner.isPresent() ? JOptionPane.YES_NO_OPTION : JOptionPane.DEFAULT_OPTION)));

        if (owner.isPresent() && choice.get() == JOptionPane.YES_OPTION) {
            ctx.portConflictService.killProcess(owner.get().pid());
            appendConsole("Ended process " + owner.get().pid() + " to free port " + settings.getPort() + ".");
        } else {
            throw new LaunchAborted();
        }
    }

    private void handleEulaPrompt() throws Exception {
        AtomicInteger choice = new AtomicInteger(JOptionPane.NO_OPTION);
        runOnEdtAndWait(() -> choice.set(JOptionPane.showConfirmDialog(
                this,
                "You must agree to the Mojang EULA to run this server.\n" + EulaService.MOJANG_EULA_URL
                        + "\n\nDo you agree?",
                "Minecraft EULA", JOptionPane.YES_NO_OPTION)));

        if (choice.get() != JOptionPane.YES_OPTION) {
            throw new LaunchAborted();
        }
        ctx.eulaService.accept();
        appendConsole("EULA accepted.");
    }

    private void runOnEdtAndWait(Runnable r) throws InterruptedException, InvocationTargetException {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeAndWait(r);
        }
    }

    private void onStop(ActionEvent e) {
        stopRequested = true;
        stopButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                if (runner.isRunning()) {
                    appendConsole("Stopping server...");
                    runner.stop(30);
                } else {
                    // Not running yet (still resolving Java, installing the modloader, etc.) — there's
                    // no process to signal, but runLaunchSequence checks stopRequested between each
                    // step and will abort at the next checkpoint instead of proceeding to launch or
                    // auto-restarting after a crash.
                    appendConsole("Stop requested — will cancel before the next step.");
                }
                return null;
            }
        }.execute();
    }

    private void onRestart(ActionEvent e) {
        if (!runner.isRunning()) {
            return;
        }
        restartRequested = true;
        restartButton.setEnabled(false);
        stopButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                appendConsole("Restart requested — stopping server...");
                runner.stop(30);
                return null;
            }
        }.execute();
    }

    public void updateSettings(ServerSettings settings) {
        this.settings = settings;
        refreshLabels();
    }
}
