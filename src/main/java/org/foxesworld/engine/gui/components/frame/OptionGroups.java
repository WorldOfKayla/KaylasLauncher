package org.foxesworld.engine.gui.components.frame;

import com.google.gson.annotations.SerializedName;
import org.foxesworld.engine.gui.components.ComponentAttributes;
import org.foxesworld.engine.gui.components.panel.PanelOptions;

import java.util.List;
import java.util.Map;

public class OptionGroups {

    @SerializedName("panelOptions")
    private PanelOptions panelOptions;

    @SerializedName("childComponents")
    private List<ComponentAttributes> childComponents;

    @SerializedName("groups")
    private Map<String, OptionGroups> groups;

    public PanelOptions getPanelOptions() {
        return panelOptions;
    }

    public List<ComponentAttributes> getChildComponents() {
        return childComponents;
    }

    public Map<String, OptionGroups> getGroups() {
        return groups;
    }
}