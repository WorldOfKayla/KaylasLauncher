package org.foxesworld.engine.gui.components;

import org.foxesworld.engine.gui.components.frame.OptionGroups;

import java.util.Map;

public class ComponentAttributes {
    private String readFrom;
    private String loadPanel;
    private String componentType;
    private String componentStyle;
    private String componentId;
    private int rowNum;
    private int imgCount;
    private int fontSize;
    private boolean enabled;
    private String keyCode;
    private String initialValue;
    private String color;
    private String localeKey;
    private String imageIcon;
    private boolean rounded;
    private int iconWidth;
    private int iconHeight;
    private int totalFrames;
    private int delay;
    private String bounds;
    private  int minValue;
    private int maxValue;
    private String thumbImage;
    private String trackImage;
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