package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.gui.theme.StellarButton;
import com.zerog.stellarserverforge.gui.theme.StellarLabels;
import com.zerog.stellarserverforge.gui.theme.StellarPanel;
import com.zerog.stellarserverforge.gui.theme.StellarTag;
import com.zerog.stellarserverforge.gui.theme.StellarTheme;
import com.zerog.stellarserverforge.model.JavaOverrideMode;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.model.ServerSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;

/**
 * The real Settings screen — reached from the dashboard's "Settings" nav link. Unlike that link's
 * old behaviour (which re-ran the entire setup wizard), this is a proper settings surface: RAM,
 * port, Java resolution mode, modloader version, and networking, each applying immediately with
 * an inline status line. Re-running the setup wizard is now just one button here, not what
 * clicking "Settings" itself does.
 */
public class SettingsPanel extends JPanel {

    private final AppContext ctx;
    private final ServerSettings settings;
    private final Runnable onReenterWizard;
    private final Runnable onSettingsChanged;
    private final Runnable onOpenUpnp;
    private final Runnable onCheckFirewall;
    private final Runnable onChangeModLoaderVersion;

    private final JLabel statusLabel = StellarLabels.muted(" ");
    private final JPanel javaModeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_8, 0));

    public SettingsPanel(AppContext ctx, ServerSettings settings, Runnable onBack, Runnable onReenterWizard,
                          Runnable onSettingsChanged, Runnable onOpenUpnp, Runnable onCheckFirewall,
                          Runnable onChangeModLoaderVersion) {
        this.ctx = ctx;
        this.settings = settings;
        this.onReenterWizard = onReenterWizard;
        this.onSettingsChanged = onSettingsChanged;
        this.onOpenUpnp = onOpenUpnp;
        this.onCheckFirewall = onCheckFirewall;
        this.onChangeModLoaderVersion = onChangeModLoaderVersion;

        setOpaque(false);
        setLayout(new BorderLayout(0, StellarTheme.SPACE_17));
        setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_22, StellarTheme.SPACE_22,
                StellarTheme.SPACE_22, StellarTheme.SPACE_22));

        JPanel header = new JPanel(new BorderLayout(StellarTheme.SPACE_11, 0));
        header.setOpaque(false);
        StellarButton back = new StellarButton("Back", StellarButton.Variant.GHOST);
        back.addActionListener(e -> onBack.run());
        header.add(back, BorderLayout.WEST);
        header.add(StellarLabels.title("Settings"), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridLayout(1, 2, StellarTheme.SPACE_17, 0));
        columns.setOpaque(false);
        columns.add(buildServerCard());
        columns.add(buildJavaAndNetworkingCard());
        add(columns, BorderLayout.CENTER);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_8, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JComponent buildServerCard() {
        StellarPanel card = new StellarPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_17, StellarTheme.SPACE_17,
                StellarTheme.SPACE_17, StellarTheme.SPACE_17));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(StellarLabels.kicker("Server"));
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_11));

        content.add(StellarLabels.body("RAM allocation"));
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_6));
        JSlider ramSlider = new JSlider(1, 128, settings.getMaxRamGigs());
        ramSlider.setOpaque(false);
        JLabel ramValue = StellarLabels.value(settings.getMaxRamGigs() + " GB");
        JPanel ramRow = new JPanel(new BorderLayout(StellarTheme.SPACE_8, 0));
        ramRow.setOpaque(false);
        ramRow.add(ramSlider, BorderLayout.CENTER);
        ramRow.add(ramValue, BorderLayout.EAST);
        content.add(ramRow);
        ramSlider.addChangeListener(e -> {
            ramValue.setText(ramSlider.getValue() + " GB");
            if (!ramSlider.getValueIsAdjusting() && ramSlider.getValue() != settings.getMaxRamGigs()) {
                settings.setMaxRamGigs(ramSlider.getValue());
                persist("RAM set to " + ramSlider.getValue() + " GB.");
            }
        });
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_17));

        content.add(StellarLabels.body("Port"));
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_6));
        JTextField portField = themedField(String.valueOf(settings.getPort()));
        portField.setMaximumSize(new Dimension(120, portField.getPreferredSize().height));
        content.add(portField);
        portField.addActionListener(e -> applyPort(portField));
        portField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                applyPort(portField);
            }
        });
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_17));

        StellarButton reenterWizard = new StellarButton("Re-run setup wizard", StellarButton.Variant.SECONDARY);
        reenterWizard.setAlignmentX(Component.LEFT_ALIGNMENT);
        reenterWizard.addActionListener(e -> onReenterWizard.run());
        content.add(reenterWizard);

        card.add(content, BorderLayout.NORTH);
        return card;
    }

    private JComponent buildJavaAndNetworkingCard() {
        StellarPanel card = new StellarPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_17, StellarTheme.SPACE_17,
                StellarTheme.SPACE_17, StellarTheme.SPACE_17));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(StellarLabels.kicker("Java resolution mode"));
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_8));
        javaModeRow.setOpaque(false);
        javaModeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        rebuildJavaModeRow();
        content.add(javaModeRow);
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_17));

        content.add(StellarLabels.kicker("Modloader"));
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_6));
        content.add(StellarLabels.value(settings.getModLoader() == ModLoader.VANILLA
                ? "Vanilla — no modloader"
                : settings.getModLoader().name() + " " + settings.getModLoaderVersion()));
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_6));
        StellarButton modLoaderVersionButton = new StellarButton("Change modloader version", StellarButton.Variant.SECONDARY);
        modLoaderVersionButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        modLoaderVersionButton.setEnabled(settings.getModLoader() != ModLoader.VANILLA);
        modLoaderVersionButton.addActionListener(e -> onChangeModLoaderVersion.run());
        content.add(modLoaderVersionButton);
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_17));

        content.add(StellarLabels.kicker("Networking"));
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_8));

        JPanel upnpRow = new JPanel(new BorderLayout());
        upnpRow.setOpaque(false);
        upnpRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        upnpRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        upnpRow.add(StellarLabels.body("UPnP port forwarding"), BorderLayout.WEST);
        upnpRow.add(new StellarTag(settings.isUsePortForwarded() ? "Enabled" : "Disabled",
                settings.isUsePortForwarded() ? StellarTag.Variant.ACCENT : StellarTag.Variant.OUTLINE), BorderLayout.EAST);
        content.add(upnpRow);
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_6));
        StellarButton upnpButton = new StellarButton("Manage UPnP", StellarButton.Variant.SECONDARY);
        upnpButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        upnpButton.addActionListener(e -> onOpenUpnp.run());
        content.add(upnpButton);
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_11));

        StellarButton firewallButton = new StellarButton("Check firewall", StellarButton.Variant.SECONDARY);
        firewallButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        firewallButton.addActionListener(e -> onCheckFirewall.run());
        content.add(firewallButton);

        card.add(content, BorderLayout.NORTH);
        return card;
    }

    private void rebuildJavaModeRow() {
        javaModeRow.removeAll();
        for (JavaOverrideMode mode : JavaOverrideMode.values()) {
            boolean selected = mode == settings.getJavaOverrideMode();
            StellarButton button = new StellarButton(label(mode),
                    selected ? StellarButton.Variant.PRIMARY : StellarButton.Variant.SECONDARY);
            button.addActionListener(e -> {
                settings.setJavaOverrideMode(mode);
                persist("Java resolution mode set to " + label(mode) + ".");
                rebuildJavaModeRow();
            });
            javaModeRow.add(button);
        }
        javaModeRow.revalidate();
        javaModeRow.repaint();
    }

    private static String label(JavaOverrideMode mode) {
        return switch (mode) {
            case AUTOMATIC -> "Automatic";
            case SYSTEM_PATH -> "System PATH";
            case FORCE_MANAGED -> "Force-managed";
        };
    }

    private void applyPort(JTextField field) {
        String text = field.getText().trim();
        int port;
        try {
            port = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            setStatus("Port must be a number.", StellarTheme.STATUS_FAILED);
            field.setText(String.valueOf(settings.getPort()));
            return;
        }
        if (port < 10000 || port > 65535) {
            setStatus("Port must be between 10000 and 65535.", StellarTheme.STATUS_FAILED);
            field.setText(String.valueOf(settings.getPort()));
            return;
        }
        if (port == settings.getPort()) {
            return;
        }
        settings.setPort(port);
        try {
            ctx.serverPropertiesService.ensureValidAndSynced(port);
        } catch (IOException ex) {
            setStatus("Port saved, but could not sync server.properties: " + ex.getMessage(), StellarTheme.STATUS_WARNING);
            onSettingsChanged.run();
            return;
        }
        persist("Port set to " + port + ".");
    }

    private void persist(String message) {
        try {
            ctx.settingsService.save(settings);
            setStatus(message, StellarTheme.STATUS_RUNNING);
        } catch (IOException ex) {
            setStatus("Could not save settings.json: " + ex.getMessage(), StellarTheme.STATUS_FAILED);
        }
        onSettingsChanged.run();
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private static JTextField themedField(String initial) {
        JTextField field = new JTextField(initial, 10);
        field.setBackground(StellarTheme.FIELD_BG);
        field.setForeground(StellarTheme.TEXT_PRIMARY);
        field.setCaretColor(StellarTheme.ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(StellarTheme.NEUTRAL_800),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        field.setFont(StellarTheme.FONT_BODY);
        return field;
    }
}
