package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.javamanaged.JavaVersionRules;
import com.zerog.stellarserverforge.model.JavaOverrideMode;
import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.model.ServerSettings;
import com.zerog.stellarserverforge.modloader.ModLoaderVersionResolver;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * First-run settings entry: Minecraft version -> modloader type -> modloader version ->
 * Java version -> RAM, mirroring the bat script's {@code settingsentry} flow (spec §1, §3),
 * translated from prompt-loop-until-valid into inline form validation.
 */
public class SetupWizardPanel extends JPanel {

    private final AppContext ctx;
    private final Consumer<ServerSettings> onComplete;

    private final CardLayout steps = new CardLayout();
    private final JPanel stepContainer = new JPanel(steps);

    private final JTextField mcVersionField = new JTextField(12);
    private final JLabel mcVersionStatus = new JLabel(" ");
    private final JButton mcNextButton = new JButton("Next");

    private final ButtonGroup modLoaderGroup = new ButtonGroup();
    private final JButton modLoaderNextButton = new JButton("Next");
    private ModLoader chosenModLoader = ModLoader.VANILLA;

    private final JRadioButton useNewestRadio = new JRadioButton("Use the newest published version", true);
    private final JRadioButton useCustomRadio = new JRadioButton("Enter a custom version");
    private final JTextField customVersionField = new JTextField(12);
    private final JLabel modLoaderVersionInfo = new JLabel(" ");
    private final JLabel modLoaderVersionStatus = new JLabel(" ");
    private final JButton modLoaderVersionNextButton = new JButton("Next");
    private String resolvedNewestModLoaderVersion;

    private final ButtonGroup javaVersionGroup = new ButtonGroup();
    private final JPanel javaVersionOptionsPanel = new JPanel();
    private final JButton javaNextButton = new JButton("Next");

