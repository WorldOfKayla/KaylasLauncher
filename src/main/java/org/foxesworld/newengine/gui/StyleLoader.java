package org.foxesworld.newengine.gui;

import com.google.gson.*;
import org.foxesworld.newengine.APP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StyleLoader {

    private final String stylesJson = "styles.json";
    private Map<String, List<String>> elementStyles = new HashMap<>();
    private APP app;

    public StyleLoader(APP app) {
        this.app = app;
        this.loadStyles();
    }

    private void loadStyles() {
        try {
            Gson gson = new Gson();
            InputStreamReader reader = new InputStreamReader(StyleLoader.class.getClassLoader().getResourceAsStream(this.stylesJson), StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(reader);

            JsonArray jsonArray = gson.fromJson(bufferedReader, JsonArray.class);

            if (jsonArray.isJsonArray()) {

                for (JsonElement arrayElement : jsonArray) {
                    // Проверьте, является ли элемент объектом
                    if (arrayElement.isJsonObject()) {
                        JsonObject jsonObject = arrayElement.getAsJsonObject();

                        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                            String element = entry.getKey();
                            List styles = new ArrayList();
                            JsonArray values = entry.getValue().getAsJsonArray();

                            for (JsonElement valueElement : values) {
                                String value = valueElement.getAsString();
                                styles.add(value);
                            }
                            this.elementStyles.put(element, styles);
                        }
                    }
                }
            }
            bufferedReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, List<String>> getElementStyles() {
        return elementStyles;
    }
}
