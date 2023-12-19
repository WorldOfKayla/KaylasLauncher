package org.foxesworld.engine.gui.components;

import com.google.gson.annotations.SerializedName;
import org.foxesworld.engine.gui.components.frame.OptionGroups;

import java.util.Map;

public class ComponentAttributes {
    @SerializedName("readFrom")
    private String readFrom;
    @SerializedName("loadPanel")
    private String loadPanel;
    @SerializedName("componentType")
    private String componentType;
    @SerializedName("componentStyle")
    private String componentStyle;
    @SerializedName("componentId")
    private String componentId;
    @SerializedName("rowNum")
    private int rowNum;
    @SerializedName("imgCount")
    private int imgCount;
    @SerializedName("fontSize")
    private int fontSize;
    @SerializedName("enabled")
    private boolean enabled;
    @SerializedName("keyCode")
    private String keyCode;
    @SerializedName("initialValue")
    private String initialValue;
    @SerializedName("color")
    private String color;
    @SerializedName("localeKey")
    private String localeKey;
    @SerializedName("imageIcon")
    private String imageIcon;
    @SerializedName("rounded")
    private boolean rounded;
    @SerializedName("iconWidth")
    private int iconWidth;
    @SerializedName("iconHeight")
    private int iconHeight;
    @SerializedName("totalFrames")
    private int totalFrames;
    @SerializedName("delay")
    private int delay;
    @SerializedName("bounds")
    private String bounds;

    private  int minValue;
    private int maxValue;
    @SerializedName("groups")
    private Map<String, OptionGroups> groups;
    private int selectedIndex = 0;
    public void setInitialValue(String initialValue) {
        this.initialValue = initialValue;
    }
    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }
    public String getReadFrom() {
        return readFrom;
    }
    public String getComponentType() {
        return componentType;
    }
    public String getComponentStyle() {
        return componentStyle;
    }
    public String getComponentId() {
        return componentId;
    }
    public int getRowNum() {
        return rowNum;
    }
    public int getImgCount() {
        return imgCount;
    }
    public int getFontSize() {
        return fontSize;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public String getKeyCode() {
        return keyCode;
    }
    public String getInitialValue() {
        return initialValue;
    }
    public String getColor() {
        return color;
    }
    public String getLocaleKey() {
        return localeKey;
    }
    public String getImageIcon() {
        return imageIcon;
    }
    public boolean isRounded() {
        return rounded;
    }
    public int getIconWidth() {
        return iconWidth;
    }
    public int getIconHeight() {
        return iconHeight;
    }
    public int getTotalFrames() {
        return totalFrames;
    }
    public int getDelay() {
        return delay;
    }
    public String getBounds() {
        return bounds;
    }
    public String getLoadPanel() {
        return loadPanel;
    }
    public Map<String, OptionGroups> getGroups() {
        return groups;
    }
    public int getMinValue() {
        return minValue;
    }
    public int getMaxValue() {
        return maxValue;
    }
    public int getSelectedIndex() {
        return selectedIndex;
    }
}