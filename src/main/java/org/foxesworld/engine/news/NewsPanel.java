package org.foxesworld.engine.news;

import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;

public class NewsPanel extends JPanel {

    private final String[] statisticsLabels = {"Views", "Likes", "Comments"};
    public NewsPanel(List<News> newsList) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        for (News news : newsList) {
            add(createNewsPanel(news));
            add(Box.createVerticalStrut(10)); // Add some vertical space between news items
        }

        // Add a scrollbar to the panel
        JScrollPane scrollPane = new JScrollPane(this);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Create a JFrame and add the JScrollPane
        JFrame frame = new JFrame("News Panel Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.getContentPane().add(scrollPane);
        frame.setVisible(true);
    }

    private JPanel createNewsPanel(News news) {
        JPanel newsPanel = new JPanel();
        newsPanel.setLayout(new BoxLayout(newsPanel, BoxLayout.Y_AXIS));

        // Create a separate panel for the upper part of the news
        JPanel upperPanel = new JPanel();
        upperPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); // Set the layout to FlowLayout.LEFT
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
        dateLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
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
        int[] statisticsValues = {news.getViews(), news.getLikes(), news.getComments()};

        // Create labels in a loop
        for (int i = 0; i < statisticsLabels.length; i++) {
            ImageIcon communityIcon = new ImageIcon(ImageUtils.getLocalImage("assets/ui/icons/vk/"+statisticsLabels[i] +".png"));
            JLabel label = new JLabel(String.valueOf(statisticsValues[i]));
            label.setIcon(communityIcon);
            statisticsPanel.add(label);
        }

        newsPanel.add(statisticsPanel);

        return newsPanel;
    }

    private Image getRoundedImage(Image image, int width, int height) {
        BufferedImage roundedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = roundedImage.createGraphics();
        g2.setClip(new Ellipse2D.Float(0, 0, width, height));
        g2.drawImage(image, 0, 0, width, height, null);
        g2.dispose();
        return roundedImage;
    }

    private String formatDate(long unixTimestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(unixTimestamp * 1000L); // Convert seconds to milliseconds
    }

    public static void main(String[] args) {
        // Assuming you already have a NewsProvider instance
        NewsProvider newsProvider = new NewsProvider();
        List<News> newsList = newsProvider.fetchNews();

        // Create a NewsPanel instance
        new NewsPanel(newsList);
    }
}
