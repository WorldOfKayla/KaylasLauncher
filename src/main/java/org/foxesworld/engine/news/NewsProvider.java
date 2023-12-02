package org.foxesworld.engine.news;

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
import java.util.ArrayList;
import java.util.List;

public class NewsProvider {
    private Engine engine;
    private static final String VK_API_URL = "https://api.vk.com/method/wall.get";
    private static final String ACCESS_TOKEN = "ccdd6e40ccdd6e40ccdd6e40ecccaf012ecccddccdd6e4092074eb9f3eea48edf8a6e39";
    private static final String GROUP_DOMAIN = "foxesworlds"; // Replace with the domain of the VK group/page

    public NewsProvider(Engine engine){
        this.engine = engine;
    }

    public List<News> fetchNews() {
        List<News> newsList = new ArrayList<>();

        try {
            // Construct the URL for VK API request
            URL url = new URL(buildUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Read the response
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                // Parse the JSON response
                JsonParser jsonParser = new JsonParser();
                JsonObject jsonResponse = jsonParser.parse(reader).getAsJsonObject();
                JsonArray posts = jsonResponse.getAsJsonObject("response").getAsJsonArray("items");
                News.setCommunityName(jsonResponse.getAsJsonObject("response").getAsJsonArray("groups").get(0).getAsJsonObject().get("name").getAsString());
                News.setCommunityPhotoUrl(jsonResponse.getAsJsonObject("response").getAsJsonArray("groups").get(0).getAsJsonObject().get("photo_50").getAsString());

                // Process each post
                for (JsonElement postElement : posts) {
                    JsonObject post = postElement.getAsJsonObject();
                    String text = post.get("text").getAsString();
                    int views = post.getAsJsonObject("views").get("count").getAsInt();
                    int likes = post.getAsJsonObject("likes").get("count").getAsInt();
                    int comments = post.getAsJsonObject("comments").get("count").getAsInt();
                    int reposts = post.getAsJsonObject("reposts").get("count").getAsInt();
                    long date = post.get("date").getAsLong(); // Get the publication date in seconds

                    List<String> tooltipPhotoUrls = new ArrayList<>();
                    List<String> originalPhotoUrls = new ArrayList<>();


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

                    // Create a News object with the publication date and add it to the list
                    newsList.add(new News(text, tooltipPhotoUrls, originalPhotoUrls, date, views, likes, comments, reposts));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return newsList;
    }

    private String getOriginalPhotoUrl(JsonObject photo) {
        JsonArray sizes = photo.getAsJsonArray("sizes");
        // Choose the size you need, for example "o" for the original size
        JsonObject originalSize = sizes.get(sizes.size() - 1).getAsJsonObject();
        return originalSize.get("url").getAsString();
    }
    private String getPhotoUrl(JsonObject photo) {
        JsonArray sizes = photo.getAsJsonArray("sizes");
        // Choose the size you need, for example "z" for the medium size
        JsonObject mediumSize = sizes.get(0).getAsJsonObject();
        return mediumSize.get("url").getAsString();
    }

    private String buildUrl() {
        StringBuilder urlBuilder = new StringBuilder(VK_API_URL);
        urlBuilder.append("?domain=").append(GROUP_DOMAIN);
        urlBuilder.append("&access_token=").append(ACCESS_TOKEN);
        urlBuilder.append("&count=5"); // Adjust count as needed
        urlBuilder.append("&extended=1");
        urlBuilder.append("&v=5.101"); // VK API version

        return urlBuilder.toString();
    }
}
