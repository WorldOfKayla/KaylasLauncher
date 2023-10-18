package org.foxesworld.engine.gui.attributes;

import com.google.gson.annotations.SerializedName;

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