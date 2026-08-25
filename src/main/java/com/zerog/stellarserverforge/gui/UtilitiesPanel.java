package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.gui.theme.StellarButton;
import com.zerog.stellarserverforge.gui.theme.StellarLabels;
import com.zerog.stellarserverforge.gui.theme.StellarPanel;
import com.zerog.stellarserverforge.gui.theme.StellarTheme;
import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ServerSettings;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Utility features (spec §13): icon generation, server-pack ZIP export, run.sh/run.bat
 * generation, and the cache purge function. A real in-window screen (reached from the dashboard's
 * "Utilities" nav link/toolbar button) rather than a separate modal dialog.
 */
public class UtilitiesPanel extends JPanel {

    private static final List<String> TAB_NAMES = List.of("Icon", "Server Pack ZIP", "Run Scripts", "Purge Cache");

    private final AppContext ctx;
    private final ServerSettings settings;
    private final JTextArea logArea = new JTextArea(10, 60);
    private final CardLayout tabCardLayout = new CardLayout();
    private final JPanel tabContent = new JPanel(tabCardLayout);
    private final JPanel tabStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_6, 0));
    private String activeTab = TAB_NAMES.get(0);

    public UtilitiesPanel(AppContext ctx, ServerSettings settings, Runnable onBack) {
        this.ctx = ctx;
        this.settings = settings;

        setOpaque(false);
        setLayout(new BorderLayout(0, StellarTheme.SPACE_17));
        setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_22, StellarTheme.SPACE_22,
                StellarTheme.SPACE_22, StellarTheme.SPACE_22));

        JPanel header = new JPanel(new BorderLayout(StellarTheme.SPACE_11, 0));
        header.setOpaque(false);
        StellarButton back = new StellarButton("Back", StellarButton.Variant.GHOST);
        back.addActionListener(e -> onBack.run());
        header.add(back, BorderLayout.WEST);
        header.add(StellarLabels.title("Utilities"), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        StellarPanel body = new StellarPanel(new BorderLayout(0, StellarTheme.SPACE_11));
        body.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_11, StellarTheme.SPACE_11,
                StellarTheme.SPACE_11, StellarTheme.SPACE_11));

        tabContent.setOpaque(false);
        tabContent.add(buildIconTab(), "Icon");
        tabContent.add(buildZipTab(), "Server Pack ZIP");
        tabContent.add(buildRunScriptsTab(), "Run Scripts");
        tabContent.add(buildPurgeTab(), "Purge Cache");

        tabStrip.setOpaque(false);
        rebuildTabStrip();

        JPanel tabArea = new JPanel(new BorderLayout(0, StellarTheme.SPACE_11));
        tabArea.setOpaque(false);
        tabArea.add(tabStrip, BorderLayout.NORTH);
        tabArea.add(tabContent, BorderLayout.CENTER);
        body.add(tabArea, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setBackground(StellarTheme.CONSOLE_BG);
        logArea.setForeground(StellarTheme.NEUTRAL_100);
        logArea.setFont(StellarTheme.FONT_MONO);
        logArea.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_11, StellarTheme.SPACE_11,
                StellarTheme.SPACE_11, StellarTheme.SPACE_11));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        body.add(scroll, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
    }

    private void rebuildTabStrip() {
        tabStrip.removeAll();
        for (String name : TAB_NAMES) {
            boolean selected = name.equals(activeTab);
            StellarButton tabButton = new StellarButton(name,
                    selected ? StellarButton.Variant.PRIMARY : StellarButton.Variant.SECONDARY);
            tabButton.addActionListener(e -> {
                activeTab = name;
                tabCardLayout.show(tabContent, name);
                rebuildTabStrip();
            });
            tabStrip.add(tabButton);
        }
        tabStrip.revalidate();
        tabStrip.repaint();
    }

    private void log(String message) {
        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private JPanel tabPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setOpaque(false);
        return panel;
    }

    private JPanel buildIconTab() {
        JPanel panel = tabPanel();
        StellarButton defaultButton = new StellarButton("Generate default icon", StellarButton.Variant.PRIMARY);
        StellarButton customButton = new StellarButton("Generate custom icon", StellarButton.Variant.SECONDARY);
        panel.add(defaultButton);
        panel.add(customButton);

        defaultButton.addActionListener(e -> {
            try {
                ctx.iconGeneratorService.generateDefault(ctx.serverDir);
                log("Generated default server-icon.png.");
            } catch (IOException ex) {
                log("Failed to generate icon: " + ex.getMessage());
            }
        });

        customButton.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            Color bg = JColorChooser.showDialog(owner, "Background Color", Color.BLUE);
            if (bg == null) {
                return;
            }
            Color text = JColorChooser.showDialog(owner, "Text Color", Color.YELLOW);
            if (text == null) {
                return;
            }
            String customText = JOptionPane.showInputDialog(owner, "Custom text (max 10 characters, optional):");
            try {
                ctx.iconGeneratorService.generateCustom(ctx.serverDir, bg, text, customText);
                log("Generated custom server-icon.png.");
            } catch (IOException ex) {
                log("Failed to generate icon: " + ex.getMessage());
            }
        });
        return panel;
    }

    private JPanel buildZipTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        DefaultListModel<String> model = new DefaultListModel<>();
        ctx.serverPackZipService.defaultCandidates(ctx.serverDir).forEach(model::addElement);
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setBackground(StellarTheme.SURFACE);
        list.setForeground(StellarTheme.TEXT_PRIMARY);
        for (int i = 0; i < model.size(); i++) {
            list.addSelectionInterval(i, i);
        }
        panel.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.setOpaque(false);
        JTextField nameField = new JTextField(settings.getMinecraftVersion() + "-server-pack", 20);
        StellarButton zipButton = new StellarButton("Export server pack", StellarButton.Variant.PRIMARY);
        row.add(StellarLabels.body("File name:"));
        row.add(nameField);
        row.add(zipButton);
        panel.add(row, BorderLayout.SOUTH);

        zipButton.addActionListener(e -> {
            List<String> selected = list.getSelectedValuesList();
            try {
                var zipPath = ctx.serverPackZipService.createZip(ctx.serverDir, selected, nameField.getText().trim());
                log("Created " + zipPath.getFileName());
            } catch (IOException ex) {
                log("Failed to create ZIP: " + ex.getMessage());
            }
        });
        return panel;
    }

    private JPanel buildRunScriptsTab() {
        JPanel panel = tabPanel();
        StellarButton generateButton = new StellarButton("Generate run.sh / run.bat", StellarButton.Variant.PRIMARY);
        panel.add(generateButton);
        panel.add(StellarLabels.muted("(Forge/NeoForge only; launch the server at least once first.)"));

        generateButton.addActionListener(e -> {
            try {
                McVersion mc = McVersion.parse(settings.getMinecraftVersion());
                ctx.runScriptGeneratorService.generate(settings, mc, ctx.serverDir);
                log("Generated run.sh and run.bat.");
            } catch (IOException ex) {
                log("Failed to generate run scripts: " + ex.getMessage());
            }
        });
        return panel;
    }

    private JPanel buildPurgeTab() {
        JPanel panel = tabPanel();
        panel.add(StellarLabels.muted("Deletes the installed server jar, modloader libraries, cached installers, "
                + "managed Java, and downloaded metadata — forces a full re-download/reinstall on the next Launch "
                + "(never touches mods/config/world saves/settings)."));
        StellarButton purgeButton = new StellarButton("Purge", StellarButton.Variant.DANGER);
        panel.add(purgeButton);

        purgeButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this),
                    "This deletes the installed server jar, modloader libraries, and cached "
                            + "modloader/Java downloads for this server — the next Launch will need to "
                            + "re-download and reinstall everything, which can take a while. "
                            + "Mods/config/world saves/settings are not touched. Continue?",
                    "Confirm purge", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                ctx.purgeService.purge(ctx.serverDir, ctx.cacheDir);
                log("Purge complete.");
            } catch (IOException ex) {
                log("Purge failed: " + ex.getMessage());
            }
        });
        return panel;
    }
}
