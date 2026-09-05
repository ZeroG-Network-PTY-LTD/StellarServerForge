package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.gui.theme.StellarButton;
import com.zerog.stellarserverforge.gui.theme.StellarLabels;
import com.zerog.stellarserverforge.gui.theme.StellarPanel;
import com.zerog.stellarserverforge.gui.theme.StellarTheme;
import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.model.ServerSettings;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final JLabel iconPreview = new JLabel();
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

        StellarButton openFolder = new StellarButton("Open server folder", StellarButton.Variant.SECONDARY);
        openFolder.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(ctx.serverDir.toFile());
            } catch (IOException | UnsupportedOperationException ex) {
                log("Could not open the server folder: " + ex.getMessage());
            }
        });
        header.add(openFolder, BorderLayout.EAST);
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

        body.add(buildLogArea(), BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);

        refreshIconPreview();
    }

    private JComponent buildLogArea() {
        JPanel wrap = new JPanel(new BorderLayout(0, StellarTheme.SPACE_6));
        wrap.setOpaque(false);

        JPanel logHeader = new JPanel(new BorderLayout());
        logHeader.setOpaque(false);
        logHeader.add(StellarLabels.kicker("OUTPUT"), BorderLayout.WEST);
        StellarButton clearButton = new StellarButton("Clear", StellarButton.Variant.SECONDARY);
        clearButton.addActionListener(e -> logArea.setText(""));
        JPanel clearWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        clearWrap.setOpaque(false);
        clearWrap.add(clearButton);
        logHeader.add(clearWrap, BorderLayout.EAST);
        wrap.add(logHeader, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setBackground(StellarTheme.CONSOLE_BG);
        logArea.setForeground(StellarTheme.NEUTRAL_100);
        logArea.setFont(StellarTheme.FONT_MONO);
        logArea.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_11, StellarTheme.SPACE_11,
                StellarTheme.SPACE_11, StellarTheme.SPACE_11));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(StellarTheme.CONSOLE_BG);
        wrap.add(scroll, BorderLayout.CENTER);
        return wrap;
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

    /** A card with a kicker heading and vertically-stacked content, used for every tab so they all
     * share the same padding/spacing instead of each tab hand-rolling its own layout. */
    private StellarPanel tabCard(String kicker) {
        StellarPanel card = new StellarPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(StellarTheme.SPACE_17, StellarTheme.SPACE_17,
                StellarTheme.SPACE_17, StellarTheme.SPACE_17));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(StellarLabels.kicker(kicker));
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_11));
        card.add(content, BorderLayout.NORTH);
        return card;
    }

    private JComponent contentOf(StellarPanel card) {
        return (JComponent) ((BorderLayout) card.getLayout()).getLayoutComponent(BorderLayout.NORTH);
    }

    /** Wrapping body text — a plain {@code JLabel} does not wrap, so long descriptive/warning copy
     * (e.g. the purge warning) would otherwise silently run off the edge of the panel. */
    private JTextArea wrapText(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(StellarTheme.FONT_CAPTION);
        area.setForeground(StellarTheme.TEXT_SECONDARY);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMaximumSize(new Dimension(520, Integer.MAX_VALUE));
        return area;
    }

    private JPanel row(Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_8, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component c : components) {
            panel.add(c);
        }
        return panel;
    }

    private JComponent buildIconTab() {
        StellarPanel card = tabCard("SERVER ICON");
        JComponent content = contentOf(card);

        content.add(wrapText("Generates server-icon.png (64x64), shown in the multiplayer server list. "
                + "If one already exists, it's kept as a numbered backup rather than overwritten."));
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_11));

        iconPreview.setPreferredSize(new Dimension(96, 96));
        iconPreview.setHorizontalAlignment(SwingConstants.CENTER);
        iconPreview.setBorder(BorderFactory.createLineBorder(StellarTheme.NEUTRAL_800));
        iconPreview.setForeground(StellarTheme.TEXT_MUTED);
        iconPreview.setFont(StellarTheme.FONT_CAPTION);

        StellarButton defaultButton = new StellarButton("Generate default icon", StellarButton.Variant.PRIMARY);
        StellarButton customButton = new StellarButton("Design custom icon...", StellarButton.Variant.SECONDARY);
        StellarButton importButton = new StellarButton("Import from file...", StellarButton.Variant.SECONDARY);

        JPanel buttonsColumn = new JPanel();
        buttonsColumn.setOpaque(false);
        buttonsColumn.setLayout(new BoxLayout(buttonsColumn, BoxLayout.Y_AXIS));
        defaultButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        customButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        importButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonsColumn.add(defaultButton);
        buttonsColumn.add(Box.createVerticalStrut(StellarTheme.SPACE_8));
        buttonsColumn.add(customButton);
        buttonsColumn.add(Box.createVerticalStrut(StellarTheme.SPACE_8));
        buttonsColumn.add(importButton);

        JPanel previewAndButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, StellarTheme.SPACE_17, 0));
        previewAndButtons.setOpaque(false);
        previewAndButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewAndButtons.add(iconPreview);
        previewAndButtons.add(buttonsColumn);
        content.add(previewAndButtons);

        defaultButton.addActionListener(e -> {
            try {
                ctx.iconGeneratorService.generateDefault(ctx.serverDir);
                log("Generated default server-icon.png.");
                refreshIconPreview();
            } catch (IOException ex) {
                log("Failed to generate icon: " + ex.getMessage());
            }
        });

        customButton.addActionListener(e -> new IconDesignerDialog(ownerFrame(), ctx.iconGeneratorService,
                ctx.serverDir, message -> {
            log(message);
            refreshIconPreview();
        }).setVisible(true));

        importButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose an image for the server icon");
            chooser.setFileFilter(new FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "bmp", "gif"));
            if (chooser.showOpenDialog(ownerFrame()) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            Path chosen = chooser.getSelectedFile().toPath();
            try {
                ctx.iconGeneratorService.importFromFile(ctx.serverDir, chosen);
                log("Imported " + chosen.getFileName() + " as server-icon.png.");
                refreshIconPreview();
            } catch (IOException ex) {
                log("Failed to import icon: " + ex.getMessage());
            }
        });

        return card;
    }

    private Frame ownerFrame() {
        Window window = SwingUtilities.getWindowAncestor(this);
        return window instanceof Frame ? (Frame) window : null;
    }

    private void refreshIconPreview() {
        Path iconPath = ctx.serverDir.resolve("server-icon.png");
        if (!Files.isRegularFile(iconPath)) {
            iconPreview.setIcon(null);
            iconPreview.setText("<html><center>No icon<br>yet</center></html>");
            return;
        }
        try {
            Image image = ImageIO.read(iconPath.toFile());
            if (image == null) {
                throw new IOException("unreadable image");
            }
            iconPreview.setText(null);
            iconPreview.setIcon(new ImageIcon(image.getScaledInstance(96, 96, Image.SCALE_SMOOTH)));
        } catch (IOException ex) {
            iconPreview.setIcon(null);
            iconPreview.setText("<html><center>Preview<br>unavailable</center></html>");
        }
    }

    private JComponent buildZipTab() {
        StellarPanel card = tabCard("SERVER PACK CONTENTS");
        JComponent content = contentOf(card);

        content.add(wrapText("Packages the selected files/folders into a distributable ZIP, alongside a short "
                + "readme. Server jars, logs, and cache/library folders are never included."));
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_8));

        DefaultListModel<String> model = new DefaultListModel<>();
        ctx.serverPackZipService.defaultCandidates(ctx.serverDir).forEach(model::addElement);
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setBackground(StellarTheme.SURFACE);
        list.setForeground(StellarTheme.TEXT_PRIMARY);
        for (int i = 0; i < model.size(); i++) {
            list.addSelectionInterval(i, i);
        }
        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        listScroll.setPreferredSize(new Dimension(360, 140));
        listScroll.setMaximumSize(new Dimension(520, 200));
        content.add(listScroll);
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_8));

        StellarButton selectAll = new StellarButton("Select all", StellarButton.Variant.SECONDARY);
        StellarButton selectNone = new StellarButton("Select none", StellarButton.Variant.SECONDARY);
        selectAll.addActionListener(e -> list.setSelectionInterval(0, model.size() - 1));
        selectNone.addActionListener(e -> list.clearSelection());
        content.add(row(selectAll, selectNone));
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_11));

        JTextField nameField = new JTextField(settings.getMinecraftVersion() + "-server-pack", 20);
        StellarButton zipButton = new StellarButton("Export server pack", StellarButton.Variant.PRIMARY);
        content.add(row(StellarLabels.body("File name:"), nameField, zipButton));

        zipButton.addActionListener(e -> {
            List<String> selected = list.getSelectedValuesList();
            if (selected.isEmpty()) {
                log("Select at least one item to include before exporting.");
                return;
            }
            Path prospective = ctx.serverDir.resolve(nameField.getText().trim() + ".zip");
            if (Files.exists(prospective)) {
                int overwrite = JOptionPane.showConfirmDialog(ownerFrame(),
                        prospective.getFileName() + " already exists — overwrite it?",
                        "Overwrite file?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (overwrite != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            try {
                var zipPath = ctx.serverPackZipService.createZip(ctx.serverDir, selected, nameField.getText().trim());
                log("Created " + zipPath.getFileName());
            } catch (IOException ex) {
                log("Failed to create ZIP: " + ex.getMessage());
            }
        });
        return card;
    }

    private JComponent buildRunScriptsTab() {
        StellarPanel card = tabCard("RUN SCRIPTS");
        JComponent content = contentOf(card);

        content.add(wrapText("Generates run.sh and run.bat next to the server jar, for Forge/NeoForge "
                + "installs that use an @args-file launch line. Launch the server at least once first, so "
                + "the installed loader's launch arguments are known."));
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_11));

        boolean supported = settings.getModLoader() == ModLoader.FORGE || settings.getModLoader() == ModLoader.NEOFORGE;
        StellarButton generateButton = new StellarButton("Generate run.sh / run.bat", StellarButton.Variant.PRIMARY);
        generateButton.setEnabled(supported);
        content.add(row(generateButton));
        if (!supported) {
            content.add(Box.createVerticalStrut(StellarTheme.SPACE_8));
            content.add(wrapText("Current modloader is " + settings.getModLoader()
                    + " — run scripts are only generated for Forge/NeoForge installs."));
        }

        generateButton.addActionListener(e -> {
            try {
                McVersion mc = McVersion.parse(settings.getMinecraftVersion());
                ctx.runScriptGeneratorService.generate(settings, mc, ctx.serverDir);
                log("Generated run.sh and run.bat.");
            } catch (IOException ex) {
                log("Failed to generate run scripts: " + ex.getMessage());
            }
        });
        return card;
    }

    private JComponent buildPurgeTab() {
        StellarPanel card = tabCard("PURGE CACHE");
        JComponent content = contentOf(card);

        content.add(wrapText("Deletes the installed server jar, modloader libraries, cached installers, "
                + "managed Java, and downloaded metadata — forces a full re-download/reinstall on the next "
                + "Launch. Mods, config, world saves, and settings are never touched."));
        content.add(Box.createVerticalStrut(StellarTheme.SPACE_11));

        StellarButton purgeButton = new StellarButton("Purge", StellarButton.Variant.DANGER);
        JLabel statusLabel = StellarLabels.muted("");
        content.add(row(purgeButton, statusLabel));

        Runnable refreshPurgeState = () -> {
            boolean purgeable = hasPurgeableFiles();
            purgeButton.setEnabled(purgeable);
            statusLabel.setText(purgeable ? "" : "Nothing installed yet — nothing to purge.");
        };
        refreshPurgeState.run();

        purgeButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(ownerFrame(),
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
                refreshPurgeState.run();
            } catch (IOException ex) {
                log("Purge failed: " + ex.getMessage());
            }
        });
        return card;
    }

    /** Mirrors what {@code PurgeService.purge} actually targets, so the button can be disabled
     * (with an explanatory message) instead of running a no-op delete pass and reporting "Purge
     * complete" when nothing was ever installed. */
    private boolean hasPurgeableFiles() {
        try {
            if (Files.isDirectory(ctx.serverDir)) {
                try (var stream = Files.newDirectoryStream(ctx.serverDir, "*.jar")) {
                    if (stream.iterator().hasNext()) {
                        return true;
                    }
                }
            }
            if (Files.exists(ctx.serverDir.resolve("libraries")) || Files.exists(ctx.serverDir.resolve(".fabric"))) {
                return true;
            }
            return Files.exists(ctx.cacheDir.resolve("installers")) || Files.exists(ctx.cacheDir.resolve("java"))
                    || Files.exists(ctx.cacheDir.resolve("versions"));
        } catch (IOException ex) {
            return true; // Unknown state — don't hide the button on a filesystem error.
        }
    }
}
