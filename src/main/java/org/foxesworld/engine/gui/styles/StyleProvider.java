package org.foxesworld.engine.gui.styles;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.foxesworld.engine.Engine;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StyleProvider {

    private final Map<String, Map<String, StyleAttributes>> elementStyles = new HashMap<>();
    private final String[] styles = {"button",  "checkBox", "label", "multiButton", "passField", "progressBar", "dropBox", "serverBox", "textField", "slider"};
    private final Engine engine;

    public StyleProvider(Engine engine) {
        Engine.LOGGER.info("Loading styles...");
        this.engine = engine;
        for(String style: this.styles){
            loadStyle(style);
        }
    }
    private void loadStyle(String component) {
        if (elementStyles.containsKey(component)) {
            return;
        }

        String stylePath = "assets/styles/" + component + ".json";
        try {
            Gson gson = new Gson();
            InputStreamReader reader = new InputStreamReader(
                    Objects.requireNonNull(StyleProvider.class.getClassLoader().getResourceAsStream(stylePath)),
                    StandardCharsets.UTF_8
            );
            engine.getLOGGER().debug("Loading " + component + " style from " + stylePath);

            JsonObject jsonRoot = gson.fromJson(reader, JsonObject.class);
            JsonObject stylesObject = jsonRoot.getAsJsonObject("styles");

            Map<String, StyleAttributes> styleMap = new HashMap<>();

            JsonObject componentStyles = stylesObject.getAsJsonObject(component);
            for (Map.Entry<String, JsonElement> entry : componentStyles.entrySet()) {
                String styleName = entry.getKey();
                JsonObject styleData = entry.getValue().getAsJsonObject();

                StyleAttributes styleAttributes = gson.fromJson(styleData, StyleAttributes.class);
                styleMap.put(styleName, styleAttributes);
            }
            elementStyles.put(component, styleMap);

        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
    }
    public Map<String, Map<String, StyleAttributes>> getElementStyles() {
        return elementStyles;
    }
    public static class StyleAttributes {
        private String name;
        private String backgroundImage;
        private String background;
        private String color;
        private String hoverColor;
        private String caretColor;
        private String align;
        private String borderColor;
        private String trackImage;
        private String thumbImage;
        private int width;
        private int height;
        private int paddingX;
        private int paddingY;
        private String font;
        private int fontSize;
        private String texture;
        private boolean opaque;

        public String getName() {
            return name;
        }

        public String getBackgroundImage() {
            return backgroundImage;
        }

        public String getBackground() {
            return background;
        }

        public String getColor() {
            return color;
        }

        public String getHoverColor() {
            return hoverColor;
        }

        public String getCaretColor() {
            return caretColor;
        }

        public String getAlign() {
            return align;
        }

        public String getBorderColor() {
            return borderColor;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getFont() {
            return font;
        }

        public int getFontSize() {
            return fontSize;
        }

        public String getTexture() {
            return texture;
        }

        public boolean isOpaque() {
            return opaque;
        }

        public String getTrackImage() {
            return trackImage;
        }

        public int getPaddingX() {
            return paddingX;
        }

        public int getPaddingY() {
            return paddingY;
        }

        public String getThumbImage() {
            return thumbImage;
        }
    }
}
