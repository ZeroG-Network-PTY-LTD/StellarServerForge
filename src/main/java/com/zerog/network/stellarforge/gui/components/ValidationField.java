package com.zerog.network.stellarforge.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * A reusable form-field component with live inline validation feedback.
 *
 * <p>Visual states:
 * <ul>
 *   <li>· (grey)   — neutral / not yet evaluated</li>
 *   <li>✓ (green)  — valid</li>
 *   <li>⚠ (yellow) — acceptable but not ideal (warning)</li>
 *   <li>✗ (red)    — invalid / required and empty</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   ValidationField vf = new ValidationField("Server Name", text -> {
 *       if (text.isEmpty()) return ValidationField.ValidationResult.invalid("Required");
 *       if (text.length() < 3) return ValidationField.ValidationResult.warning("Too short");
 *       return null; // valid
 *   });
 *   panel.add(vf);
 *   String value = vf.getText();
 *   boolean ok   = vf.isInputValid();
 * </pre>
 */
public class ValidationField extends JPanel {

    // ── State enum ────────────────────────────────────────────────────────────

    public enum State { NEUTRAL, VALID, WARNING, INVALID }

    // ── Validator interface ───────────────────────────────────────────────────

    @FunctionalInterface
    public interface Validator {
        /**
         * Validate the given text.
         * @return {@code null} for valid, or a {@link ValidationResult} describing the issue.
         */
        ValidationResult validate(String text);
    }

    // ── Result model ──────────────────────────────────────────────────────────

    public static class ValidationResult {
        public final State  state;
        public final String message;

        public ValidationResult(State state, String message) {
            this.state   = state;
            this.message = message != null ? message : "";
        }

        public static ValidationResult valid()                 { return new ValidationResult(State.VALID,   ""); }
        public static ValidationResult invalid(String msg)     { return new ValidationResult(State.INVALID,  msg); }
        public static ValidationResult warning(String msg)     { return new ValidationResult(State.WARNING,  msg); }
    }

    // ── Colour constants ──────────────────────────────────────────────────────

    private static final Color CLR_BORDER_NEUTRAL = new Color(65, 65, 85);
    private static final Color CLR_BORDER_VALID   = new Color(39, 174, 96);
    private static final Color CLR_BORDER_WARN    = new Color(230, 126, 34);
    private static final Color CLR_BORDER_INVALID = new Color(192, 57, 43);

    private static final Color CLR_ICON_NEUTRAL   = new Color(120, 120, 135);
    private static final Color CLR_ICON_VALID     = new Color(39,  174, 96);
    private static final Color CLR_ICON_WARN      = new Color(230, 126, 34);
    private static final Color CLR_ICON_INVALID   = new Color(192, 57,  43);

    // ── Components ────────────────────────────────────────────────────────────

    private final JLabel     labelComp;
    private final JTextField field;
    private final JLabel     iconLabel;

    private final Validator validator;
    private State currentState = State.NEUTRAL;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Create a ValidationField with a label text and a validator.
     *
     * @param labelText  Short descriptive label shown to the left of the field.
     * @param validator  Function that validates the text; may be {@code null} for no validation.
     */
    public ValidationField(String labelText, Validator validator) {
        this.validator = validator;

        setLayout(new BorderLayout(6, 0));
        setOpaque(false);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        // ── Label ─────────────────────────────────────────────────────────────
        labelComp = new JLabel(labelText);
        labelComp.setForeground(new Color(185, 185, 210));
        labelComp.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        labelComp.setPreferredSize(new Dimension(130, 28));
        labelComp.setHorizontalAlignment(SwingConstants.RIGHT);

        // ── Text field ────────────────────────────────────────────────────────
        field = new JTextField();
        field.setBackground(new Color(38, 38, 52));
        field.setForeground(new Color(220, 220, 235));
        field.setCaretColor(Color.WHITE);
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CLR_BORDER_NEUTRAL),
                new EmptyBorder(3, 6, 3, 6)));

        // ── Icon label ────────────────────────────────────────────────────────
        iconLabel = new JLabel("·");
        iconLabel.setForeground(CLR_ICON_NEUTRAL);
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        iconLabel.setPreferredSize(new Dimension(24, 28));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

        add(labelComp, BorderLayout.WEST);
        add(field,     BorderLayout.CENTER);
        add(iconLabel, BorderLayout.EAST);

        // Live validation on each keystroke
        if (validator != null) {
            field.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e)  { runValidation(); }
                public void removeUpdate(DocumentEvent e)  { runValidation(); }
                public void changedUpdate(DocumentEvent e) { /* structural changes, no-op */ }
            });
        }
    }

    /** Create a ValidationField without a validator (neutral icon always shown). */
    public ValidationField(String labelText) {
        this(labelText, null);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** @return the current text in the field. */
    public String getText()             { return field.getText(); }

    /** Set the field text (does NOT trigger live validation). */
    public void setText(String text)    { field.setText(text); }

    /** @return current validation state. */
    public State getValidationState()   { return currentState; }

    /** @return the inner {@link JTextField} for advanced configuration. */
    public JTextField getTextField()    { return field; }

    /**
     * Force re-validate the current text.
     * Useful when applying a template or loading saved values.
     */
    public void revalidateInput()       { runValidation(); }

    /**
     * @return {@code true} when state is {@link State#VALID} or {@link State#NEUTRAL}
     *         (i.e. not INVALID or WARNING-blocking).
     */
    public boolean isInputValid()       { return currentState == State.VALID || currentState == State.NEUTRAL; }

    /** Override the default label width (default 130 px). */
    public void setLabelWidth(int px) {
        labelComp.setPreferredSize(new Dimension(px, labelComp.getPreferredSize().height));
        revalidate();
    }

    /** Set a hint/placeholder text shown when the field is empty. */
    public void setPlaceholder(String hint) {
        field.putClientProperty("JTextField.placeholderText", hint);
    }

    /** Enable/disable editing of this field. */
    public void setFieldEditable(boolean editable) {
        field.setEditable(editable);
        field.setEnabled(editable);
        if (!editable) {
            field.setBackground(new Color(30, 30, 42));
            field.setForeground(new Color(140, 140, 160));
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void runValidation() {
        if (validator == null) return;

        ValidationResult result = validator.validate(field.getText());
        if (result == null) {
            applyState(State.VALID, "");
        } else {
            applyState(result.state, result.message);
        }
    }

    private void applyState(State state, String tooltip) {
        this.currentState = state;
        String icon; Color iconColor; Color borderColor;
        switch (state) {
            case VALID:
                icon = "✓"; iconColor = CLR_ICON_VALID;   borderColor = CLR_BORDER_VALID;
                break;
            case WARNING:
                icon = "⚠"; iconColor = CLR_ICON_WARN;    borderColor = CLR_BORDER_WARN;
                break;
            case INVALID:
                icon = "✗"; iconColor = CLR_ICON_INVALID; borderColor = CLR_BORDER_INVALID;
                break;
            default:
                icon = "·"; iconColor = CLR_ICON_NEUTRAL; borderColor = CLR_BORDER_NEUTRAL;
        }
        iconLabel.setText(icon);
        iconLabel.setForeground(iconColor);
        iconLabel.setToolTipText((tooltip != null && !tooltip.isEmpty()) ? tooltip : null);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                new EmptyBorder(3, 6, 3, 6)));
        repaint();
    }
}

