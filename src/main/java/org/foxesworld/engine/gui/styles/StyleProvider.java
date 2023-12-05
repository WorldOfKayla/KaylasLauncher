package org.foxesworld.engine.gui.styles;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import org.foxesworld.engine.Engine;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StyleProvider {

    private Map<String, Map<String, StyleAttributes>> elementStyles = new HashMap<>();
    private String[] styles = {"button",  "checkBox", "label", "multiButton", "passField", "progressBar", "scrollBox", "serverBox", "textField"};
    private Engine engine;

    public StyleProvider(Engine engine) {
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
