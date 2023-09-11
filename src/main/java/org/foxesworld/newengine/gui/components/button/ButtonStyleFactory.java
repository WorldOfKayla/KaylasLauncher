package org.foxesworld.newengine.gui.components.button;

import org.foxesworld.newengine.gui.components.Align;
import org.foxesworld.newengine.gui.components.StyleLoader;

import static org.foxesworld.newengine.gui.components.Align.CENTER;

public class ButtonStyleFactory {
    private ButtonStyle buttonStyle;

    public ButtonStyleFactory(StyleLoader.StyleAttributes styles) {
        this.createButtonStyle(styles.name, "assets/ui/button/"+styles.texture+".png", styles.font, styles.color, Float.valueOf(styles.fontSize), true, CENTER);
    }

    public void createButtonStyle(String styleName, String imagePath, String font, String color, float fontSize, boolean isVisible, Align align) {
        buttonStyle = new ButtonStyle(imagePath, font, fontSize, color, isVisible, align);
    }

    public ButtonStyle getButtonStyle() {
        return buttonStyle;
    }
}
