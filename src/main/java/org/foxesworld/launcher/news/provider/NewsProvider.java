package org.foxesworld.launcher.news.provider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.foxesworld.engine.Engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsProvider {
    private final Engine engine;
    private static final String VK_API_URL = "https://api.vk.com/method/wall.get";
    private static final String[] STATS_VALUES_KEYS = {"views", "likes", "comments", "reposts"};
    private final Gson gson = new Gson();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final long CACHE_VALIDITY_DURATION = 60 * 60 * 1000; // 1 hour in milliseconds
    private List<NewsAttributes> cachedNewsAttributesList = null;
    private long lastFetchTime = 0;

    public NewsProvider(Engine engine) {
        this.engine = engine;
    }

    public static String[] getStatsValuesKeys() {
        return STATS_VALUES_KEYS;
    }

    public void fetchNews(NewsFetchCallback callback) {
        if (isCacheValid()) {
            callback.onNewsFetched(cachedNewsAttributesList);
            return;
        }

        executorService.submit(() -> {
            List<NewsAttributes> newsAttributesList = new ArrayList<>();
            try {
                URL url = new URL(buildUrl());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    JsonObject jsonResponse = gson.fromJson(reader, JsonObject.class);
                    parseResponse(jsonResponse, newsAttributesList);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            updateCache(newsAttributesList);
            callback.onNewsFetched(newsAttributesList);
        });
    }

    private boolean isCacheValid() {
        return cachedNewsAttributesList != null && (System.currentTimeMillis() - lastFetchTime) < CACHE_VALIDITY_DURATION;
    }

    private void updateCache(List<NewsAttributes> newsAttributesList) {
        this.cachedNewsAttributesList = newsAttributesList;
        this.lastFetchTime = System.currentTimeMillis();
    }

    private void parseResponse(JsonObject jsonResponse, List<NewsAttributes> newsAttributesList) {
        JsonArray posts = jsonResponse.getAsJsonObject("response").getAsJsonArray("items");
        String communityName = jsonResponse.getAsJsonObject("response").getAsJsonArray("groups")
                .get(0).getAsJsonObject().get("name").getAsString();
        String communityPhotoUrl = jsonResponse.getAsJsonObject("response").getAsJsonArray("groups")
                .get(0).getAsJsonObject().get("photo_50").getAsString();

        NewsAttributes.setCommunityName(communityName);
        NewsAttributes.setCommunityPhotoUrl(communityPhotoUrl);

        for (JsonElement postElement : posts) {
            JsonObject post = postElement.getAsJsonObject();
            NewsAttributes newsAttributes = createNewsAttributes(post);
            newsAttributesList.add(newsAttributes);
        }
    }

    private NewsAttributes createNewsAttributes(JsonObject post) {
        String text = post.get("text").getAsString();
        Map<String, Integer> statsValues = extractStatsValues(post);
        long date = post.get("date").getAsLong();
        List<String> tooltipPhotoUrls = new ArrayList<>();
        List<String> originalPhotoUrls = new ArrayList<>();
        extractPhotoUrls(post, tooltipPhotoUrls, originalPhotoUrls);

        return new NewsAttributes(text, statsValues, date, tooltipPhotoUrls, originalPhotoUrls);
    }

    private Map<String, Integer> extractStatsValues(JsonObject post) {
        Map<String, Integer> statsValues = new HashMap<>();
        for (String key : STATS_VALUES_KEYS) {
            Optional<JsonElement> statElement = Optional.ofNullable(post.getAsJsonObject(key));
            int statVal = statElement.map(e -> e.getAsJsonObject().get("count").getAsInt()).orElse(0);
            statsValues.put(key, statVal);
        }
        return statsValues;
    }

    private void extractPhotoUrls(JsonObject post, List<String> tooltipPhotoUrls, List<String> originalPhotoUrls) {
        JsonArray attachments = post.getAsJsonArray("attachments");
        if (attachments != null) {
            for (JsonElement attachmentElement : attachments) {
                JsonObject attachment = attachmentElement.getAsJsonObject();
                if ("photo".equals(attachment.get("type").getAsString())) {
                    JsonObject photo = attachment.getAsJsonObject("photo");
                    tooltipPhotoUrls.add(getPhotoUrl(photo));
                    originalPhotoUrls.add(getOriginalPhotoUrl(photo));
                }
            }
        }
    }

    private String getOriginalPhotoUrl(JsonObject photo) {
        JsonArray sizes = photo.getAsJsonArray("sizes");
        return sizes.get(sizes.size() - 1).getAsJsonObject().get("url").getAsString();
    }

    private String getPhotoUrl(JsonObject photo) {
        JsonArray sizes = photo.getAsJsonArray("sizes");
        return sizes.get(0).getAsJsonObject().get("url").getAsString();
    }

    private String buildUrl() {
        return VK_API_URL + "?domain=" + this.engine.getEngineData().getGroupDomain() +
                "&access_token=" + this.engine.getEngineData().getAccessToken() +
                "&count=100&extended=1&v=" + this.engine.getEngineData().getVkAPIversion();
    }

    public interface NewsFetchCallback {
        void onNewsFetched(List<NewsAttributes> newsAttributesList);
    }
}