    private final JSpinner ramSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 128, 1));
    private final JButton finishButton = new JButton("Finish");

    private McVersion validatedMcVersion;
    private int chosenJavaVersion;
    private String chosenModLoaderVersion = "";

    public SetupWizardPanel(AppContext ctx, Consumer<ServerSettings> onComplete) {
        this.ctx = ctx;
        this.onComplete = onComplete;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("StellarServerForge — First-Time Setup");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        add(title, BorderLayout.NORTH);

        stepContainer.add(buildMcVersionStep(), "mc");
        stepContainer.add(buildModLoaderTypeStep(), "modloader");
        stepContainer.add(buildModLoaderVersionStep(), "modloaderVersion");
        stepContainer.add(buildJavaVersionStep(), "java");
        stepContainer.add(buildRamStep(), "ram");
        add(stepContainer, BorderLayout.CENTER);

        steps.show(stepContainer, "mc");
    }

    private JPanel buildMcVersionStep() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("Enter the Minecraft version (e.g. 1.20.1):"));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(mcVersionField);
        row.add(mcNextButton);
        panel.add(row);
        mcVersionStatus.setForeground(Color.GRAY);
        panel.add(mcVersionStatus);

        mcNextButton.addActionListener(e -> validateMcVersion());
        mcVersionField.addActionListener(e -> validateMcVersion());
        return panel;
    }

    private void validateMcVersion() {
        String entered = mcVersionField.getText().trim();
        if (entered.isEmpty()) {
            mcVersionStatus.setText("Enter a version.");
            mcVersionStatus.setForeground(Color.RED);
            return;
        }
        mcNextButton.setEnabled(false);
        mcVersionStatus.setForeground(Color.GRAY);
        mcVersionStatus.setText("Checking against the Mojang version list...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return ctx.mojangManifestService.isValidReleaseVersion(entered);
            }

            @Override
            protected void done() {
                mcNextButton.setEnabled(true);
                try {
                    boolean valid = get();
                    if (!valid) {
                        mcVersionStatus.setForeground(Color.RED);
                        mcVersionStatus.setText("Not a recognized release version. Only release versions "
                                + "(no snapshots/betas) are supported — try again.");
                        return;
                    }
                    validatedMcVersion = McVersion.parse(entered);
                    mcVersionStatus.setForeground(new Color(0, 128, 0));
                    mcVersionStatus.setText("Valid.");
                    steps.show(stepContainer, "modloader");
                } catch (Exception ex) {
                    mcVersionStatus.setForeground(Color.RED);
                    mcVersionStatus.setText("Could not reach the Mojang version list: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private JPanel buildModLoaderTypeStep() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(new JLabel("Select the modloader:"));

        for (ModLoader loader : ModLoader.values()) {
            JRadioButton radio = new JRadioButton(loader.name(), loader == ModLoader.VANILLA);
            radio.addActionListener(e -> chosenModLoader = loader);
            modLoaderGroup.add(radio);
            wrapper.add(radio);
        }
        panel.add(wrapper, BorderLayout.CENTER);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(modLoaderNextButton);
        panel.add(row, BorderLayout.SOUTH);

        modLoaderNextButton.addActionListener(e -> {
            if (chosenModLoader == ModLoader.VANILLA) {
                chosenModLoaderVersion = "";
                populateJavaVersionStep(validatedMcVersion);
                steps.show(stepContainer, "java");
            } else {
                beginModLoaderVersionResolution();
                steps.show(stepContainer, "modloaderVersion");
            }
        });
        return panel;
    }

    private JPanel buildModLoaderVersionStep() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(modLoaderVersionInfo);

        ButtonGroup group = new ButtonGroup();
        group.add(useNewestRadio);
        group.add(useCustomRadio);
        wrapper.add(useNewestRadio);
        JPanel customRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        customRow.add(useCustomRadio);
        customRow.add(customVersionField);
        wrapper.add(customRow);
        modLoaderVersionStatus.setForeground(Color.RED);
        wrapper.add(modLoaderVersionStatus);
        panel.add(wrapper, BorderLayout.CENTER);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(modLoaderVersionNextButton);
        panel.add(row, BorderLayout.SOUTH);

        modLoaderVersionNextButton.addActionListener(e -> confirmModLoaderVersion());
        return panel;
    }

    private void beginModLoaderVersionResolution() {
        useNewestRadio.setSelected(true);
        customVersionField.setText("");
        modLoaderVersionStatus.setText(" ");
        modLoaderVersionInfo.setText("Fetching available " + chosenModLoader + " versions for "
                + validatedMcVersion.raw() + "...");
        modLoaderVersionNextButton.setEnabled(false);
        resolvedNewestModLoaderVersion = null;

        new SwingWorker<Optional<String>, Void>() {
            @Override
            protected Optional<String> doInBackground() throws Exception {
                Path metadataFile = ctx.modLoaderMetadataService.ensureMetadataFile(chosenModLoader, validatedMcVersion);
                Path promotionsFile = chosenModLoader == ModLoader.FORGE
                        ? ctx.modLoaderMetadataService.ensurePromotionsFile()
                        : null;
                return ctx.modLoaderVersionResolver.resolveNewest(chosenModLoader, validatedMcVersion, metadataFile, promotionsFile);
            }

            @Override
            protected void done() {
                modLoaderVersionNextButton.setEnabled(true);
                try {
                    Optional<String> newest = get();
                    if (newest.isPresent()) {
                        resolvedNewestModLoaderVersion = newest.get();
                        modLoaderVersionInfo.setText("Newest detected " + chosenModLoader + " version for "
                                + validatedMcVersion.raw() + ": " + resolvedNewestModLoaderVersion);
                        useNewestRadio.setText("Use " + resolvedNewestModLoaderVersion);
                        useNewestRadio.setEnabled(true);
                        useNewestRadio.setSelected(true);
                    } else {
                        modLoaderVersionInfo.setText("Could not auto-detect a newest version — enter one manually.");
                        useNewestRadio.setEnabled(false);
                        useCustomRadio.setSelected(true);
                    }
                } catch (Exception ex) {
                    modLoaderVersionInfo.setText("Could not fetch " + chosenModLoader
                            + " version data (" + rootMessage(ex) + ") — enter a version manually.");
                    useNewestRadio.setEnabled(false);
                    useCustomRadio.setSelected(true);
                }
            }
        }.execute();
    }

    private void confirmModLoaderVersion() {
        if (useNewestRadio.isSelected() && resolvedNewestModLoaderVersion != null) {
            chosenModLoaderVersion = resolvedNewestModLoaderVersion;
            populateJavaVersionStep(validatedMcVersion);
            steps.show(stepContainer, "java");
            return;
        }

        String entered = customVersionField.getText().trim();
        if (entered.isEmpty()) {
            modLoaderVersionStatus.setText("Enter a version number.");
            return;
        }
        if (chosenModLoader == ModLoader.FORGE && ModLoaderVersionResolver.containsLetters(entered)) {
            modLoaderVersionStatus.setText("Forge versions are purely numeric — that doesn't look right.");
            return;
        }

        modLoaderVersionNextButton.setEnabled(false);
        modLoaderVersionStatus.setText("Checking that version exists...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                Path metadataFile = ctx.modLoaderMetadataService.ensureMetadataFile(chosenModLoader, validatedMcVersion);
                return ctx.modLoaderVersionResolver.isValidVersion(chosenModLoader, validatedMcVersion, metadataFile, entered);
            }

            @Override
            protected void done() {
                modLoaderVersionNextButton.setEnabled(true);
                try {
                    if (get()) {
                        chosenModLoaderVersion = entered;
                        modLoaderVersionStatus.setText(" ");
                        populateJavaVersionStep(validatedMcVersion);
                        steps.show(stepContainer, "java");
                    } else {
                        modLoaderVersionStatus.setText("That version does not seem to exist on the " + chosenModLoader
                                + " file server for Minecraft " + validatedMcVersion.raw() + " — try another.");
                    }
                } catch (Exception ex) {
                    modLoaderVersionStatus.setText("Could not verify that version: " + rootMessage(ex));
                }
            }
        }.execute();
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.toString();
    }

    private JPanel buildJavaVersionStep() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(new JLabel("Select the Java version to use for this server:"));
        javaVersionOptionsPanel.setLayout(new BoxLayout(javaVersionOptionsPanel, BoxLayout.Y_AXIS));
        wrapper.add(javaVersionOptionsPanel);
        panel.add(wrapper, BorderLayout.CENTER);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(javaNextButton);
        panel.add(row, BorderLayout.SOUTH);

        javaNextButton.addActionListener(e -> steps.show(stepContainer, "ram"));
        return panel;
    }

    private void populateJavaVersionStep(McVersion mc) {
        javaVersionOptionsPanel.removeAll();
        javaVersionGroup.getElements().asIterator().forEachRemaining(javaVersionGroup::remove);

        JavaVersionRules.JavaOptions options = JavaVersionRules.resolve(mc);
        List<Integer> choices = options.options();
        chosenJavaVersion = options.defaultVersion();

        if (options.onlyOneChoice()) {
            javaVersionOptionsPanel.add(new JLabel("Java " + choices.get(0) + " (the only supported version for this Minecraft release)."));
        } else {
            for (int choice : choices) {
                JRadioButton radio = new JRadioButton("Java " + choice + (choice == options.defaultVersion() ? " (recommended)" : ""));
                radio.setSelected(choice == options.defaultVersion());
                radio.addActionListener(e -> chosenJavaVersion = choice);
                javaVersionGroup.add(radio);
                javaVersionOptionsPanel.add(radio);
            }
        }
        javaVersionOptionsPanel.revalidate();
        javaVersionOptionsPanel.repaint();
    }

    private JPanel buildRamStep() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(new JLabel("Maximum RAM to allocate (GB):"));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(ramSpinner);
        wrapper.add(row);
        panel.add(wrapper, BorderLayout.CENTER);

        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomRow.add(finishButton);
        panel.add(bottomRow, BorderLayout.SOUTH);

        finishButton.addActionListener(e -> finishWizard());
        return panel;
    }

    private void finishWizard() {
        ServerSettings settings = new ServerSettings();
        settings.setMinecraftVersion(validatedMcVersion.raw());
        settings.setModLoader(chosenModLoader);
        settings.setModLoaderVersion(chosenModLoaderVersion);
        settings.setJavaVersion(chosenJavaVersion);
        settings.setMaxRamGigs((Integer) ramSpinner.getValue());
        settings.setJavaOverrideMode(JavaOverrideMode.AUTOMATIC);
        onComplete.accept(settings);
    }
}
