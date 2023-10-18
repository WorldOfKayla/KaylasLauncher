package org.foxesworld.engine.gui.components.scrollBox;

import org.foxesworld.engine.gui.components.Components;
import org.foxesworld.engine.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;


public class ScrollBoxStyle {

    private  Components components;
    public String fontName;
    public float fontSize;
    public Color color;
    public BufferedImage texture;

    public ScrollBoxStyle(Components components) {
        this.components = components;
        this.fontName = components.style.font;
        this.fontSize = components.style.fontSize;
        this.color = hexToColor(components.style.color);
        this.texture = ImageUtils.getLocalImage(components.style.texture);
    }

    public void apply(ScrollBox scrollBox) {
        scrollBox.setForeground(this.color);
        scrollBox.setFont(components.appFrame.getFontUtils().getFont(this.fontName, this.fontSize));
        int comboboxh = this.texture.getHeight() / 5;
        int comboboxw = this.texture.getWidth();
        scrollBox.defaultTX = this.texture.getSubimage(0, 0, comboboxw, comboboxh);
        scrollBox.rolloverTX = this.texture.getSubimage(0, comboboxh, comboboxw, comboboxh);
        scrollBox.openedTX = this.texture.getSubimage(0, comboboxh * 2, comboboxw, comboboxh);
        scrollBox.panelTX = this.texture.getSubimage(0, comboboxh * 3, comboboxw, comboboxh);
        scrollBox.selectedTX = this.texture.getSubimage(0, comboboxh * 4, comboboxw, comboboxh);
        scrollBox.point = ImageUtils.getLocalImage("assets/ui/icons/point.png");
    }
}

