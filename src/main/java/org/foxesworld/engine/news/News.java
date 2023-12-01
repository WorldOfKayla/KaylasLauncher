package org.foxesworld.engine.news;

import java.util.List;

public class News {
    private String text;
    private List<String> photoUrls;
    private long publicationDate;
    private int views;
    private int likes;
    private int comments;

    public News(String text, List<String> photoUrls, long publicationDate, int views, int likes, int comments) {
        this.text = text;
        this.photoUrls = photoUrls;
        this.publicationDate = publicationDate;
        this.views = views;
        this.likes = likes;
        this.comments = comments;
    }

    public String getText() {
        return text;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
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
}
