package org.foxesworld.newengine.gui.components.textfield;

import org.foxesworld.newengine.gui.styles.StyleProvider;

import java.awt.*;

public class TextfieldStyleFactory {
    private TextfieldStyle textfieldStyle;

    public TextfieldStyleFactory(StyleProvider.StyleAttributes styles) {
        System.out.println(styles.background);
        this.createTextfieldStyles(styles.name, styles.texture, styles.width, styles.height, styles.font, Float.valueOf(styles.fontSize), styles.color, styles.background, styles.borderColor, styles.caretColor);
    }

    public void createTextfieldStyles(String styleName, String imagePath, int width, int height, String font, float fontSize, String textColor, String backgroundColor, String borderColor, String caretColor) {
        textfieldStyle = new TextfieldStyle(imagePath, width, height, font, fontSize, textColor, backgroundColor, borderColor, caretColor);
    }

    public TextfieldStyle getTextfieldStyle() {
        return textfieldStyle;
    }
}
