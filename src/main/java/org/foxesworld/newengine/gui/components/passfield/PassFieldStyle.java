package org.foxesworld.newengine.gui.components.passfield;

import org.foxesworld.newengine.gui.components.Components;
import org.foxesworld.newengine.gui.styles.StyleProvider;
import org.foxesworld.newengine.utils.FontUtils;
import org.foxesworld.newengine.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.border.Border;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;


public class PassFieldStyle {
    public String fontName = "";
    public String echoChar = "";
    public float fontSize = 1.0f;

    public BufferedImage texture;
    public Color textColor;
    public Color caretColor;
    public Border border;

    public PassFieldStyle(StyleProvider.StyleAttributes style) {
        this.texture = ImageUtils.getLocalImage(style.texture);
        this.textColor = hexToColor(style.color);
        this.caretColor = hexToColor(style.caretColor);
        this.echoChar = "*";
    }

    public void apply(PassField pass) {
        pass.texture = this.texture;
        //pass.font = FontUtils.getFont(this.fontName, this.fontSize);
        pass.setCaretColor(this.caretColor);
        pass.setBackground(this.textColor);
        pass.setForeground(this.textColor);
        pass.setBorder(this.border);
    }
}

