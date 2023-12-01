package org.foxesworld.engine.news;

import java.util.Date;
import java.util.List;

public class News {
    private String text;
    private List<String> photoUrls;
    private long publicationDate;

    public News(String text, List<String> photoUrls, long publicationDate) {
        this.text = text;
        this.photoUrls = photoUrls;
        this.publicationDate = publicationDate;
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
}
