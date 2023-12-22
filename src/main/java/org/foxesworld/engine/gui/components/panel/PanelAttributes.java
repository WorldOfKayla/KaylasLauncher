package org.foxesworld.engine.gui.components.panel;
public class PanelAttributes {
    private boolean opaque = false;
    private boolean visible;
    private boolean focusable;
    private int cornerRadius;
    private String border = "";
    private String listener = "";
    private String background = "";
    private String backgroundImage;
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

    public void setOpaque(boolean opaque) {
        this.opaque = opaque;
    }
}