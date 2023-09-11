package org.foxesworld.newengine.gui.components.textfield;

import org.foxesworld.newengine.APP;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class TextfieldStyleFactory {
    private Map<String, TextfieldStyle> textfieldStyles = new HashMap<>();

    public TextfieldStyleFactory(List styles) {
        for(Object style: styles){
            this.createTextfieldStyles(String.valueOf(style), "assets/ui/input/"+style+".png",Color.BLACK,Color.decode("0xd4dc7b"));
        }
    }

    public TextfieldStyle createTextfieldStyles(
            String styleName,
            String imagePath,

            Color textColor,
            Color carretColor
    ) {

        TextfieldStyle textfieldStyle = new TextfieldStyle(imagePath, textColor, carretColor);
        textfieldStyles.put(styleName, textfieldStyle);

        return textfieldStyle;
    }

    public TextfieldStyle getTextfieldStyle(String styleName) {
        return textfieldStyles.get(styleName);
    }
}
