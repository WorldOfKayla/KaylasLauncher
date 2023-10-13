package org.foxesworld.newengine.gui.components;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class StyleProvider {

    private final String stylesJson = "styles.json";
    private Map<String, Map<String, StyleAttributes>> elementStyles = new HashMap<>();

    public StyleProvider() {
        this.loadStyles();
    }

        private void loadStyles() {
            try {
                Gson gson = new Gson();
                InputStreamReader reader = new InputStreamReader(
                        StyleProvider.class.getClassLoader().getResourceAsStream(this.stylesJson),
                        StandardCharsets.UTF_8
                );

                JsonObject jsonRoot = gson.fromJson(reader, JsonObject.class);
                JsonObject stylesObject = jsonRoot.getAsJsonObject("styles");

                for (Map.Entry<String, JsonElement> entry : stylesObject.entrySet()) {
                    String elementType = entry.getKey();
                    Map<String, StyleAttributes> styleMap = new HashMap<>();

                    JsonObject styleBlock = entry.getValue().getAsJsonObject();
                    for (Map.Entry<String, JsonElement> styleEntry : styleBlock.entrySet()) {
                        String styleName = styleEntry.getKey();
                        JsonObject styleData = styleEntry.getValue().getAsJsonObject();

                        StyleAttributes styleAttributes = gson.fromJson(styleData, StyleAttributes.class);
                        styleMap.put(styleName, styleAttributes);
                    }

                    elementStyles.put(elementType, styleMap);
                }
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }


    public Map<String, Map<String, StyleAttributes>> getElementStyles() {
        return elementStyles;
    }

    public Map<String, StyleAttributes> getStylesForElementType(String elementType) {
        return elementStyles.get(elementType);
    }

    public class StyleAttributes {
        @SerializedName("name")
        public String name;

        @SerializedName("background")
        public String background;

        @SerializedName("forgeground")
        public String forgeground;

        @SerializedName("borderColor")
        public String borderColor;
        @SerializedName("width")
        public int width;

        @SerializedName("height")
        public int height;

        @SerializedName("font")
        public String font;

        @SerializedName("fontSize")
        public int fontSize;

        @SerializedName("color")
        public String color;

        @SerializedName("texture")
        public String texture;
    }
}
