package org.foxesworld.newengine.gui.components.label;

import org.foxesworld.newengine.gui.styles.StyleProvider;

public class LabelStyleFactory {
    private LabelStyle labelStyle;

    public LabelStyleFactory(StyleProvider.StyleAttributes styles) {
        this.createLabelStyles(styles.name, styles.font, Float.valueOf(styles.fontSize), styles.color, styles.color);
    }

    public void createLabelStyles(String styleName, String font, float fontSize, String textColor,String carretColor) {
        labelStyle = new LabelStyle(font, fontSize, textColor, carretColor);
    }

    public LabelStyle getLabelStyle() {
        return labelStyle;
    }
}
