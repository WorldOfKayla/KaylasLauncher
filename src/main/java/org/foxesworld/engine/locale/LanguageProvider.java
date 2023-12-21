package org.foxesworld.engine.locale;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.foxesworld.engine.Engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageProvider {
    private final Map<String, Map<String, String>> localizationData = new HashMap<>();

    public LanguageProvider(Engine engine, String langFilePath) {
        String currentLang = engine.getCONFIG().getLang();
        try {
            Gson gson = new Gson();
            InputStreamReader reader = new InputStreamReader(engine.getClass().getResourceAsStream(langFilePath), StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(reader);
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }

            JsonElement jsonElement = gson.fromJson(jsonStringBuilder.toString(), JsonElement.class);
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            for (Map.Entry<String, JsonElement> categoryEntry : jsonObject.entrySet()) {
                JsonObject categoryData = categoryEntry.getValue().getAsJsonObject();
                Map<String, String> categoryMap = new HashMap<>();

                for (Map.Entry<String, JsonElement> localizedData : categoryData.entrySet()) {
                    String localizedKey = localizedData.getKey();
                    JsonObject localizedValues = localizedData.getValue().getAsJsonObject();

                    if (localizedValues.has(currentLang)) {
                        String localizedValue = localizedValues.get(currentLang).getAsString();
                        categoryMap.put(localizedKey, localizedValue);
                    }
                }

                localizationData.put(categoryEntry.getKey(), categoryMap);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getString(String key) {
        if (key != null) {
            if (key.contains(".")) {
                String[] parts = key.split("\\.");
                if (parts.length == 2) {
                    String category = parts[0];
                    String localizedKey = parts[1];
                    if (localizationData.containsKey(category)) {
                        Map<String, String> categoryMap = localizationData.get(category);
                        if (categoryMap.containsKey(localizedKey)) {
                            return categoryMap.get(localizedKey);
                        }
                    }
                }
            }
        }
        return key;
    }
}
