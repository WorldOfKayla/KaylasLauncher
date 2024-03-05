package org.foxesworld.launcher.news.provider;

import java.util.List;

public class NewsAttributes {
    private final String text;
    private final List<String> tooltipPhotoUrls, originalPhotoUrls;
    private final long publicationDate;
    private final int views, likes, comments, reposts;
    private static String groupName, groupPicture;

    public NewsAttributes(NewsProvider newsProvider) {
        this.text = newsProvider.getText();
        this.tooltipPhotoUrls = newsProvider.getTooltipPhotoUrls();
        this.originalPhotoUrls = newsProvider.getOriginalPhotoUrls();
        this.publicationDate = newsProvider.getDate();
        this.views = newsProvider.getStatsValues().get("views");
        this.likes = newsProvider.getStatsValues().get("likes");
        this.comments = newsProvider.getStatsValues().get("comments");
        this.reposts = newsProvider.getStatsValues().get("reposts");
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
