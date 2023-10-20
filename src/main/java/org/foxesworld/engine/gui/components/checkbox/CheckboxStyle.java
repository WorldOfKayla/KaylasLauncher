package org.foxesworld.engine.gui.components.checkbox;

import org.foxesworld.engine.gui.components.Components;
import org.foxesworld.engine.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class CheckboxStyle {
    public String fontName;
    public float fontSize;
    public Color color;
    public BufferedImage texture;
    private Components components;

    public CheckboxStyle(Components components) {
        this.components = components;
        this.fontName = components.style.font;
        this.fontSize = components.style.fontSize;
        this.color = hexToColor(components.style.color);
        this.texture = ImageUtils.getLocalImage(components.style.texture);
    }

    public void apply(Checkbox checkbox) {
        checkbox.setVisible(true);
        checkbox.setForeground(this.color);
        checkbox.setFont(components.engine.getFontUtils().getFont(this.fontName, this.fontSize));
        int i = this.texture.getWidth() / 4;
        checkbox.defaultTX = this.texture.getSubimage(0, 0, i, i);
        checkbox.rolloverTX = this.texture.getSubimage(i, 0, i, i);
        checkbox.selectedTX = this.texture.getSubimage(i * 2, 0, i, i);
        checkbox.selectedRolloverTX = this.texture.getSubimage(i * 3, 0, i, i);
        checkbox.setIcon(new ImageIcon(checkbox.defaultTX));
        checkbox.setRolloverIcon(new ImageIcon(checkbox.rolloverTX));
        checkbox.setSelectedIcon(new ImageIcon(checkbox.selectedTX));
        checkbox.setRolloverSelectedIcon(new ImageIcon(checkbox.selectedRolloverTX));
    }
}

