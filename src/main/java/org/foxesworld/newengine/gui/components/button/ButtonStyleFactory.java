package org.foxesworld.newengine.gui.components.button;

import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.gui.components.Align;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.foxesworld.newengine.gui.components.Align.CENTER;

public class ButtonStyleFactory {
    private Map<String, ButtonStyle> buttonStyles = new HashMap<>();

    public ButtonStyleFactory(List styles) {
        for(Object style: styles){
            this.createButtonStyle(String.valueOf(style), "assets/ui/button/"+style+".png", Color.decode("0xd4dc7b"), true, CENTER);
        }
    }

    public ButtonStyle createButtonStyle(
            String styleName,
            String imagePath,
            Color textColor,
            boolean isVisible,
            Align align
    ) {

        ButtonStyle buttonStyle = new ButtonStyle(imagePath, textColor, isVisible, align);
        buttonStyles.put(styleName, buttonStyle);

        return buttonStyle;
    }

    public ButtonStyle getButtonStyle(String styleName) {
        return buttonStyles.get(styleName);
    }
}
