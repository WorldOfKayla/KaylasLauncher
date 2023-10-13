package org.foxesworld.newengine.gui.components.frame;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

class FrameAttributes {
    @SerializedName("title")
    String title;

    @SerializedName("width")
    int width;

    @SerializedName("height")
    int height;

    @SerializedName("resizable")
    boolean resizable;

    @SerializedName("undecorated")
    boolean undecorated;

    @SerializedName("groups")
    Map<String, List<ComponentAttributes>> groups;
}