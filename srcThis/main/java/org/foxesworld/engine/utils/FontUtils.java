package org.foxesworld.engine.utils;

import org.foxesworld.engine.Engine;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FontUtils {
    public static Map<String, Font> fonts = new HashMap<>();

    private Engine engine;
    public FontUtils(Engine engine){
        this.engine = engine;
    }
    public Font getFont(String name, float size) {
        if (!name.equals("")) {
            try {
                if (fonts.containsKey(name)) {
                    return fonts.get(name).deriveFont(size);
                }
                Font font = null;
                try {
                    font = Font.createFont(0, FontUtils.class.getResourceAsStream("/assets/fonts/" + name + ".ttf"));
                    this.engine.getLOGGER().info("Created font - "+name);
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        font = Font.createFont(0, FontUtils.class.getResourceAsStream("/assets/fonts/" + name + ".otf"));
                        this.engine.getLOGGER().info("Created font - "+name);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                fonts.put(name, font);
                return font.deriveFont(size);
            } catch (Exception e) {
                e.printStackTrace();
                this.engine.getLOGGER().error("Failed to create font!");
                return null;
            }
        }
        return null;
    }

    public static Color hexToColor(String hex) {
        if (hex != null) {
            if (!hex.equals("")) {
                hex = hex.replace("#", "");
                int red = Integer.parseInt(hex.substring(0, 2), 16);
                int green = Integer.parseInt(hex.substring(2, 4), 16);
                int blue = Integer.parseInt(hex.substring(4, 6), 16);

                int alpha = 255;
                if (hex.length() == 8) {
                    alpha = Integer.parseInt(hex.substring(6, 8), 16);
                }
                return new Color(red, green, blue, alpha);
            }
        }
        return new Color(255, 255, 255);
    }

}
