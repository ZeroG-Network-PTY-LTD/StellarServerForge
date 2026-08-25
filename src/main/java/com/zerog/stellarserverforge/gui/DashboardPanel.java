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
    private final Runnable onReenterSettings;

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
    private final StellarButton settingsButton = new StellarButton("Re-run setup wizard", StellarButton.Variant.SECONDARY);
    private final StellarButton ramButton = new StellarButton("Change RAM", StellarButton.Variant.SECONDARY);
    private final StellarButton javaOverrideButton = new StellarButton("Cycle Java mode", StellarButton.Variant.SECONDARY);
    private final StellarButton modLoaderVersionButton = new StellarButton("Change modloader version", StellarButton.Variant.SECONDARY);
    private final StellarButton portButton = new StellarButton("Change port", StellarButton.Variant.SECONDARY);
    private final StellarButton upnpButton = new StellarButton("UPnP", StellarButton.Variant.SECONDARY);
    private final StellarButton firewallButton = new StellarButton("Check firewall", StellarButton.Variant.SECONDARY);
    private final StellarButton modsButton = new StellarButton("Mods", StellarButton.Variant.SECONDARY);
    private final StellarButton utilitiesButton = new StellarButton("Utilities", StellarButton.Variant.SECONDARY);
    private final StellarButton curseForgeButton = new StellarButton("Import CurseForge profile", StellarButton.Variant.SECONDARY);
    private final StellarButton zeroGModsButton = new StellarButton("ZeroG Network mods", StellarButton.Variant.SECONDARY);
    private final JCheckBox autoRestartCheckbox = new JCheckBox("Auto-restart on crash (up to 5x)");

    private final JTextArea console = new JTextArea();
    private volatile String lastResolvedJavaCommand;
    private volatile boolean stopRequested;
    private int restartCount;

    public DashboardPanel(AppContext ctx, ServerSettings settings, Runnable onReenterSettings) {
        this.ctx = ctx;
        this.settings = settings;
        this.onReenterSettings = onReenterSettings;
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
                "Mods", false, this::onOpenMods));
        links.add(new com.zerog.stellarserverforge.gui.theme.StellarNavLink(
                "Utilities", false, this::onOpenUtilities));
        links.add(new com.zerog.stellarserverforge.gui.theme.StellarNavLink(
                "Settings", false, onReenterSettings));

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
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_8, 0));
        bar.setOpaque(false);
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
        return bar;
    }

    private JComponent buildFooter() {
        StellarPanel card = new StellarPanel(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JPanel primaryRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        primaryRow.setOpaque(false);
        primaryRow.add(launchButton);
        primaryRow.add(stopButton);
        primaryRow.add(autoRestartCheckbox);
        autoRestartCheckbox.setOpaque(false);
        autoRestartCheckbox.setForeground(StellarTheme.TEXT_SECONDARY);
        autoRestartCheckbox.setFont(StellarTheme.FONT_BODY);

        JPanel toolRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolRow.setOpaque(false);
        toolRow.add(ramButton);
        toolRow.add(javaOverrideButton);
        toolRow.add(modLoaderVersionButton);
        toolRow.add(portButton);
        toolRow.add(upnpButton);
        toolRow.add(firewallButton);
        toolRow.add(modsButton);
        toolRow.add(zeroGModsButton);
        toolRow.add(utilitiesButton);
        toolRow.add(curseForgeButton);
        toolRow.add(settingsButton);

        card.add(primaryRow, BorderLayout.NORTH);
        card.add(toolRow, BorderLayout.SOUTH);

        launchButton.addActionListener(this::onLaunch);
        stopButton.addActionListener(this::onStop);
        settingsButton.addActionListener(e -> onReenterSettings.run());
        ramButton.addActionListener(e -> onChangeRam());
        javaOverrideButton.addActionListener(e -> onCycleJavaOverride());
        modLoaderVersionButton.addActionListener(e -> onChangeModLoaderVersion());
        portButton.addActionListener(e -> onChangePort());
        upnpButton.addActionListener(e -> onOpenUpnp());
        firewallButton.addActionListener(e -> onCheckFirewall());
        modsButton.addActionListener(e -> onOpenMods());
        utilitiesButton.addActionListener(e -> onOpenUtilities());
        curseForgeButton.addActionListener(e -> onOpenCurseForgeImport());
        zeroGModsButton.addActionListener(e -> onOpenZeroGMods());
        return card;
    }

    private Frame ownerFrame() {
        Window window = SwingUtilities.getWindowAncestor(this);
        return window instanceof Frame ? (Frame) window : null;
    }

    private void onOpenMods() {
        new ModsDialog(ownerFrame(), ctx, settings).setVisible(true);
    }

    private void onOpenUtilities() {
        new UtilitiesDialog(ownerFrame(), ctx, settings).setVisible(true);
    }

    private void onOpenZeroGMods() {
        new ZeroGModsDialog(ownerFrame(), ctx, settings).setVisible(true);
    }

    private void onOpenCurseForgeImport() {
        new CurseForgeImportDialog(ownerFrame(), ctx, imported -> {
            this.settings = imported;
            persistSettings();
            refreshLabels();
        }).setVisible(true);
    }

    private static void themeSpinner(JSpinner spinner) {
        spinner.setFont(StellarTheme.FONT_BODY);
        spinner.setBorder(BorderFactory.createLineBorder(StellarTheme.PANEL_BORDER));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            defaultEditor.getTextField().setBackground(StellarTheme.VOID_BLACK);
            defaultEditor.getTextField().setForeground(StellarTheme.STAR_CYAN);
            defaultEditor.getTextField().setCaretColor(StellarTheme.STAR_CYAN);
            defaultEditor.getTextField().setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        }
    }

    private void onChangePort() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(Math.max(settings.getPort(), 10000), 10000, 65535, 1));
        themeSpinner(spinner);
        int result = JOptionPane.showConfirmDialog(this, spinner, "Server Port (>= 10000)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        int newPort = (Integer) spinner.getValue();
        settings.setPort(newPort);
        persistSettings();
        try {
            ctx.serverPropertiesService.ensureValidAndSynced(newPort);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not sync server.properties: " + ex.getMessage(),
                    "Sync failed", JOptionPane.WARNING_MESSAGE);
        }
        refreshLabels();
    }

    private void onOpenUpnp() {
        new UpnpDialog(ownerFrame(), ctx, settings, this::refreshLabels).setVisible(true);
    }

    private void onCheckFirewall() {
        if (lastResolvedJavaCommand == null) {
            JOptionPane.showMessageDialog(this, "Launch the server at least once first, so the Java executable "
                    + "being used is known.", "Firewall check", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        firewallButton.setEnabled(false);
        new SwingWorker<com.zerog.stellarserverforge.net_port.FirewallCheckService.Result, Void>() {
            @Override
            protected com.zerog.stellarserverforge.net_port.FirewallCheckService.Result doInBackground() {
                return ctx.firewallCheckService.check(settings.getPort(), lastResolvedJavaCommand);
            }

            @Override
            protected void done() {
                firewallButton.setEnabled(true);
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

    private void onChangeRam() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(settings.getMaxRamGigs(), 1, 128, 1));
        themeSpinner(spinner);
        int result = JOptionPane.showConfirmDialog(this, spinner, "Maximum RAM (GB)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            settings.setMaxRamGigs((Integer) spinner.getValue());
            persistSettings();
            refreshLabels();
        }
    }

    private void onChangeModLoaderVersion() {
        new ModLoaderVersionDialog(ownerFrame(), ctx, settings, this::refreshLabels).setVisible(true);
    }

    private void onCycleJavaOverride() {
        JavaOverrideMode next = switch (settings.getJavaOverrideMode()) {
            case AUTOMATIC -> JavaOverrideMode.SYSTEM_PATH;
            case SYSTEM_PATH -> JavaOverrideMode.FORCE_MANAGED;
            case FORCE_MANAGED -> JavaOverrideMode.AUTOMATIC;
        };
        settings.setJavaOverrideMode(next);
        persistSettings();
        refreshLabels();
    }

    private void persistSettings() {
        try {
            ctx.settingsService.save(settings);
        } catch (java.io.IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save settings.json: " + e.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshLabels() {
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
        modLoaderVersionButton.setEnabled(settings.getModLoader() != ModLoader.VANILLA);
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
        settingsButton.setEnabled(false);
        stopButton.setEnabled(true);
        console.setText("");
        restartCount = 0;
        stopRequested = false;

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
                    settingsButton.setEnabled(true);
                    stopButton.setEnabled(false);
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
            SwingUtilities.invokeLater(() -> stopButton.setEnabled(true));
            appendConsole("Launching server...");
            runner.start(java.command(), jvmArgs, tailArgs, this::appendConsole);

            int exitCode = runner.waitForExit();
            appendConsole("Server process exited with code " + exitCode + ".");

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
            settingsButton.setEnabled(true);
            stopButton.setEnabled(false);
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

    public void updateSettings(ServerSettings settings) {
        this.settings = settings;
        refreshLabels();
    }
}
