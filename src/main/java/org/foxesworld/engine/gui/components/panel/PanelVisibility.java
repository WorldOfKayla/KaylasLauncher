package org.foxesworld.engine.gui.components.panel;

import org.foxesworld.engine.Engine;

import javax.swing.*;

public class PanelVisibility {

    private  Engine engine;

    public PanelVisibility(Engine engine){
        this.engine = engine;
    }

    public void displayPanel(String displayString) {
        String[] panelElements = displayString.split("\\|");
        if (panelElements.length <= 1) {
            this.panelVisibility(displayString);
        } else {
            for (String panelElement : panelElements) {
                this.panelVisibility(panelElement);
            }
        }
    }
    private void panelVisibility(String panelElement) {
        String[] parts = panelElement.split("->");
        if (parts.length == 2) {
            String panelName = parts[0];
            boolean displayValue = Boolean.parseBoolean(parts[1]);
            JPanel groupPanel = engine.getGuiBuilder().getPanelsMap().get(panelName);
            if (groupPanel != null) {
                groupPanel.setVisible(displayValue);
            }
        }
    }
}
