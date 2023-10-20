package org.foxesworld.engine.gui.components.scrollBox;

import org.foxesworld.engine.gui.components.ComponentFactory;
import org.foxesworld.engine.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;


public class ScrollBoxStyle {

    private ComponentFactory componentFactory;
    public String fontName;
    public float fontSize;
    public Color color;
    public BufferedImage texture;

    public ScrollBoxStyle(ComponentFactory componentFactory) {
        this.componentFactory = componentFactory;
        this.fontName = componentFactory.style.font;
        this.fontSize = componentFactory.style.fontSize;
        this.color = hexToColor(componentFactory.style.color);
        this.texture = ImageUtils.getLocalImage(componentFactory.style.texture);
    }

    public void apply(ScrollBox scrollBox) {
        scrollBox.setForeground(this.color);
        scrollBox.setFont(componentFactory.engine.getFONTUTILS().getFont(this.fontName, this.fontSize));
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

