package org.foxesworld.engine.news;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class NewsPanel extends JPanel {

    public NewsPanel(List<News> newsList) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        for (News news : newsList) {
            add(createNewsPanel(news));
            add(Box.createVerticalStrut(10)); // Add some vertical space between news items
        }
    }

    private JPanel createNewsPanel(News news) {
        JPanel newsPanel = new JPanel();
        newsPanel.setLayout(new BoxLayout(newsPanel, BoxLayout.Y_AXIS));

        JLabel textLabel = new JLabel(news.getText());
        newsPanel.add(textLabel);

        JLabel dateLabel = new JLabel("Published on: " + this.getFormattedDate(news.getPublicationDate()));
        newsPanel.add(dateLabel);

        // Display photos in full size
        for (String photoUrl : news.getPhotoUrls()) {
            try {
                ImageIcon imageIcon = new ImageIcon(new URL(photoUrl));
                Image image = imageIcon.getImage();
                imageIcon = new ImageIcon(image);
                JLabel photoLabel = new JLabel(imageIcon);
                newsPanel.add(photoLabel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return newsPanel;
    }

    public String getFormattedDate(long publicationDate) {
        // Convert UNIX timestamp to LocalDateTime
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(publicationDate), ZoneId.systemDefault());

        // Format the date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()));
    }

    public static void main(String[] args) {
        // Assuming you already have a NewsProvider instance
        NewsProvider newsProvider = new NewsProvider();
        List<News> newsList = newsProvider.fetchNews();

        // Create a JFrame and add the NewsPanel
        JFrame frame = new JFrame("News Panel Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.getContentPane().add(new NewsPanel(newsList));
        frame.setVisible(true);
    }
}
