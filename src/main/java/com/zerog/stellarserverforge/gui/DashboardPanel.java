package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.javamanaged.JavaProvisioningService;
import com.zerog.stellarserverforge.launch.LaunchArgsBuilder;
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

    private final JLabel mcVersionLabel = new JLabel();
    private final JLabel modLoaderLabel = new JLabel();
    private final JLabel javaVersionLabel = new JLabel();
    private final JLabel ramLabel = new JLabel();
    private final JLabel portLabel = new JLabel();
    private final JLabel javaOverrideLabel = new JLabel();
    private final JLabel statusLabel = new JLabel("Idle");

    private final JButton launchButton = new JButton("Launch");
    private final JButton stopButton = new JButton("Stop");
    private final JButton settingsButton = new JButton("Re-enter Settings");
    private final JButton ramButton = new JButton("Change RAM");
    private final JButton javaOverrideButton = new JButton("Cycle Java Mode");

    private final JTextArea console = new JTextArea();

    public DashboardPanel(AppContext ctx, ServerSettings settings, Runnable onReenterSettings) {
        this.ctx = ctx;
        this.settings = settings;
        this.onReenterSettings = onReenterSettings;
        this.runner = new ServerProcessRunner(ctx.serverDir);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildConsole(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        refreshLabels();
        stopButton.setEnabled(false);
    }

    private JComponent buildHeader() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 4));
        panel.add(new JLabel("Minecraft version:"));
        panel.add(mcVersionLabel);
        panel.add(new JLabel("Modloader:"));
        panel.add(modLoaderLabel);
        panel.add(new JLabel("Java version:"));
        panel.add(javaVersionLabel);
        panel.add(new JLabel("Max RAM:"));
        panel.add(ramLabel);
        panel.add(new JLabel("Port:"));
        panel.add(portLabel);
        panel.add(new JLabel("Java mode:"));
        panel.add(javaOverrideLabel);
        panel.add(new JLabel("Status:"));
        panel.add(statusLabel);
        return panel;
    }

    private JComponent buildConsole() {
        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(console);
        scroll.setBorder(BorderFactory.createTitledBorder("Console"));
        return scroll;
    }

    private JComponent buildFooter() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(launchButton);
        panel.add(stopButton);
        panel.add(ramButton);
        panel.add(javaOverrideButton);
        panel.add(settingsButton);

        launchButton.addActionListener(this::onLaunch);
        stopButton.addActionListener(this::onStop);
        settingsButton.addActionListener(e -> onReenterSettings.run());
        ramButton.addActionListener(e -> onChangeRam());
        javaOverrideButton.addActionListener(e -> onCycleJavaOverride());
        return panel;
    }

    private void onChangeRam() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(settings.getMaxRamGigs(), 1, 128, 1));
        int result = JOptionPane.showConfirmDialog(this, spinner, "Maximum RAM (GB)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            settings.setMaxRamGigs((Integer) spinner.getValue());
            persistSettings();
            refreshLabels();
        }
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
            case AUTOMATIC -> "Automatic (detect or download)";
            case SYSTEM_PATH -> "System PATH java";
            case FORCE_MANAGED -> "Force managed (Adoptium)";
        });
    }

    private void appendConsole(String line) {
        SwingUtilities.invokeLater(() -> {
            console.append(line + System.lineSeparator());
            console.setCaretPosition(console.getDocument().getLength());
        });
    }

    private void setStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }

    private void onLaunch(ActionEvent e) {
        launchButton.setEnabled(false);
        settingsButton.setEnabled(false);
        console.setText("");

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

    private void runLaunchSequence() throws Exception {
        setStatus("Resolving Java...");
        appendConsole("Resolving Java " + settings.getJavaVersion() + "...");
        JavaProvisioningService.ResolvedJava java = ctx.javaProvisioningService.resolve(
                settings.getJavaVersion(), settings.getJavaOverrideMode());
        appendConsole("Using Java from: " + java.source() + " (" + java.command() + ")");

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

        setStatus("Checking port availability...");
        if (!ctx.portConflictService.isPortFree(settings.getPort())) {
            handlePortConflict();
        }

        setStatus("Checking EULA...");
        if (!ctx.eulaService.isAccepted()) {
            handleEulaPrompt();
        }

        McVersion mc = McVersion.parse(settings.getMinecraftVersion());
        List<String> tailArgs = ensureModLoaderInstalledAndBuildTailArgs(mc, java.command());

        List<String> jvmArgs = LaunchArgsBuilder.buildJvmArgs(settings, settings.getJavaVersion());

        setStatus("Running");
        SwingUtilities.invokeLater(() -> stopButton.setEnabled(true));
        appendConsole("Launching server...");
        runner.start(java.command(), jvmArgs, tailArgs, this::appendConsole);

        int exitCode = runner.waitForExit();
        appendConsole("Server process exited with code " + exitCode + ".");
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
        stopButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                appendConsole("Stopping server...");
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
