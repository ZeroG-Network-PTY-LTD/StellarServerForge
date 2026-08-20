package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ServerSettings;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Utility features (spec §13): icon generation, server-pack ZIP export, run.sh/run.bat
 * generation, and the cache purge function.
 */
public class UtilitiesDialog extends JDialog {

    private final AppContext ctx;
    private final ServerSettings settings;
    private final JTextArea logArea = new JTextArea(10, 60);

    public UtilitiesDialog(Frame owner, AppContext ctx, ServerSettings settings) {
        super(owner, "Utilities", true);
        this.ctx = ctx;
        this.settings = settings;

        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Icon", buildIconTab());
        tabs.addTab("Server Pack ZIP", buildZipTab());
        tabs.addTab("Run Scripts", buildRunScriptsTab());
        tabs.addTab("Purge Cache", buildPurgeTab());
        add(tabs, BorderLayout.NORTH);

        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(closeButton);
        add(south, BorderLayout.SOUTH);

        setSize(640, 480);
        setLocationRelativeTo(owner);
    }

    private void log(String message) {
        logArea.append(message + System.lineSeparator());
    }

    private JPanel buildIconTab() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton defaultButton = new JButton("Generate Default Icon");
        JButton customButton = new JButton("Generate Custom Icon...");
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
            Color bg = JColorChooser.showDialog(this, "Background Color", Color.BLUE);
            if (bg == null) {
                return;
            }
            Color text = JColorChooser.showDialog(this, "Text Color", Color.YELLOW);
            if (text == null) {
                return;
            }
            String customText = JOptionPane.showInputDialog(this, "Custom text (max 10 characters, optional):");
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
        DefaultListModel<String> model = new DefaultListModel<>();
        ctx.serverPackZipService.defaultCandidates(ctx.serverDir).forEach(model::addElement);
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        for (int i = 0; i < model.size(); i++) {
            list.addSelectionInterval(i, i);
        }
        panel.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField nameField = new JTextField(settings.getMinecraftVersion() + "-server-pack", 20);
        JButton zipButton = new JButton("Create ZIP");
        row.add(new JLabel("File name:"));
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
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton generateButton = new JButton("Generate run.sh / run.bat");
        panel.add(generateButton);
        panel.add(new JLabel("(Forge/NeoForge only; launch the server at least once first.)"));

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
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Deletes cached modloader installers, managed Java, and downloaded metadata "
                + "(never touches mods/config/world saves/settings)."));
        JButton purgeButton = new JButton("Purge");
        panel.add(purgeButton);

        purgeButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "This deletes cached modloader/Java downloads for this server. Continue?",
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
