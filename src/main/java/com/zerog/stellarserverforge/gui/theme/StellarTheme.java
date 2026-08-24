package com.zerog.stellarserverforge.gui.theme;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Set;

/**
 * The "Nocturne" design system for StellarServerForge — a near-neutral blue-grey dark ground,
 * Inter type, 8px radii, and a single blurple accent used as a line, mark or glow, never a large
 * fill. Per the design manual: no starfield, no gradients, no cyan/gold, no decorative animation.
 * Every color/font/spacing value used by the UI must come from this class.
 */
public final class StellarTheme {

    private StellarTheme() {
    }

    // ── Core roles ──────────────────────────────────────────────────────────
    public static final Color BG = new Color(0x16, 0x18, 0x26);
    public static final Color SURFACE = new Color(0x23, 0x25, 0x32);
    public static final Color FIELD_BG = new Color(0x1C, 0x1E, 0x2C);
    public static final Color CONSOLE_BG = new Color(0x29, 0x2B, 0x31);

    public static final Color TEXT_PRIMARY = new Color(0xE9, 0xE9, 0xED);
    public static final Color TEXT_SECONDARY = new Color(0xB2, 0xB6, 0xCA);
    public static final Color TEXT_MUTED = new Color(0x75, 0x79, 0x8C);
    public static final Color TEXT_KICKER = new Color(0x93, 0x97, 0xAB);

    public static final Color DIVIDER = new Color(0xE9, 0xE9, 0xED, 41);

    // ── Accent ramp ─────────────────────────────────────────────────────────
    public static final Color ACCENT = new Color(0x91, 0x84, 0xD9);
    public static final Color ACCENT_100 = new Color(0xF5, 0xF4, 0xFF);
    public static final Color ACCENT_200 = new Color(0xE7, 0xE5, 0xFE);
    public static final Color ACCENT_300 = new Color(0xD2, 0xCE, 0xFD);
    public static final Color ACCENT_400 = new Color(0xB5, 0xAB, 0xFC);
    public static final Color ACCENT_700 = new Color(0x5D, 0x52, 0x94);
    public static final Color ACCENT_800 = new Color(0x42, 0x3A, 0x6A);
    public static final Color ACCENT_900 = new Color(0x2B, 0x27, 0x41);

    // ── Neutral ramp ────────────────────────────────────────────────────────
    public static final Color NEUTRAL_100 = new Color(0xF3, 0xF5, 0xFE);
    public static final Color NEUTRAL_300 = new Color(0xCF, 0xD3, 0xE5);
    public static final Color NEUTRAL_500 = new Color(0x93, 0x97, 0xAB);
    public static final Color NEUTRAL_700 = new Color(0x59, 0x5D, 0x6C);
    public static final Color NEUTRAL_800 = new Color(0x3F, 0x42, 0x4D);
    public static final Color NEUTRAL_900 = new Color(0x29, 0x2B, 0x31);

    // ── Status (deliberate extension, §3 of the manual) ────────────────────
    public static final Color STATUS_RUNNING = new Color(0x84, 0xD9, 0xA0);
    public static final Color STATUS_WARNING = new Color(0xD9, 0xC1, 0x84);
    public static final Color STATUS_FAILED = new Color(0xD9, 0x8A, 0x84);
    public static final Color STATUS_IDLE = NEUTRAL_500;

    // ── Legacy field-name aliases ───────────────────────────────────────────
    // Kept so every call site across the app doesn't need a mechanical rename; each maps to the
    // closest Nocturne role for the way it was actually used.
    public static final Color VOID_BLACK = CONSOLE_BG;
    public static final Color DEEP_SPACE = FIELD_BG;
    public static final Color PANEL_BG = SURFACE;
    public static final Color PANEL_BG_SOLID = SURFACE;
    public static final Color PANEL_BORDER = NEUTRAL_800;
    public static final Color STAR_CYAN = ACCENT;
    public static final Color STAR_CYAN_DIM = ACCENT_700;
    public static final Color STELLAR_GOLD = STATUS_WARNING;
    public static final Color SUCCESS_GREEN = STATUS_RUNNING;
    public static final Color WARNING_ORANGE = STATUS_WARNING;
    public static final Color ERROR_RED = STATUS_FAILED;

    // ── Type ────────────────────────────────────────────────────────────────
    private static final String SANS = resolveFamily("Inter", "Segoe UI");
    private static final String MONO = resolveFamily("JetBrains Mono", "Consolas");

    /** Screen title — one per screen, flush left. */
    public static final Font FONT_TITLE = new Font(SANS, Font.PLAIN, 20);
    /** Card title / dialog title. */
    public static final Font FONT_HEADING = new Font(SANS, Font.PLAIN, 15);
    /** Default UI font: labels, buttons, body copy. */
    public static final Font FONT_BODY = new Font(SANS, Font.PLAIN, 13);
    /** Field-group labels and card eyebrows: uppercase, +tracking, neutral-500. */
    public static final Font FONT_KICKER = new Font(SANS, Font.BOLD, 10);
    /** Helper text under fields. */
    public static final Font FONT_CAPTION = new Font(SANS, Font.PLAIN, 11);
    /** Console, versions, paths, ports. */
    public static final Font FONT_MONO = new Font(MONO, Font.PLAIN, 12);
    /** Button / small UI label weight — never bolder than this. */
    public static final Font FONT_LABEL = new Font(SANS, Font.PLAIN, 13);

