package org.foxesworld.newengine.gui.components.frame;

import com.google.gson.annotations.SerializedName;

class ComponentAttributes {

    @SerializedName("componentType")
    String componentType;

    @SerializedName("componentStyle")
    String componentStyle;

    @SerializedName("localeKey")
    String localeKey;

    @SerializedName("xPos")
    int xPos;

    @SerializedName("yPos")
    int yPos;

    @SerializedName("width")
    int width;

    @SerializedName("height")
    int height;
}