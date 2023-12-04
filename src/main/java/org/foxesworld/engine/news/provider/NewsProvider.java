package org.foxesworld.engine.news.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.foxesworld.engine.Engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsProvider {
    private Engine engine;
    private final String VK_API_URL = "https://api.vk.com/method/wall.get";
    private static String[] statsValuesKeys = {"views", "likes", "comments", "reposts"};
    private String text;
    private Map<String, Integer> statsValues;
    private long date;
    private List<String> tooltipPhotoUrls;
    private List<String> originalPhotoUrls;
    public NewsProvider(Engine engine){
        this.engine = engine;
    }

    public List<NewsAttributes> fetchNews() {
        List<NewsAttributes> newsAttributesList = new ArrayList<>();

        try {
            // Construct the URL for VK API request
            URL url = new URL(buildUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Read the response
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                // Parse the JSON response
                JsonParser jsonParser = new JsonParser();
                JsonObject jsonResponse = jsonParser.parse(reader).getAsJsonObject();
                JsonArray posts = jsonResponse.getAsJsonObject("response").getAsJsonArray("items");
                NewsAttributes.setCommunityName(jsonResponse.getAsJsonObject("response").getAsJsonArray("groups").get(0).getAsJsonObject().get("name").getAsString());
                NewsAttributes.setCommunityPhotoUrl(jsonResponse.getAsJsonObject("response").getAsJsonArray("groups").get(0).getAsJsonObject().get("photo_50").getAsString());

                // Process each post
                for (JsonElement postElement : posts) {
                    JsonObject post = postElement.getAsJsonObject();
                    text = post.get("text").getAsString();
                    statsValues = new HashMap<>();
                    for(String value: statsValuesKeys){
                        int statVal = post.getAsJsonObject(value) != null ? post.getAsJsonObject(value).get("count").getAsInt() : 0;
                        statsValues.put(value, statVal);
                    }

                    date = post.get("date").getAsLong(); // Get the publication date in seconds
                    tooltipPhotoUrls = new ArrayList<>();
                    originalPhotoUrls = new ArrayList<>();
                    // Process attachments
                    JsonArray attachments = post.getAsJsonArray("attachments");
                    if (attachments != null) {
                        for (JsonElement attachmentElement : attachments) {
                            JsonObject attachment = attachmentElement.getAsJsonObject();
                            String attachmentType = attachment.get("type").getAsString();
                            if (attachmentType.equals("photo")) {

                                JsonObject photo = attachment.getAsJsonObject("photo");
                                String tooltipUrl = getPhotoUrl(photo);
                                String originalUrl = getOriginalPhotoUrl(photo);
                                tooltipPhotoUrls.add(tooltipUrl);
                                originalPhotoUrls.add(originalUrl);
                            }
                            // Handle other attachment types (e.g., video, link) as needed
                        }
                    }

                    // Create a NewsAttributes object with the publication date and add it to the list
                    newsAttributesList.add(new NewsAttributes(this));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return newsAttributesList;
    }

    private String getOriginalPhotoUrl(JsonObject photo) {
        JsonArray sizes = photo.getAsJsonArray("sizes");
        JsonObject originalSize = sizes.get(sizes.size() - 1).getAsJsonObject();
        return originalSize.get("url").getAsString();
    }
    private String getPhotoUrl(JsonObject photo) {
        JsonArray sizes = photo.getAsJsonArray("sizes");
        JsonObject mediumSize = sizes.get(0).getAsJsonObject();
        return mediumSize.get("url").getAsString();
    }

    private String buildUrl() {
        StringBuilder urlBuilder = new StringBuilder(VK_API_URL);
        urlBuilder.append("?domain=").append(this.engine.getEngineData().getGroupDomain());
        urlBuilder.append("&access_token=").append(this.engine.getEngineData().getAccessToken());
        urlBuilder.append("&count=5"); // Adjust count as needed
        urlBuilder.append("&extended=1");
        urlBuilder.append("&v=").append(this.engine.getEngineData().getVkAPIversion());

        return urlBuilder.toString();
    }

    public String getText() {
        return text;
    }

    public Map<String, Integer> getStatsValues() {
        return statsValues;
    }

    public static String[] getStatsValuesKeys() {
        return statsValuesKeys;
    }

    public long getDate() {
        return date;
    }

    public List<String> getTooltipPhotoUrls() {
        return tooltipPhotoUrls;
    }

    public List<String> getOriginalPhotoUrls() {
        return originalPhotoUrls;
    }
}
