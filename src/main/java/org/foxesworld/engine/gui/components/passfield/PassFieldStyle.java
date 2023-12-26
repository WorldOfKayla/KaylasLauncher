package org.foxesworld.engine.gui.components.passfield;

import org.foxesworld.engine.gui.components.ComponentFactory;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;


public class PassFieldStyle {
    private ComponentFactory componentFactory;
    public String fontName = "";
    public String echoChar = "";
    public float fontSize = 1.0f;

    public BufferedImage texture;
    public Color textColor;
    public Color caretColor;
    public Border border;

    public PassFieldStyle(ComponentFactory componentFactory) {
        this.componentFactory = componentFactory;
        this.texture = ImageUtils.getLocalImage(componentFactory.style.getTexture());
        this.textColor = hexToColor(componentFactory.style.getColor());
        this.caretColor = hexToColor(componentFactory.style.getCaretColor());
        this.echoChar = "*";
    }

    public void apply(PassField pass) {
        pass.texture = this.texture;
        pass.setPaddingX(this.componentFactory.style.getPaddingX());
        pass.setPaddingY(this.componentFactory.style.getPaddingY());
        pass.setCaretColor(this.caretColor);
        pass.setBackground(this.textColor);
        pass.setForeground(this.textColor);
        pass.setBorder(this.border);
    }
}

