package org.foxesworld.engine.gui.components.dropBox;

import org.foxesworld.engine.gui.components.ComponentFactory;
import org.foxesworld.engine.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;


public class DropBoxStyle {

    private ComponentFactory componentFactory;
    public String fontName;
    public float fontSize;
    public Color color;
    public BufferedImage texture;

    public DropBoxStyle(ComponentFactory componentFactory) {
        this.componentFactory = componentFactory;
        this.fontName = componentFactory.style.getFont();
        this.fontSize = componentFactory.style.getFontSize();
        this.color = hexToColor(componentFactory.style.getColor());
        this.texture = ImageUtils.getLocalImage(componentFactory.style.getTexture());
    }

    public void apply(DropBox dropBox) {
        dropBox.setForeground(this.color);
        dropBox.setFont(componentFactory.engine.getFONTUTILS().getFont(this.fontName, this.fontSize));
        int dropBoxH = this.texture.getHeight() / 7;
        int dropBoxW = this.texture.getWidth();
        dropBox.defaultTX = this.texture.getSubimage(0, 0, dropBoxW, dropBoxH);
        dropBox.rolloverTX = this.texture.getSubimage(0, dropBoxH, dropBoxW, dropBoxH);
        dropBox.openedTX = this.texture.getSubimage(0, dropBoxH * 2, dropBoxW, dropBoxH);
        dropBox.panelTX = this.texture.getSubimage(0, dropBoxH * 3, dropBoxW, dropBoxH);
        dropBox.selectedTX = this.texture.getSubimage(0, dropBoxH * 4, dropBoxW, dropBoxH);
        dropBox.point = ImageUtils.getLocalImage("assets/ui/icons/point.png");
    }
}

