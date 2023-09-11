package org.foxesworld.newengine.gui.components.button;

import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.gui.components.Align;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.foxesworld.newengine.gui.components.Align.CENTER;

public class ButtonStyleFactory {
    private APP app;
    private List buttonTextures;
    private Map<String, ButtonStyle> buttonStyles = new HashMap<>();

    public ButtonStyleFactory(APP app, List styles) {
        this.app = app;
        for(Object style: styles){
            this.createButtonStyle(String.valueOf(style), "Roboto-Black", "assets/ui/button/"+style+".png", 11f, Color.decode("0xd4dc7b"), true, CENTER);
        }
    }

    public ButtonStyle createButtonStyle(
            String styleName,
            String fontName,
            String imagePath,
            float fontSize,
            Color textColor,
            boolean isVisible,
            Align align
    ) {

        ButtonStyle buttonStyle = new ButtonStyle(fontName, imagePath, fontSize, textColor, isVisible, align);
        buttonStyles.put(styleName, buttonStyle);

        return buttonStyle;
    }

    public ButtonStyle getButtonStyle(String styleName) {
        return buttonStyles.get(styleName);
    }
}
