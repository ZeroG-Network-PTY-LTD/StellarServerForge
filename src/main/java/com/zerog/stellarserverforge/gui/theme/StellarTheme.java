package com.zerog.stellarserverforge.gui.theme;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Color;
import java.awt.Font;

/**
 * The "Stellar" galaxy color palette and global Look &amp; Feel setup, shared by every screen so
 * the app reads as one deliberately designed thing rather than default grey Swing.
 */
public final class StellarTheme {

    private StellarTheme() {
    }

    // Core palette — deep space navy/violet base with cyan/gold stellar accents.
    public static final Color VOID_BLACK = new Color(0x05, 0x06, 0x12);
    public static final Color DEEP_SPACE = new Color(0x0B, 0x0E, 0x24);
    public static final Color NEBULA_PURPLE = new Color(0x2A, 0x1B, 0x4D);
    public static final Color NEBULA_BLUE = new Color(0x14, 0x1F, 0x52);

    public static final Color PANEL_BG = new Color(0x12, 0x14, 0x2B, 235);
    public static final Color PANEL_BG_SOLID = new Color(0x11, 0x13, 0x28);
    public static final Color PANEL_BORDER = new Color(0x3A, 0x3F, 0x7A);

    public static final Color STAR_CYAN = new Color(0x5E, 0xE7, 0xFF);
    public static final Color STAR_CYAN_DIM = new Color(0x2E, 0x8F, 0xA8);
    public static final Color STELLAR_GOLD = new Color(0xF5, 0xC5, 0x4B);
    public static final Color NOVA_PINK = new Color(0xE0, 0x63, 0xC7);
    public static final Color SUCCESS_GREEN = new Color(0x5C, 0xE6, 0x8A);
    public static final Color WARNING_ORANGE = new Color(0xF5, 0x9B, 0x4B);
    public static final Color ERROR_RED = new Color(0xFF, 0x6B, 0x7A);

    public static final Color TEXT_PRIMARY = new Color(0xE8, 0xEC, 0xFF);
    public static final Color TEXT_SECONDARY = new Color(0x9A, 0xA3, 0xD9);
    public static final Color TEXT_MUTED = new Color(0x6B, 0x72, 0xA3);

    public static final Font FONT_TITLE = new Font("Segoe UI Semibold", Font.BOLD, 24);
    public static final Font FONT_HEADING = new Font("Segoe UI Semibold", Font.BOLD, 15);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 12);

    /** Installs Nimbus as the base L&F (which respects UIManager color overrides far better than
     * native L&Fs), then repaints its defaults to the Stellar palette. Call once, before any
     * window is constructed. */
    public static void install() {
        try {
            for (var info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Fall back to whatever default L&F is active; theme colors below still apply where possible.
        }

        UIManager.put("control", PANEL_BG_SOLID);
        UIManager.put("info", PANEL_BG_SOLID);
        UIManager.put("nimbusBase", NEBULA_BLUE);
        UIManager.put("nimbusBlueGrey", new Color(0x2B, 0x2F, 0x55));
        UIManager.put("nimbusLightBackground", DEEP_SPACE);
        UIManager.put("nimbusFocus", STAR_CYAN);
        UIManager.put("nimbusSelectionBackground", new Color(0x33, 0x3E, 0x8A));
        UIManager.put("nimbusSelectedText", TEXT_PRIMARY);
        UIManager.put("nimbusDisabledText", TEXT_MUTED);
        UIManager.put("nimbusBorder", PANEL_BORDER);
        UIManager.put("nimbusGreen", SUCCESS_GREEN);
        UIManager.put("nimbusRed", ERROR_RED);
        UIManager.put("nimbusOrange", WARNING_ORANGE);
        UIManager.put("nimbusAlertYellow", STELLAR_GOLD);
        UIManager.put("text", TEXT_PRIMARY);
        UIManager.put("textForeground", TEXT_PRIMARY);
        UIManager.put("textBackground", DEEP_SPACE);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("Panel.background", PANEL_BG_SOLID);
        UIManager.put("OptionPane.background", PANEL_BG_SOLID);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("TitledBorder.titleColor", STAR_CYAN);
        UIManager.put("TextField.font", FONT_BODY);
        UIManager.put("TextArea.font", FONT_MONO);
        UIManager.put("TextArea.background", VOID_BLACK);
        UIManager.put("TextArea.foreground", STAR_CYAN);
        UIManager.put("TextArea.caretForeground", STAR_CYAN);
        UIManager.put("List.background", DEEP_SPACE);
        UIManager.put("List.foreground", TEXT_PRIMARY);
        UIManager.put("List.selectionBackground", new Color(0x33, 0x3E, 0x8A));
        UIManager.put("List.selectionForeground", TEXT_PRIMARY);
        UIManager.put("ScrollPane.background", DEEP_SPACE);
        UIManager.put("Viewport.background", DEEP_SPACE);
        UIManager.put("TabbedPane.selected", NEBULA_BLUE);
        UIManager.put("TabbedPane.background", PANEL_BG_SOLID);
        UIManager.put("TabbedPane.foreground", TEXT_PRIMARY);
        UIManager.put("CheckBox.font", FONT_BODY);
        UIManager.put("Button.font", FONT_LABEL);

        UIManager.put("Spinner.background", VOID_BLACK);
        UIManager.put("Spinner.foreground", TEXT_PRIMARY);
        UIManager.put("Spinner.font", FONT_BODY);
        UIManager.put("FormattedTextField.background", VOID_BLACK);
        UIManager.put("FormattedTextField.foreground", STAR_CYAN);
        UIManager.put("FormattedTextField.caretForeground", STAR_CYAN);

        UIManager.put("ComboBox.background", VOID_BLACK);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", new Color(0x33, 0x3E, 0x8A));
        UIManager.put("ComboBox.selectionForeground", TEXT_PRIMARY);
        UIManager.put("ComboBox.font", FONT_BODY);

        UIManager.put("ToolTip.background", PANEL_BG_SOLID);
        UIManager.put("ToolTip.foreground", TEXT_PRIMARY);
        UIManager.put("ToolTip.font", FONT_BODY);

        UIManager.put("PopupMenu.background", PANEL_BG_SOLID);
        UIManager.put("MenuItem.background", PANEL_BG_SOLID);
        UIManager.put("MenuItem.foreground", TEXT_PRIMARY);
        UIManager.put("MenuItem.selectionBackground", new Color(0x33, 0x3E, 0x8A));

        UIManager.put("ScrollBar.thumb", new Color(0x3A, 0x3F, 0x7A));
        UIManager.put("ScrollBar.thumbDarkShadow", NEBULA_BLUE);
        UIManager.put("ScrollBar.thumbHighlight", STAR_CYAN_DIM);
        UIManager.put("ScrollBar.track", DEEP_SPACE);
        UIManager.put("ScrollBar.width", 14);

        UIManager.put("ProgressBar.foreground", STAR_CYAN);
        UIManager.put("ProgressBar.background", VOID_BLACK);
        UIManager.put("ProgressBar.selectionForeground", TEXT_PRIMARY);
        UIManager.put("ProgressBar.selectionBackground", TEXT_PRIMARY);

        UIManager.put("Separator.foreground", PANEL_BORDER);
        UIManager.put("Separator.background", DEEP_SPACE);
    }
}
