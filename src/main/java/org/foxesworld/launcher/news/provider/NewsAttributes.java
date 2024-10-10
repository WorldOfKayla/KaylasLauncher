package org.foxesworld.launcher.news.provider;

import java.util.List;
import java.util.Map;

public class NewsAttributes {
    private final String text;
    private final List<String> tooltipPhotoUrls, originalPhotoUrls;
    private final long publicationDate;
    private final int views, likes, comments, reposts;
    private static String groupName, groupPicture;

    public NewsAttributes(String text, Map<String, Integer> statsValues, long publicationDate,
                          List<String> tooltipPhotoUrls, List<String> originalPhotoUrls) {
        this.text = text;
        this.tooltipPhotoUrls = tooltipPhotoUrls;
        this.originalPhotoUrls = originalPhotoUrls;
        this.publicationDate = publicationDate;
        this.views = statsValues.getOrDefault("views", 0);
        this.likes = statsValues.getOrDefault("likes", 0);
        this.comments = statsValues.getOrDefault("comments", 0);
        this.reposts = statsValues.getOrDefault("reposts", 0);
    }


    public String getText() {
        return text;
    }

    public List<String> getTooltipPhotoUrls() {
        return tooltipPhotoUrls;
    }

    public List<String> getOriginalPhotoUrls() {
        return originalPhotoUrls;
    }

    public long getPublicationDate() {
        return publicationDate;
    }

    public int getViews() {
        return views;
    }

    public int getLikes() {
        return likes;
    }

    public int getComments() {
        return comments;
    }

    public int getReposts() {
        return reposts;
    }

    public String getCommunityName() {
        return groupName;
    }

    public String getCommunityPhotoUrl() {
        return groupPicture;
    }

    public static void setCommunityName(String communityName) {
        groupName = communityName;
    }

    public static void setCommunityPhotoUrl(String communityPhotoUrl) {
        groupPicture = communityPhotoUrl;
    }
}
