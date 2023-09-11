package org.foxesworld.newengine.utils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FontUtils {
    public static Map<String, Font> fonts = new HashMap<String, Font>();

    public static Font getFont(String name, float size) {
        if (!name.equals("")) {
            try {
                if (fonts.containsKey(name)) {
                    return fonts.get(name).deriveFont(size);
                }
                Font font = null;
                try {
                    font = Font.createFont(0, FontUtils.class.getResourceAsStream("/assets/fonts/" + name + ".ttf"));
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        font = Font.createFont(0, FontUtils.class.getResourceAsStream("/assets/fonts/" + name + ".otf"));
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                fonts.put(name, font);
                return font.deriveFont(size);
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.send("Failed to create font!", 0, true);
                return null;
            }
        }
        return null;
    }

    public static Color hexToColor(String hex) {
        hex = hex.replace("#", "");

        int red = Integer.parseInt(hex.substring(0, 2), 16);
        int green = Integer.parseInt(hex.substring(2, 4), 16);
        int blue = Integer.parseInt(hex.substring(4, 6), 16);

        return new Color(red, green, blue);
    }
}
