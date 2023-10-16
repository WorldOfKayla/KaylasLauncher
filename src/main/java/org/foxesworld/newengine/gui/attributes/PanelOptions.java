package org.foxesworld.newengine.gui.attributes;

import com.google.gson.annotations.SerializedName;

public class PanelOptions {
    @SerializedName("opaque")
    public boolean opaque = false;
    @SerializedName("border")
    public String border = "";
    @SerializedName("listener")
    public String listener = "";

    @SerializedName("background")
    public String background = "";

    @SerializedName("width")
    public int width = 100;

    @SerializedName("height")
    public int height = 100;
    @SerializedName("xPos")
    public int xPos = 0;
    @SerializedName("yPos")
    public int yPos = 0;
}