    // ── Spacing & radius (rounded-pixel scale, §5) ──────────────────────────
    public static final int SPACE_3 = 3;
    public static final int SPACE_6 = 6;
    public static final int SPACE_8 = 8;
    public static final int SPACE_11 = 11;
    public static final int SPACE_17 = 17;
    public static final int SPACE_22 = 22;

    public static final int RADIUS_TAG = 4;
    public static final int RADIUS_CONTROL = 8;
    public static final int RADIUS_PANEL = 14;

    private static String resolveFamily(String preferred, String fallback) {
        try {
            Set<String> available = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames());
            return available.contains(preferred) ? preferred : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    /** Installs Nimbus as the base L&amp;F (it respects UIManager color overrides far better than
     * native L&amp;Fs), then repaints its defaults to the Nocturne palette. Call once, before any
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

        UIManager.put("control", SURFACE);
        UIManager.put("info", SURFACE);
        UIManager.put("nimbusBase", FIELD_BG);
        UIManager.put("nimbusBlueGrey", NEUTRAL_800);
        UIManager.put("nimbusLightBackground", BG);
        UIManager.put("nimbusFocus", ACCENT);
        UIManager.put("nimbusSelectionBackground", ACCENT_900);
        UIManager.put("nimbusSelectedText", TEXT_PRIMARY);
        UIManager.put("nimbusDisabledText", TEXT_MUTED);
        UIManager.put("nimbusBorder", NEUTRAL_800);
        UIManager.put("nimbusGreen", STATUS_RUNNING);
        UIManager.put("nimbusRed", STATUS_FAILED);
        UIManager.put("nimbusOrange", STATUS_WARNING);
        UIManager.put("nimbusAlertYellow", STATUS_WARNING);
        UIManager.put("text", TEXT_PRIMARY);
        UIManager.put("textForeground", TEXT_PRIMARY);
        UIManager.put("textBackground", FIELD_BG);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("TitledBorder.titleColor", ACCENT_300);
        UIManager.put("TextField.font", FONT_BODY);
        UIManager.put("TextField.background", FIELD_BG);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground", ACCENT);
        UIManager.put("TextArea.font", FONT_MONO);
        UIManager.put("TextArea.background", CONSOLE_BG);
        UIManager.put("TextArea.foreground", NEUTRAL_100);
        UIManager.put("TextArea.caretForeground", ACCENT);
        UIManager.put("List.background", SURFACE);
        UIManager.put("List.foreground", TEXT_PRIMARY);
        UIManager.put("List.selectionBackground", ACCENT_900);
        UIManager.put("List.selectionForeground", ACCENT_300);
        UIManager.put("ScrollPane.background", BG);
        UIManager.put("Viewport.background", BG);
        UIManager.put("TabbedPane.selected", SURFACE);
        UIManager.put("TabbedPane.background", BG);
        UIManager.put("TabbedPane.foreground", TEXT_SECONDARY);
        UIManager.put("TabbedPane.selectedForeground", TEXT_PRIMARY);
        UIManager.put("TabbedPane.underlineColor", ACCENT);
        UIManager.put("CheckBox.font", FONT_BODY);
        UIManager.put("Button.font", FONT_LABEL);

        UIManager.put("Spinner.background", FIELD_BG);
        UIManager.put("Spinner.foreground", TEXT_PRIMARY);
        UIManager.put("Spinner.font", FONT_BODY);
        UIManager.put("FormattedTextField.background", FIELD_BG);
        UIManager.put("FormattedTextField.foreground", TEXT_PRIMARY);
        UIManager.put("FormattedTextField.caretForeground", ACCENT);

        UIManager.put("ComboBox.background", FIELD_BG);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", ACCENT_900);
        UIManager.put("ComboBox.selectionForeground", ACCENT_300);
        UIManager.put("ComboBox.font", FONT_BODY);

        UIManager.put("ToolTip.background", SURFACE);
        UIManager.put("ToolTip.foreground", TEXT_PRIMARY);
        UIManager.put("ToolTip.font", FONT_CAPTION);

        UIManager.put("PopupMenu.background", SURFACE);
        UIManager.put("MenuItem.background", SURFACE);
        UIManager.put("MenuItem.foreground", TEXT_PRIMARY);
        UIManager.put("MenuItem.selectionBackground", ACCENT_900);

        UIManager.put("ScrollBar.thumb", NEUTRAL_800);
        UIManager.put("ScrollBar.thumbDarkShadow", NEUTRAL_900);
        UIManager.put("ScrollBar.thumbHighlight", NEUTRAL_700);
        UIManager.put("ScrollBar.track", BG);
        UIManager.put("ScrollBar.width", 10);

        UIManager.put("ProgressBar.foreground", ACCENT);
        UIManager.put("ProgressBar.background", FIELD_BG);
        UIManager.put("ProgressBar.selectionForeground", TEXT_PRIMARY);
        UIManager.put("ProgressBar.selectionBackground", TEXT_PRIMARY);

        UIManager.put("Separator.foreground", NEUTRAL_800);
        UIManager.put("Separator.background", BG);
    }
}
