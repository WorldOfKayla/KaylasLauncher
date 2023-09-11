package org.foxesworld.newengine.gui.components.textfield;

import org.foxesworld.newengine.gui.components.StyleLoader;

import java.awt.*;

public class TextfieldStyleFactory {
    private TextfieldStyle textfieldStyle;

    public TextfieldStyleFactory(StyleLoader.StyleAttributes styles) {
        this.createTextfieldStyles(styles.name, styles.texture, styles.width, styles.height, styles.font, Float.valueOf(styles.fontSize), styles.color ,Color.decode("0xd4dc7b"));
    }

    public void createTextfieldStyles(String styleName, String imagePath, int width, int height, String font, float fontSize, String textColor,Color carretColor) {
        textfieldStyle = new TextfieldStyle(imagePath, width, height, font, fontSize, textColor, carretColor);
    }

    public TextfieldStyle getTextfieldStyle() {
        return textfieldStyle;
    }
}
