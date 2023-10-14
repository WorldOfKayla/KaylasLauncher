package org.foxesworld.newengine.gui.components.textfield;

import org.foxesworld.newengine.gui.styles.StyleProvider;

import java.awt.*;

public class TextfieldStyleFactory {
    private TextfieldStyle textfieldStyle;

    public TextfieldStyleFactory(StyleProvider.StyleAttributes styles) {
        textfieldStyle = new TextfieldStyle(styles);
    }

    public TextfieldStyle getTextfieldStyle() {
        return textfieldStyle;
    }
}
