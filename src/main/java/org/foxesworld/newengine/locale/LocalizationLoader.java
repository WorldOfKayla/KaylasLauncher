package org.foxesworld.newengine.locale;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.foxesworld.newengine.FormCreatorApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class LocalizationLoader {
    private FormCreatorApp formCreatorApp;
    private Map<String, Map<String, String>> localizationData = new HashMap<>();

    public LocalizationLoader(FormCreatorApp formCreatorApp) {
        this.formCreatorApp = formCreatorApp;
        try {
            Gson gson = new Gson();
            InputStreamReader reader = new InputStreamReader(formCreatorApp.getLangFile(), StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(reader);
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }

            JsonElement jsonElement = gson.fromJson(jsonStringBuilder.toString(), JsonElement.class);
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            Set<Entry<String, JsonElement>> entrySet = jsonObject.entrySet();
            for (Entry<String, JsonElement> entry : entrySet) {
                JsonObject langData = entry.getValue().getAsJsonObject();
                Map<String, String> langMap = new HashMap<>();
                Set<Entry<String, JsonElement>> langEntrySet = langData.entrySet();
                for (Entry<String, JsonElement> langEntry : langEntrySet) {
                    langMap.put(langEntry.getKey(), langEntry.getValue().getAsString());
                }
                localizationData.put(entry.getKey(), langMap);
            }

            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getString(String key) {
        String lang = formCreatorApp.getLOCALE();
        if (localizationData.containsKey(key)) {
            Map<String, String> langMap = localizationData.get(key);
            if (langMap.containsKey(lang)) {
                return langMap.get(lang);
            }
        }
        return key;
    }
}
