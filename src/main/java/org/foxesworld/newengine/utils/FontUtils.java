package org.foxesworld.newengine.utils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FontUtils {
    public static Map<String, Font> fonts = new HashMap<String, Font>();

    public static Font getFont(String name, float size) {
        try {
            if (fonts.containsKey(name)) {
                return fonts.get(name).deriveFont(size);
            }
            Font font = null;
            try {
                font = Font.createFont(0, FontUtils.class.getResourceAsStream("/assets/fonts/" + name + ".ttf"));
            }
            catch (Exception e) {
                e.printStackTrace();
                try {
                    font = Font.createFont(0, FontUtils.class.getResourceAsStream("/assets/fonts/" + name + ".otf"));
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            fonts.put(name, font);
            return font.deriveFont(size);
        }
        catch (Exception e) {
            e.printStackTrace();
            LogUtils.send("Failed to create font!", 0, true);
            return null;
        }
    }
}
