package com.zerog.network.stellarforge.gui.components;

import com.zerog.network.stellarforge.model.ServerProfile;

import javax.swing.*;
import java.awt.*;

/**
 * Custom cell renderer for profile list display
 */
public class ProfileListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        
        if (value instanceof ServerProfile) {
            ServerProfile profile = (ServerProfile) value;
            String displayText = profile.getProfileName();
            
            if (profile.isFavorite()) {
                displayText += " ⭐";
            }
            
            if (profile.getDescription() != null && !profile.getDescription().isEmpty()) {
                displayText = "<html><b>" + displayText + "</b><br/>" +
                             "<small>" + profile.getDescription() + "</small></html>";
            }
            
            setText(displayText);
        }
        
        return this;
    }
}

