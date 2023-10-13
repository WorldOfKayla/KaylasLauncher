package org.foxesworld.newengine.gui.attributes;

import com.google.gson.annotations.SerializedName;

public class ComponentAttributes {

    @SerializedName("componentType")
    public String componentType;

    @SerializedName("componentStyle")
    public String componentStyle;

    @SerializedName("componentId")
    public String componentId;

    @SerializedName("localeKey")
    public String localeKey;

    @SerializedName("xPos")
    public int xPos;

    @SerializedName("yPos")
    public int yPos;

    @SerializedName("width")
    public int width;

    @SerializedName("height")
    public int height;
}