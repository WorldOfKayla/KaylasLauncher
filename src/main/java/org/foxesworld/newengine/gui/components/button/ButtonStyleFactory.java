package org.foxesworld.newengine.gui.components.button;

import org.foxesworld.newengine.gui.styles.StyleProvider;

public class ButtonStyleFactory {
    private ButtonStyle buttonStyle;

    public ButtonStyleFactory(StyleProvider.StyleAttributes styles) {
        this.buttonStyle = new ButtonStyle(styles);
    }

    public ButtonStyle getButtonStyle() {
        return buttonStyle;
    }
}
