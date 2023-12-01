package org.foxesworld.engine.news;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;

public class NewsPanel extends JPanel {

    private JScrollPane scrollPane;
    private JPanel contentPanel;

    public NewsPanel(List<News> newsList) {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        for (News news : newsList) {
            contentPanel.add(createNewsPanel(news));
            contentPanel.add(Box.createVerticalStrut(0)); // Add some vertical space between news items
        }
        contentPanel.setOpaque(false); // Make the content panel transparent

        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false); // Make the scroll pane transparent
        scrollPane.getViewport().setOpaque(false); // Make the viewport transparent

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        setOpaque(false); // Make the NewsPanel transparent
    }

    private JPanel createNewsPanel(News news) {
        JPanel newsPanel = new JPanel();
        newsPanel.setLayout(new BoxLayout(newsPanel, BoxLayout.Y_AXIS));
        newsPanel.setOpaque(false); // Make the newsPanel transparent

        // Create a separate panel for the upper part of the news
        JPanel upperPanel = new JPanel();
        upperPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        upperPanel.setBackground(Color.LIGHT_GRAY); // Set the background color

        try {
            // Display the community photo with rounded corners
            ImageIcon communityIcon = new ImageIcon(new URL(news.getCommunityPhotoUrl()));
            Image communityImage = communityIcon.getImage();
            communityIcon = new ImageIcon(getRoundedImage(communityImage, 50, 50));
            JLabel communityLabel = new JLabel(communityIcon);
            communityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            upperPanel.add(communityLabel);

            // Display the community name
            JLabel communityNameLabel = new JLabel(news.getCommunityName());
            communityNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            upperPanel.add(communityNameLabel);
        } catch (IOException e) {
            e.printStackTrace();
            // Debugging: Print error message if image loading fails
            System.err.println("Error loading community photo: " + e.getMessage());
        }

        // Add the publication date to the upper panel
        JLabel dateLabel = new JLabel("Published on: " + formatDate(news.getPublicationDate()));
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        upperPanel.add(dateLabel);

        // Add the upper panel to the main news panel
        newsPanel.add(upperPanel);

        // Display the news text as a title
        JLabel titleLabel = new JLabel(news.getText());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Set the font and style as needed
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        newsPanel.add(titleLabel);

        // Display photos in full size
        for (String photoUrl : news.getOriginalPhotoUrls()) {
            try {
                ImageIcon imageIcon = new ImageIcon(new URL(photoUrl));
                Image image = imageIcon.getImage();
                imageIcon = new ImageIcon(image);
                JLabel photoLabel = new JLabel(imageIcon);
                // Center the photo
                photoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                newsPanel.add(photoLabel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Display statistics (views, likes, comments) at the bottom-left
        JPanel statisticsPanel = new JPanel();
        statisticsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        // Define the statistics labels and their values
        String[] statisticsLabels = {"Views", "Likes", "Comments"};
        int[] statisticsValues = {news.getViews(), news.getLikes(), news.getComments()};

        // Create labels in a loop
        for (int i = 0; i < statisticsLabels.length; i++) {
            JLabel label = new JLabel(statisticsLabels[i] + ": " + statisticsValues[i]);
            statisticsPanel.add(label);
        }

        newsPanel.add(statisticsPanel);

        return newsPanel;
    }

    private String formatDate(long unixTimestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(unixTimestamp * 1000L); // Convert seconds to milliseconds
    }

    private Image getRoundedImage(Image image, int width, int height) {
        BufferedImage roundedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = roundedImage.createGraphics();
        g2.setClip(new Ellipse2D.Float(0, 0, width, height));
        g2.drawImage(image, 0, 0, width, height, null);
        g2.dispose();
        return roundedImage;
    }
}
