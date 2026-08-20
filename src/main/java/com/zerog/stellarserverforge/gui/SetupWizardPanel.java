package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.javamanaged.JavaVersionRules;
import com.zerog.stellarserverforge.model.JavaOverrideMode;
import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.model.ServerSettings;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * First-run settings entry: Minecraft version -> Java version -> RAM, mirroring the bat script's
 * {@code settingsentry} flow (spec §1, §3), translated from prompt-loop-until-valid into inline
 * form validation. Modloader is fixed to VANILLA in this phase.
 */
public class SetupWizardPanel extends JPanel {

    private final AppContext ctx;
    private final Consumer<ServerSettings> onComplete;

    private final CardLayout steps = new CardLayout();
    private final JPanel stepContainer = new JPanel(steps);

    private final JTextField mcVersionField = new JTextField(12);
    private final JLabel mcVersionStatus = new JLabel(" ");
    private final JButton mcNextButton = new JButton("Next");

    private final ButtonGroup javaVersionGroup = new ButtonGroup();
    private final JPanel javaVersionOptionsPanel = new JPanel();
    private final JButton javaNextButton = new JButton("Next");

    private final JSpinner ramSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 128, 1));
    private final JButton finishButton = new JButton("Finish");

    private McVersion validatedMcVersion;
    private int chosenJavaVersion;

    public SetupWizardPanel(AppContext ctx, Consumer<ServerSettings> onComplete) {
        this.ctx = ctx;
        this.onComplete = onComplete;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("StellarServerForge — First-Time Setup");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        add(title, BorderLayout.NORTH);

        stepContainer.add(buildMcVersionStep(), "mc");
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
                    populateJavaVersionStep(validatedMcVersion);
                    steps.show(stepContainer, "java");
                } catch (Exception ex) {
                    mcVersionStatus.setForeground(Color.RED);
                    mcVersionStatus.setText("Could not reach the Mojang version list: " + ex.getMessage());
                }
            }
        }.execute();
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

        javaNextButton.addActionListener(e -> {
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
        settings.setModLoader(ModLoader.VANILLA);
        settings.setModLoaderVersion("");
        settings.setJavaVersion(chosenJavaVersion);
        settings.setMaxRamGigs((Integer) ramSpinner.getValue());
        settings.setJavaOverrideMode(JavaOverrideMode.AUTOMATIC);
        onComplete.accept(settings);
    }
}
