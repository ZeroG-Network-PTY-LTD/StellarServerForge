package com.zerog.stellarserverforge.gui.theme;

import javax.swing.JPanel;
import java.awt.LayoutManager;

/**
 * The window's content-pane background: a flat Nocturne ground. The previous starfield/nebula
 * backdrop is cancelled per the design manual — no gradients, glows or decorative animation.
 */
public class StarfieldPanel extends JPanel {

    public StarfieldPanel() {
        this(new java.awt.BorderLayout());
    }

    public StarfieldPanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        setBackground(StellarTheme.BG);
    }
}
