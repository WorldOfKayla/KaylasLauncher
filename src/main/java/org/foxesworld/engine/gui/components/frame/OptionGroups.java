package org.foxesworld.engine.gui.components.frame;

import com.google.gson.annotations.SerializedName;
import org.foxesworld.engine.gui.components.ComponentAttributes;
import org.foxesworld.engine.gui.components.panel.PanelOptions;

import java.util.List;
import java.util.Map;

public class OptionGroups {

    @SerializedName("panelOptions")
    public PanelOptions panelOptions;

    @SerializedName("childComponents")
    public List<ComponentAttributes> childComponents;

    @SerializedName("groups")
    public Map<String, OptionGroups> groups;
}