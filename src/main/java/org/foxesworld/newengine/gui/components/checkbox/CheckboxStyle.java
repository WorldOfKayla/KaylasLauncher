package org.foxesworld.newengine.gui.components.checkbox;

import org.foxesworld.newengine.gui.styles.StyleProvider;
import org.foxesworld.newengine.utils.FontUtils;
import org.foxesworld.newengine.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;

public class CheckboxStyle {
    public String fontName;
    public float fontSize;
    public Color color;
    public BufferedImage texture;

    public CheckboxStyle(StyleProvider.StyleAttributes styles) {
        this.fontName = styles.font;
        this.fontSize = styles.fontSize;
        this.color = hexToColor(styles.color);
        this.texture = ImageUtils.getLocalImage(styles.texture);
    }

    public void apply(Checkbox checkbox) {
        checkbox.setVisible(true);
        checkbox.setForeground(this.color);
        checkbox.setFont(FontUtils.getFont(this.fontName, this.fontSize));
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

