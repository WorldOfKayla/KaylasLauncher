package org.foxesworld.engine.gui.components.panel;

import com.google.gson.annotations.SerializedName;

public class PanelOptions {
    @SerializedName("opaque")
    private boolean opaque = false;
    @SerializedName("visible")
    private boolean visible;
    @SerializedName("focusable")
    private boolean focusable;

    @SerializedName("rounded")
    private boolean rounded;

    @SerializedName("cornerRadius")
    private int cornerRadius;

    @SerializedName("border")
    private String border = "";
    @SerializedName("listener")
    private String listener = "";

    @SerializedName("background")
    private String background = "";
    @SerializedName("backgroundImage")
    private String backgroundImage;

    @SerializedName("bounds")
    private String bounds = "";

    public boolean isOpaque() {
        return opaque;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isFocusable() {
        return focusable;
    }

    public boolean isRounded() {
        return rounded;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public String getBorder() {
        return border;
    }

    public String getListener() {
        return listener;
    }

    public String getBackground() {
        return background;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public String getBounds() {
        return bounds;
    }
}
