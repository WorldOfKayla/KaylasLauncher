package org.foxesworld.newengine.gui.components.passfield;

import org.foxesworld.newengine.gui.components.Components;
import org.foxesworld.newengine.utils.ImageUtils;

import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;


public class PassFieldStyle {
    public String fontName = "";
    public String echoChar = "";
    public float fontSize = 1.0f;

    public BufferedImage texture;
    public Color textColor;
    public Color caretColor;
    public Border border;

    public PassFieldStyle(Components components) {
        this.texture = ImageUtils.getLocalImage(components.style.texture);
        this.textColor = hexToColor(components.style.color);
        this.caretColor = hexToColor(components.style.caretColor);
        this.echoChar = "*";
    }

    public void apply(PassField pass) {
        pass.texture = this.texture;
        pass.setCaretColor(this.caretColor);
        pass.setBackground(this.textColor);
        pass.setForeground(this.textColor);
        pass.setBorder(this.border);
    }
}

