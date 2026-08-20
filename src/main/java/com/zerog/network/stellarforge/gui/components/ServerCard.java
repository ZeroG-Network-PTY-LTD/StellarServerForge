package com.zerog.network.stellarforge.gui.components;

import com.zerog.network.stellarforge.model.ServerProfile;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;

/**
 * Visual card widget representing a single server profile on the dashboard.
 * Supports both quick action buttons and a right-click context menu.
 */
public class ServerCard extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, HH:mm");

    /**
     * Callback interface for card actions.
     * The extra methods ({@link #onBackup}, {@link #onDuplicate}, {@link #onToggleFavorite},
     * {@link #onDelete}) have empty default implementations so existing callers don't break.
     */
    public interface CardAction {
        void onLaunch(ServerProfile profile);
        void onConfigure(ServerProfile profile);

        /** Triggered when the user chooses Backup from the context menu. */
        default void onBackup(ServerProfile profile) {}

        /** Triggered when the user chooses Duplicate from the context menu. */
        default void onDuplicate(ServerProfile profile) {}

        /** Triggered when the user chooses Toggle Favourite from the context menu. */
        default void onToggleFavorite(ServerProfile profile) {}

        /** Triggered when the user chooses Delete from the context menu. */
        default void onDelete(ServerProfile profile) {}
    }

    private final ServerProfile profile;
    private boolean hovered = false;

    public ServerCard(ServerProfile profile, CardAction action) {
        this.profile = profile;

        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(14, 16, 14, 16));
        setBackground(new Color(45, 45, 50));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(260, 130));

        // Parse accent colour from profile
        Color accent = parseColor(profile.getColorCode());

        // ── Left accent bar ───────────────────────────────────────────────
        JPanel accentBar = new JPanel();
        accentBar.setBackground(accent);
        accentBar.setPreferredSize(new Dimension(4, 0));
        add(accentBar, BorderLayout.WEST);

        // ── Main info ─────────────────────────────────────────────────────
        JPanel info = new JPanel(new GridLayout(4, 1, 0, 2));
        info.setOpaque(false);

        JLabel nameLabel = new JLabel(profile.getProfileName() + (profile.isFavorite() ? " ⭐" : ""));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        String mcVer  = profile.getConfig().getMinecraftVersion();
        String loader = profile.getConfig().getModLoader().getDisplayName();
        JLabel versionLabel = new JLabel("MC " + mcVer + "  •  " + loader);
        versionLabel.setForeground(new Color(170, 170, 180));
        versionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

        int ram  = profile.getConfig().getMaxRamGb();
        int port = profile.getConfig().getPort();
        JLabel detailsLabel = new JLabel(ram + " GB RAM  •  Port " + port);
        detailsLabel.setForeground(new Color(130, 130, 140));
        detailsLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));

        String used = profile.getLastUsed() != null
                ? "Last used " + profile.getLastUsed().format(FMT)
                : "Never used";
        JLabel lastUsedLabel = new JLabel(used);
        lastUsedLabel.setForeground(new Color(100, 100, 110));
        lastUsedLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 10));

        info.add(nameLabel);
        info.add(versionLabel);
        info.add(detailsLabel);
        info.add(lastUsedLabel);

        // ── Buttons ───────────────────────────────────────────────────────
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);

        JButton launchBtn = makeButton("▶", new Color(39, 174, 96));
        launchBtn.setToolTipText("Launch server");
        launchBtn.addActionListener(e -> action.onLaunch(profile));

        JButton configBtn = makeButton("⚙", new Color(52, 152, 219));
        configBtn.setToolTipText("Configure server");
        configBtn.addActionListener(e -> action.onConfigure(profile));

        buttons.add(configBtn);
        buttons.add(launchBtn);

        add(info, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        // ── Hover highlight + right-click context menu ────────────────────
        JPopupMenu contextMenu = buildContextMenu(profile, action);
        setComponentPopupMenu(contextMenu);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                hovered = true;
                setBackground(new Color(55, 55, 62));
                repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                hovered = false;
                setBackground(new Color(45, 45, 50));
                repaint();
            }
            @Override public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) contextMenu.show(ServerCard.this, e.getX(), e.getY());
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) contextMenu.show(ServerCard.this, e.getX(), e.getY());
            }
        });
    }

    // ── Context menu ──────────────────────────────────────────────────────────

    private JPopupMenu buildContextMenu(ServerProfile profile, CardAction action) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem launchItem     = new JMenuItem("▶  Launch Server");
        JMenuItem configItem     = new JMenuItem("⚙  Configure");
        JMenuItem backupItem     = new JMenuItem("💾  Backup World");
        JMenuItem duplicateItem  = new JMenuItem("📋  Duplicate Profile");
        JMenuItem favoriteItem   = new JMenuItem(profile.isFavorite()
                ? "★  Remove from Favourites" : "☆  Add to Favourites");
        JMenuItem deleteItem     = new JMenuItem("🗑  Delete Profile");

        styleMenuItem(launchItem,    new Color(39, 174, 96));
        styleMenuItem(configItem,    null);
        styleMenuItem(backupItem,    null);
        styleMenuItem(duplicateItem, null);
        styleMenuItem(favoriteItem,  new Color(180, 140, 0));
        styleMenuItem(deleteItem,    new Color(192, 57, 43));

        launchItem.addActionListener(e     -> action.onLaunch(profile));
        configItem.addActionListener(e     -> action.onConfigure(profile));
        backupItem.addActionListener(e     -> action.onBackup(profile));
        duplicateItem.addActionListener(e  -> action.onDuplicate(profile));
        favoriteItem.addActionListener(e   -> action.onToggleFavorite(profile));
        deleteItem.addActionListener(e     -> action.onDelete(profile));

        menu.add(launchItem);
        menu.add(configItem);
        menu.addSeparator();
        menu.add(backupItem);
        menu.add(duplicateItem);
        menu.add(favoriteItem);
        menu.addSeparator();
        menu.add(deleteItem);
        return menu;
    }

    private void styleMenuItem(JMenuItem item, Color fg) {
        item.setBackground(new Color(45, 45, 52));
        item.setForeground(fg != null ? fg : new Color(210, 210, 225));
        item.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        btn.setPreferredSize(new Dimension(32, 26));
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (hovered) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(255, 255, 255, 10));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private static Color parseColor(String hex) {
        try {
            if (hex != null && hex.startsWith("#")) {
                return Color.decode(hex);
            }
        } catch (Exception ignored) {}
        return new Color(41, 128, 185); // default blue
    }
}

