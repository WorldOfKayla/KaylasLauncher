package org.foxesworld.newengine.gui.attributes;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class FrameAttributes {
    @SerializedName("title")
    public String title;

    @SerializedName("width")
    public int width;

    @SerializedName("height")
    public int height;

    @SerializedName("resizable")
    public boolean resizable;

    @SerializedName("backgroundImage")
    public String backgroundImage;

    @SerializedName("backgroundBlur")
    public String backgroundBlur;

    @SerializedName("undecorated")
    public boolean undecorated;

    @SerializedName("groups")
    public Map<String, OptionGroups> groups;
}