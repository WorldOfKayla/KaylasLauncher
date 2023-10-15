package org.foxesworld.newengine.gui.attributes;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class OptionGroups {
    @SerializedName("panelOptions")
    public PanelOptions panelOptions;

    @SerializedName("childrenComponents")
    public List<ComponentAttributes> childrenComponents;
    @SerializedName("groups")
    public Map<String, OptionGroups> groups;
}