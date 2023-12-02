package org.foxesworld.engine.news;

import java.util.List;

public class News {
    private String text;
    private List<String> tooltipPhotoUrls;
    private List<String> originalPhotoUrls;
    private long publicationDate;
    private int views;
    private int likes;
    private int comments;
    private int reposts;
    private static String groupName; // Added field for community name
    private static String groupPicture; // Added field for community photo URL

    public News(String text, List<String> tooltipPhotoUrls, List<String> originalPhotoUrls, long publicationDate, int views, int likes, int comments, int reposts) {
        this.text = text;
        this.tooltipPhotoUrls = tooltipPhotoUrls;
        this.originalPhotoUrls = originalPhotoUrls;
        this.publicationDate = publicationDate;
        this.views = views;
        this.likes = likes;
        this.comments = comments;
        this.reposts = reposts;
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
