package org.foxesworld.engine.gui.components.panel;

import com.google.gson.annotations.SerializedName;

public class PanelOptions {
    @SerializedName("opaque")
    public boolean opaque = false;

    @SerializedName("visible")
    public boolean visible;
    @SerializedName("border")
    public String border = "";
    @SerializedName("listener")
    public String listener = "";

    @SerializedName("background")
    public String background = "";

    @SerializedName("bounds")
    public String bounds = "";
}
