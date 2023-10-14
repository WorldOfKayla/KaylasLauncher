package org.foxesworld.newengine.gui.attributes;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OptionGroups {
    @SerializedName("panelOptions")
    public PanelOptions panelOptions;

    @SerializedName("childrenComponents")
    public List<ComponentAttributes> childrenComponents;
}