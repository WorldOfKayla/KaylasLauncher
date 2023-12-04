package org.foxesworld.engine.gui;

import org.foxesworld.engine.gui.components.frame.OptionGroups;

import javax.swing.*;
import java.util.Map;

public interface GuiBuilderListener {

    void onPanelBuild(Map<String, OptionGroups> groups, String componentGroup, JPanel parentPanel);
    void onPanelsBuilt();
}
