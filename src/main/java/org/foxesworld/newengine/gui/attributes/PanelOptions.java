package org.foxesworld.newengine.gui.attributes;

import com.google.gson.annotations.SerializedName;

public class PanelOptions {
    @SerializedName("opaque")
    public boolean opaque;

    @SerializedName("background")
    public String background;

    @SerializedName("width")
    public int width;

    @SerializedName("height")
    public int height;

    @SerializedName("xPos")
    public int xPos;

    @SerializedName("yPos")
    public int yPos;
}
