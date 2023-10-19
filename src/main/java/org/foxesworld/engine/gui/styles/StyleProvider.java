package org.foxesworld.engine.gui.styles;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import org.foxesworld.engine.AppFrame;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class StyleProvider {

    private Map<String, Map<String, StyleAttributes>> elementStyles = new HashMap<>();
    private AppFrame appFrame;

    public StyleProvider(AppFrame appFrame) {
        this.appFrame = appFrame;
    }

    public Map<String, StyleAttributes> loadStyle(String component) {
        Map<String, Map<String, StyleAttributes>> elementStyles = new HashMap<>();
        String stylePath = "assets/styles/" + component + ".json";
        try {
            Gson gson = new Gson();
            InputStreamReader reader = new InputStreamReader(
                    StyleProvider.class.getClassLoader().getResourceAsStream(stylePath),
                    StandardCharsets.UTF_8
            );
            appFrame.getLOGGER().debug("Loading " + component + " style from " + stylePath);

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
        return elementStyles.get(component);
    }

    public Map<String, Map<String, StyleAttributes>> getElementStyles() {
        return elementStyles;
    }

    public class StyleAttributes {

        @SerializedName("name")
        public String name;
        @SerializedName("backgroundImage")
        public String backgroundImage;

        @SerializedName("background")
        public String background;
        @SerializedName("color")
        public String color;
        @SerializedName("caretColor")
        public String caretColor;
        @SerializedName("align")
        public String align;
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

        @SerializedName("texture")
        public String texture;
    }
}
