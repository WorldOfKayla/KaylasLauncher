package org.foxesworld.engine.news;

import org.foxesworld.engine.utils.FontUtils;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;

public class NewsPanel extends JPanel {

    private String[] statisticsLabels = {"views", "likes", "comments"};
    private JScrollPane scrollPane;
    private JPanel contentPanel;

    public NewsPanel(List<News> newsList) {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(true);
        contentPanel.setBackground(FontUtils.hexToColor("#1e1f2073"));

        for (News news : newsList) {
            contentPanel.add(createNewsPanel(news));
            contentPanel.add(Box.createVerticalStrut(10)); // Add some vertical space between news items
        }


        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        adjustScrollPaneSensitivity(scrollPane);

        setLayout(new BoxLayout(this, 0));
        add(scrollPane, BorderLayout.CENTER);
        setOpaque(false);
    }

    private JPanel createNewsPanel(News news) {
        JPanel newsPanel = new JPanel();
        newsPanel.setLayout(new BoxLayout(newsPanel, BoxLayout.Y_AXIS));
        newsPanel.setOpaque(false);

        // Create a separate panel for the upper part of the news
        JPanel upperPanel = new JPanel();
        upperPanel.setOpaque(false);
        upperPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

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
            communityNameLabel.setForeground(Color.WHITE);
            upperPanel.add(communityNameLabel);

            // Add the publication date to the upper panel
            JLabel dateLabel = new JLabel(formatDate(news.getPublicationDate()));
            dateLabel.setIcon(new ImageIcon(ImageUtils.getLocalImage("assets/ui/icons/vk/time.png")));
            dateLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
            dateLabel.setForeground(Color.WHITE);
            upperPanel.add(dateLabel);

            // Add the upper panel to the main news panel
            newsPanel.add(upperPanel);

            // Display the news text as a title
            JLabel titleLabel = new JLabel(news.getText());
            titleLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Set the font and style as needed
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            newsPanel.add(titleLabel);

            // Display photos in full size
            for (String photoUrl : news.getTooltipPhotoUrls()) {
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
            statisticsPanel.setOpaque(false);

            // Define the statistics labels and their values
            int[] statisticsValues = {news.getViews(), news.getLikes(), news.getComments()};

            // Create labels in a loop
            for (int i = 0; i < statisticsLabels.length; i++) {
                ImageIcon imageIcon = new ImageIcon(ImageUtils.getLocalImage("assets/ui/icons/vk/" + statisticsLabels[i] + ".png"));
                JLabel label = new JLabel(String.valueOf(statisticsValues[i]));
                label.setIcon(imageIcon);
                label.setForeground(Color.WHITE);
                statisticsPanel.add(label);
            }

            newsPanel.add(statisticsPanel);

            // Add spacing before the VK post icon
            newsPanel.add(Box.createVerticalStrut(10)); // Adjust the vertical spacing as needed

            // Display the VK post icon to the left at the end of the news
            ImageIcon vkIcon = new ImageIcon(ImageUtils.getLocalImage("assets/ui/icons/vk/post.png"));
            JLabel vkLabel = new JLabel(vkIcon);
            vkLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            newsPanel.add(vkLabel);

        } catch (IOException e) {
            e.printStackTrace();
            // Debugging: Print error message if image loading fails
            System.err.println("Error loading community photo: " + e.getMessage());
        }

        return newsPanel;
    }

    private void adjustScrollPaneSensitivity(JScrollPane scrollPane) {
        scrollPane.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                Adjustable adj = scrollPane.getVerticalScrollBar();
                int scrollAmount = e.getUnitsToScroll() * adj.getBlockIncrement();
                adj.setValue(adj.getValue() + scrollAmount);
            }
        });
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
