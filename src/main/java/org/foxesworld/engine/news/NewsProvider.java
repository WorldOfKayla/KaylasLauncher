package org.foxesworld.engine.news;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NewsProvider {

    private static final String VK_API_URL = "https://api.vk.com/method/wall.get";
    private static final String ACCESS_TOKEN = "ccdd6e40ccdd6e40ccdd6e40ecccaf012ecccddccdd6e4092074eb9f3eea48edf8a6e39";
    private static final String GROUP_DOMAIN = "foxesworlds"; // Replace with the domain of the VK group/page

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

                // Process each post
                for (JsonElement postElement : posts) {
                    JsonObject post = postElement.getAsJsonObject();
                    String text = post.get("text").getAsString();
                    int views = post.getAsJsonObject("views").get("count").getAsInt();
                    int likes = post.getAsJsonObject("likes").get("count").getAsInt();
                    int comments = post.getAsJsonObject("comments").get("count").getAsInt();
                    long date = post.get("date").getAsLong(); // Get the publication date in seconds

                    List<String> photoUrls = new ArrayList<>();
                    // Process attachments
                    JsonArray attachments = post.getAsJsonArray("attachments");
                    if (attachments != null) {
                        for (JsonElement attachmentElement : attachments) {
                            JsonObject attachment = attachmentElement.getAsJsonObject();
                            String attachmentType = attachment.get("type").getAsString();
                            if (attachmentType.equals("photo")) {
                                JsonObject photo = attachment.getAsJsonObject("photo");
                                String photoUrl = getPhotoUrl(photo);
                                photoUrls.add(photoUrl);
                            }
                            // Handle other attachment types (e.g., video, link) as needed
                        }
                    }

                    // Create a News object with the publication date and add it to the list
                    newsList.add(new News(text, photoUrls, date, views, likes, comments));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return newsList;
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
