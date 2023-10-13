package org.foxesworld.newengine.gui.components.textfield;

import org.foxesworld.newengine.gui.components.StyleProvider;

import java.awt.*;

public class TextfieldStyleFactory {
    private TextfieldStyle textfieldStyle;

    public TextfieldStyleFactory(StyleProvider.StyleAttributes styles) {
        this.createTextfieldStyles(styles.name, styles.texture, styles.width, styles.height, styles.font, Float.valueOf(styles.fontSize), styles.color, styles.borderColor, Color.decode("0xd4dc7b"));
    }

    public void createTextfieldStyles(String styleName, String imagePath, int width, int height, String font, float fontSize, String textColor, String borderColor, Color carretColor) {
        textfieldStyle = new TextfieldStyle(imagePath, width, height, font, fontSize, textColor, borderColor, carretColor);
    }

    public TextfieldStyle getTextfieldStyle() {
        return textfieldStyle;
    }
}
