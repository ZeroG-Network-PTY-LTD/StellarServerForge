package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.model.ServerSettings;
import com.zerog.stellarserverforge.net.IpLookupService;
import org.bitlet.weupnp.GatewayDevice;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * UPnP port-forwarding submenu (spec §3.3), backed by a native Java UPnP client instead of the
 * bat script's bundled {@code Portforwarded.Server.exe} .NET helper.
 */
public class UpnpDialog extends JDialog {

    private final AppContext ctx;
    private final ServerSettings settings;
    private final Runnable onSettingsChanged;

    private final JLabel statusLabel = new JLabel("Checking your connection...");
    private final JLabel protocolLabel = new JLabel();
    private final JButton toggleProtocolButton = new JButton("Cycle Protocol (TCP/BOTH/UDP)");
    private final JButton activateButton = new JButton("Activate UPnP");
    private final JButton deactivateButton = new JButton("Deactivate UPnP");
    private final JButton closeButton = new JButton("Close");

    public UpnpDialog(Frame owner, AppContext ctx, ServerSettings settings, Runnable onSettingsChanged) {
        super(owner, "UPnP Port Forwarding", true);
        this.ctx = ctx;
        this.settings = settings;
        this.onSettingsChanged = onSettingsChanged;

        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(statusLabel);
        center.add(protocolLabel);
        add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(toggleProtocolButton);
        buttons.add(activateButton);
        buttons.add(deactivateButton);
        buttons.add(closeButton);
        add(buttons, BorderLayout.SOUTH);

        toggleProtocolButton.addActionListener(e -> cycleProtocol());
        activateButton.addActionListener(e -> activate());
        deactivateButton.addActionListener(e -> deactivate());
        closeButton.addActionListener(e -> dispose());

        setSize(480, 220);
        setLocationRelativeTo(owner);
        setButtonsEnabled(false);
        refreshProtocolLabel();
        checkCgnat();
    }

    private void refreshProtocolLabel() {
        protocolLabel.setText("Protocol: " + settings.getProtocol()
                + " | UPnP currently: " + (settings.isUsePortForwarded() ? "ACTIVE" : "inactive"));
    }

    private void setButtonsEnabled(boolean enabled) {
        toggleProtocolButton.setEnabled(enabled);
        activateButton.setEnabled(enabled);
        deactivateButton.setEnabled(enabled);
    }

    private void checkCgnat() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return ctx.ipLookupService.fetchPublicIp();
            }

            @Override
            protected void done() {
                try {
                    String publicIp = get();
                    if (publicIp != null && IpLookupService.isCgnat(publicIp)) {
                        statusLabel.setText("Your public IP (" + publicIp + ") is behind Carrier-Grade NAT — "
                                + "UPnP cannot work on this network. Consider a service like playit.gg instead.");
                        setButtonsEnabled(false);
                    } else {
                        statusLabel.setText(publicIp != null ? "Public IP: " + publicIp : "Ready.");
                        setButtonsEnabled(true);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Could not determine your public IP — proceeding anyway.");
                    setButtonsEnabled(true);
                }
            }
        }.execute();
    }

    private void cycleProtocol() {
        settings.setProtocol(switch (settings.getProtocol()) {
            case "TCP" -> "BOTH";
            case "BOTH" -> "UDP";
            default -> "TCP";
        });
        persist();
        refreshProtocolLabel();
    }

    private void activate() {
        setButtonsEnabled(false);
        statusLabel.setText("Discovering your router (UPnP)...");

        new SwingWorker<Void, Void>() {
            private String error;

            @Override
            protected Void doInBackground() {
                try {
                    GatewayDevice gateway = ctx.upnpService.discoverGateway();
                    if (gateway == null) {
                        error = "No UPnP-capable router was found on this network.";
                        return null;
                    }
                    String localIp = gateway.getLocalAddress().getHostAddress();
                    if (!"UDP".equals(settings.getProtocol())) {
                        ctx.upnpService.addMapping(gateway, "TCP", settings.getPort(), settings.getPort(), localIp,
                                "StellarServerForge");
                    }
                    if (!"TCP".equals(settings.getProtocol())) {
                        ctx.upnpService.addMapping(gateway, "UDP", settings.getPortUdp(), settings.getPortUdp(), localIp,
                                "StellarServerForge");
                    }
                } catch (IOException e) {
                    error = e.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                if (error != null) {
                    statusLabel.setText("Activation failed: " + error);
                    return;
                }
                settings.setUsePortForwarded(true);
                persist();
                refreshProtocolLabel();
                statusLabel.setText("UPnP forwarding activated.");
            }
        }.execute();
    }

    private void deactivate() {
        setButtonsEnabled(false);
        statusLabel.setText("Removing port mapping(s)...");

        new SwingWorker<Void, Void>() {
            private String error;

            @Override
            protected Void doInBackground() {
                try {
                    GatewayDevice gateway = ctx.upnpService.discoverGateway();
                    if (gateway != null) {
                        if (!"UDP".equals(settings.getProtocol())) {
                            ctx.upnpService.removeMapping(gateway, "TCP", settings.getPort());
                        }
                        if (!"TCP".equals(settings.getProtocol())) {
                            ctx.upnpService.removeMapping(gateway, "UDP", settings.getPortUdp());
                        }
                    }
                } catch (IOException e) {
                    error = e.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                settings.setUsePortForwarded(false);
                persist();
                refreshProtocolLabel();
                statusLabel.setText(error != null
                        ? "Marked inactive locally; router removal reported: " + error
                        : "UPnP forwarding deactivated.");
            }
        }.execute();
    }

    private void persist() {
        try {
            ctx.settingsService.save(settings);
        } catch (IOException e) {
            statusLabel.setText("Warning: could not save settings.json: " + e.getMessage());
        }
        onSettingsChanged.run();
    }
}
