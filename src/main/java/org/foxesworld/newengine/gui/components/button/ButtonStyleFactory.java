package org.foxesworld.newengine.gui.components.button;

import org.foxesworld.newengine.gui.components.Align;
import org.foxesworld.newengine.gui.styles.StyleProvider;

import static org.foxesworld.newengine.gui.components.Align.CENTER;

public class ButtonStyleFactory {
    private ButtonStyle buttonStyle;

    public ButtonStyleFactory(StyleProvider.StyleAttributes styles) {
        this.createButtonStyle(styles.name, styles.texture, styles.font, styles.width, styles.height, styles.color, Float.valueOf(styles.fontSize), true, CENTER);
    }

    public void createButtonStyle(String styleName, String imagePath, String font, int width, int height, String color, float fontSize, boolean isVisible, Align align) {
        buttonStyle = new ButtonStyle(imagePath, font, width, height, fontSize, color, isVisible, align);
    }

    public ButtonStyle getButtonStyle() {
        return buttonStyle;
    }
}
