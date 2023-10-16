package org.foxesworld.newengine.gui.attributes;

import com.google.gson.annotations.SerializedName;

public class PanelOptions {
    @SerializedName("opaque")
    public boolean opaque = false;

    @SerializedName("visibility")
    public boolean visibility;
    @SerializedName("border")
    public String border = "";
    @SerializedName("listener")
    public String listener = "";

    @SerializedName("background")
    public String background = "";

    @SerializedName("bounds")
    public String bounds = "";
}
