package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.gui.theme.StellarButton;
import com.zerog.stellarserverforge.gui.theme.StellarLabels;
import com.zerog.stellarserverforge.gui.theme.StellarPanel;
import com.zerog.stellarserverforge.gui.theme.StellarTheme;
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
    private final JPanel stepContainer = themedPanel(steps);

    private final JLabel stepIndicator = StellarLabels.muted("Step 1 of 5");

    private final JTextField mcVersionField = themedField(12);
    private final JLabel mcVersionStatus = StellarLabels.muted(" ");
    private final StellarButton mcNextButton = new StellarButton("Next →", StellarButton.Variant.PRIMARY);

    private final ButtonGroup modLoaderGroup = new ButtonGroup();
    private final StellarButton modLoaderNextButton = new StellarButton("Next →", StellarButton.Variant.PRIMARY);
    private ModLoader chosenModLoader = ModLoader.VANILLA;

    private final JRadioButton useNewestRadio = themedRadio("Use the newest published version", true);
    private final JRadioButton useCustomRadio = themedRadio("Enter a custom version", false);
    private final JTextField customVersionField = themedField(12);
    private final JLabel modLoaderVersionInfo = StellarLabels.muted(" ");
    private final JLabel modLoaderVersionStatus = StellarLabels.muted(" ");
    private final StellarButton modLoaderVersionNextButton = new StellarButton("Next →", StellarButton.Variant.PRIMARY);
    private String resolvedNewestModLoaderVersion;

    private final ButtonGroup javaVersionGroup = new ButtonGroup();
    private final JPanel javaVersionOptionsPanel = themedPanel(null);
    private final StellarButton javaNextButton = new StellarButton("Next →", StellarButton.Variant.PRIMARY);

    private final JSpinner ramSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 128, 1));
    private final StellarButton finishButton = new StellarButton("Launch Into Orbit ✨", StellarButton.Variant.PRIMARY);

    private McVersion validatedMcVersion;
    private int chosenJavaVersion;
    private String chosenModLoaderVersion = "";

    public SetupWizardPanel(AppContext ctx, Consumer<ServerSettings> onComplete) {
        this.ctx = ctx;
        this.onComplete = onComplete;
        setOpaque(false);
        setLayout(new GridBagLayout());

        StellarPanel card = new StellarPanel(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        card.setPreferredSize(new Dimension(560, 460));

        JLabel title = StellarLabels.title("StellarServerForge");
        JLabel subtitle = StellarLabels.muted("Modded Minecraft server setup");
        JPanel header = themedPanel(null);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title);
        header.add(subtitle);
        header.add(Box.createVerticalStrut(4));
        stepIndicator.setFont(StellarTheme.FONT_LABEL);
        stepIndicator.setForeground(StellarTheme.STELLAR_GOLD);
        header.add(stepIndicator);
        card.add(header, BorderLayout.NORTH);

        stepContainer.add(buildMcVersionStep(), "mc");
        stepContainer.add(buildModLoaderTypeStep(), "modloader");
        stepContainer.add(buildModLoaderVersionStep(), "modloaderVersion");
        stepContainer.add(buildJavaVersionStep(), "java");
        stepContainer.add(buildRamStep(), "ram");
        card.add(stepContainer, BorderLayout.CENTER);

        add(card);
        steps.show(stepContainer, "mc");
    }

    private static JPanel themedPanel(LayoutManager layout) {
        JPanel panel = layout == null ? new JPanel() : new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static JTextField themedField(int columns) {
        JTextField field = new JTextField(columns);
        field.setBackground(StellarTheme.VOID_BLACK);
        field.setForeground(StellarTheme.STAR_CYAN);
        field.setCaretColor(StellarTheme.STAR_CYAN);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(StellarTheme.PANEL_BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        field.setFont(StellarTheme.FONT_BODY);
        return field;
    }

    private static JRadioButton themedRadio(String text, boolean selected) {
        JRadioButton radio = new JRadioButton(text, selected);
        radio.setOpaque(false);
        radio.setForeground(StellarTheme.TEXT_PRIMARY);
        radio.setFont(StellarTheme.FONT_BODY);
        return radio;
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

    private JPanel buildMcVersionStep() {
        JPanel panel = themedPanel(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(StellarLabels.heading("What Minecraft version?"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(StellarLabels.muted("e.g. 1.20.1, 1.21.1 — release versions only"));
        panel.add(Box.createVerticalStrut(10));
        JPanel row = themedPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.add(mcVersionField);
        row.add(mcNextButton);
        panel.add(row);
        panel.add(mcVersionStatus);

        mcNextButton.addActionListener(e -> validateMcVersion());
        mcVersionField.addActionListener(e -> validateMcVersion());
        return panel;
    }

    private void validateMcVersion() {
        String entered = mcVersionField.getText().trim();
        if (entered.isEmpty()) {
            setStatus(mcVersionStatus, "Enter a version.", StellarTheme.WARNING_ORANGE);
            return;
        }
        mcNextButton.setEnabled(false);
        setStatus(mcVersionStatus, "Checking against the Mojang version list...", StellarTheme.TEXT_SECONDARY);

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
                        setStatus(mcVersionStatus, "Not a recognized release version. Only release versions "
                                + "(no snapshots/betas) are supported — try again.", StellarTheme.ERROR_RED);
                        return;
                    }
                    validatedMcVersion = McVersion.parse(entered);
                    setStatus(mcVersionStatus, "Valid.", StellarTheme.SUCCESS_GREEN);
                    stepIndicator.setText("Step 2 of 5 — Modloader");
                    steps.show(stepContainer, "modloader");
                } catch (Exception ex) {
                    setStatus(mcVersionStatus, "Could not reach the Mojang version list: " + ex.getMessage(),
                            StellarTheme.ERROR_RED);
                }
            }
        }.execute();
    }

    private JPanel buildModLoaderTypeStep() {
        JPanel panel = themedPanel(new BorderLayout());
        JPanel wrapper = themedPanel(null);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(StellarLabels.heading("Choose your modloader"));
        wrapper.add(Box.createVerticalStrut(10));

        for (ModLoader loader : ModLoader.values()) {
            JRadioButton radio = themedRadio(loader.name(), loader == ModLoader.VANILLA);
            radio.addActionListener(e -> chosenModLoader = loader);
            modLoaderGroup.add(radio);
            wrapper.add(radio);
            wrapper.add(Box.createVerticalStrut(2));
        }
        panel.add(wrapper, BorderLayout.CENTER);

        JPanel row = themedPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(modLoaderNextButton);
        panel.add(row, BorderLayout.SOUTH);

        modLoaderNextButton.addActionListener(e -> {
            if (chosenModLoader == ModLoader.VANILLA) {
                chosenModLoaderVersion = "";
                populateJavaVersionStep(validatedMcVersion);
                stepIndicator.setText("Step 4 of 5 — Java Version");
                steps.show(stepContainer, "java");
            } else {
                beginModLoaderVersionResolution();
                stepIndicator.setText("Step 3 of 5 — " + chosenModLoader + " Version");
                steps.show(stepContainer, "modloaderVersion");
            }
        });
        return panel;
    }

    private JPanel buildModLoaderVersionStep() {
        JPanel panel = themedPanel(new BorderLayout());
        JPanel wrapper = themedPanel(null);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(StellarLabels.heading("Modloader version"));
        wrapper.add(Box.createVerticalStrut(6));
        wrapper.add(modLoaderVersionInfo);
        wrapper.add(Box.createVerticalStrut(8));

        ButtonGroup group = new ButtonGroup();
        group.add(useNewestRadio);
        group.add(useCustomRadio);
        wrapper.add(useNewestRadio);
        JPanel customRow = themedPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        customRow.add(useCustomRadio);
        customRow.add(customVersionField);
        wrapper.add(customRow);
        wrapper.add(modLoaderVersionStatus);
        panel.add(wrapper, BorderLayout.CENTER);

        JPanel row = themedPanel(new FlowLayout(FlowLayout.LEFT));
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
                var preflight = ctx.networkPreflightService.checkModLoaderAndMojangHosts(chosenModLoader);
                if (!preflight.ok()) {
                    throw new java.io.IOException("Could not resolve: " + String.join(", ", preflight.unresolvedHosts())
                            + " — check your internet connection or try a public DNS server (1.1.1.1 or 8.8.8.8).");
                }
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
            stepIndicator.setText("Step 4 of 5 — Java Version");
            steps.show(stepContainer, "java");
            return;
        }

        String entered = customVersionField.getText().trim();
        if (entered.isEmpty()) {
            setStatus(modLoaderVersionStatus, "Enter a version number.", StellarTheme.WARNING_ORANGE);
            return;
        }
        if (chosenModLoader == ModLoader.FORGE && ModLoaderVersionResolver.containsLetters(entered)) {
            setStatus(modLoaderVersionStatus, "Forge versions are purely numeric — that doesn't look right.",
                    StellarTheme.ERROR_RED);
            return;
        }

        modLoaderVersionNextButton.setEnabled(false);
        setStatus(modLoaderVersionStatus, "Checking that version exists...", StellarTheme.TEXT_SECONDARY);

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
                        stepIndicator.setText("Step 4 of 5 — Java Version");
                        steps.show(stepContainer, "java");
                    } else {
                        setStatus(modLoaderVersionStatus, "That version does not seem to exist on the "
                                + chosenModLoader + " file server for Minecraft " + validatedMcVersion.raw()
                                + " — try another.", StellarTheme.ERROR_RED);
                    }
                } catch (Exception ex) {
                    setStatus(modLoaderVersionStatus, "Could not verify that version: " + rootMessage(ex),
                            StellarTheme.ERROR_RED);
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

    private static void setStatus(JLabel label, String text, Color color) {
        label.setText(text);
        label.setForeground(color);
    }

    private JPanel buildJavaVersionStep() {
        JPanel panel = themedPanel(new BorderLayout());
        JPanel wrapper = themedPanel(null);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(StellarLabels.heading("Java version"));
        wrapper.add(Box.createVerticalStrut(8));
        javaVersionOptionsPanel.setLayout(new BoxLayout(javaVersionOptionsPanel, BoxLayout.Y_AXIS));
        wrapper.add(javaVersionOptionsPanel);
        panel.add(wrapper, BorderLayout.CENTER);

        JPanel row = themedPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(javaNextButton);
        panel.add(row, BorderLayout.SOUTH);

        javaNextButton.addActionListener(e -> {
            stepIndicator.setText("Step 5 of 5 — Memory");
            steps.show(stepContainer, "ram");
        });
        return panel;
    }

    private void populateJavaVersionStep(McVersion mc) {
        javaVersionOptionsPanel.removeAll();
        javaVersionGroup.getElements().asIterator().forEachRemaining(javaVersionGroup::remove);

        JavaVersionRules.JavaOptions options = JavaVersionRules.resolve(mc);
        List<Integer> choices = options.options();
        chosenJavaVersion = options.defaultVersion();

        if (options.onlyOneChoice()) {
            javaVersionOptionsPanel.add(StellarLabels.body(
                    "Java " + choices.get(0) + " (the only supported version for this Minecraft release)."));
        } else {
            for (int choice : choices) {
                JRadioButton radio = themedRadio(
                        "Java " + choice + (choice == options.defaultVersion() ? " (recommended)" : ""),
                        choice == options.defaultVersion());
                radio.addActionListener(e -> chosenJavaVersion = choice);
                javaVersionGroup.add(radio);
                javaVersionOptionsPanel.add(radio);
            }
        }
        javaVersionOptionsPanel.revalidate();
        javaVersionOptionsPanel.repaint();
    }

    private JPanel buildRamStep() {
        JPanel panel = themedPanel(new BorderLayout());
        JPanel wrapper = themedPanel(null);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(StellarLabels.heading("Maximum RAM to allocate"));
        wrapper.add(Box.createVerticalStrut(8));
        themeSpinner(ramSpinner);
        JPanel row = themedPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(ramSpinner);
        row.add(StellarLabels.muted("GB"));
        wrapper.add(row);
        panel.add(wrapper, BorderLayout.CENTER);

        JPanel bottomRow = themedPanel(new FlowLayout(FlowLayout.LEFT));
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
