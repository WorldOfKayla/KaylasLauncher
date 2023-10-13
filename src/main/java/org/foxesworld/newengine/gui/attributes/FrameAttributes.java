package org.foxesworld.newengine.gui.attributes;

import com.google.gson.annotations.SerializedName;

import java.util.List;
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

    @SerializedName("undecorated")
    public boolean undecorated;

    @SerializedName("group")
    public Map<String, List<ComponentAttributes>> group;
}