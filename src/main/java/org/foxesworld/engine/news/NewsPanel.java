package org.foxesworld.engine.news;

import org.foxesworld.engine.Engine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;
import static org.foxesworld.engine.utils.ImageUtils.*;

public class NewsPanel extends JPanel {
    /*
    * TODO
    *  That's a sample and is hardcoded
    *  EXPERIMENTAL
    * */
    private JScrollPane scrollPane;
    private JPanel contentPanel;
    public NewsPanel(List<News> newsList) {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(true);
        contentPanel.setBackground(hexToColor("#1e1f2073"));

        for (News news : newsList) {
            contentPanel.add(createNewsPanel(news));
            contentPanel.add(Box.createVerticalStrut(10));
        }

        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        // Applying custom ScrollBarStyle
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        adjustScrollPaneSensitivity(scrollPane);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(scrollPane, BorderLayout.CENTER);
        setOpaque(false);
    }

    private JPanel createNewsPanel(News news) {
        JPanel newsPanel = new JPanel();
        JPanel newsInner = new JPanel();
        newsInner.setOpaque(false);
        newsInner.setBorder(new EmptyBorder(10, 10, 10, 10));
        newsInner.setLayout(new BoxLayout(newsInner, BoxLayout.Y_AXIS));
        newsPanel.setLayout(new BoxLayout(newsPanel, BoxLayout.Y_AXIS));
        newsPanel.setOpaque(false);

        // Creating a separate panel for the upper part of the news
        JPanel upperPanel = new JPanel();
        upperPanel.setOpaque(true);
        upperPanel.setBackground(hexToColor("#3366938a"));

        try {
            // Display the community photo with rounded corners
            ImageIcon communityIcon = new ImageIcon(new URL(news.getCommunityPhotoUrl()));
            Image communityImage = communityIcon.getImage();
            communityIcon = new ImageIcon(getRoundedImage(communityImage, 1.2, 20));

            JLabel communityLabel = new JLabel(communityIcon);
            communityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            upperPanel.add(communityLabel);

            // Display the community name
            JLabel communityNameLabel = new JLabel(news.getCommunityName());
            communityNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            communityNameLabel.setForeground(Color.WHITE);
            upperPanel.add(communityNameLabel);

            // Add the publication date to the upper panel
            JLabel dateLabel = new JLabel("<html><body style='width: 240px; text-align: right; padding: 0px;'>" + formatDate(news.getPublicationDate()) + "</body></html>");
            //dateLabel.setIcon(new ImageIcon(getLocalImage("assets/ui/icons/vk/time.png")));
            dateLabel.setForeground(Color.WHITE);
            upperPanel.add(dateLabel);

            // Add the upper panel to the main news panel
            newsPanel.add(upperPanel);

            // Create a panel for the news text
            JPanel textPanel = new JPanel(new BorderLayout());
            textPanel.setOpaque(false);

            // Display the news text as a title
            String labelText = "<html><body style='width: 370px; text-align: left; padding: 0px; margin-left: 5px; margin-right: 5px;'>" + news.getText() + "</body></html>";
            JLabel newsText = new JLabel(labelText);
            newsText.setFont(new Font("Arial", Font.BOLD, 11));
            newsText.setBorder(new EmptyBorder(10, 10, 10, 10));
            newsText.setForeground(Color.WHITE);

            // Place the text on the left side of the text panel
            textPanel.add(newsText, BorderLayout.WEST);
            newsPanel.add(textPanel);

            // Display photos without resizing or in full size if there's only one photo
            if (news.getTooltipPhotoUrls().size() == 1) {
                try {
                    ImageIcon imageIcon = new ImageIcon(new URL(news.getOriginalPhotoUrls().get(0)));
                    Image image = getRoundedImage(imageIcon.getImage(), 2.2, 15);
                    ImageIcon fullSizeIcon = new ImageIcon(image);
                    JLabel photoLabel = new JLabel(fullSizeIcon);
                    photoLabel.setAlignmentX(CENTER_ALIGNMENT);
                    newsInner.add(photoLabel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                for (String photoUrl : news.getTooltipPhotoUrls()) {
                    try {
                        ImageIcon imageIcon = new ImageIcon(new URL(photoUrl));
                        JLabel photoLabel = new JLabel(imageIcon);
                        newsInner.add(photoLabel);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Add the photo panel to the main news panel
            newsPanel.add(newsInner);

            // Display statistics (views, likes, comments) at the bottom-left
            JPanel statisticsPanel = new JPanel();
            statisticsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            statisticsPanel.setBackground(hexToColor("#df8b08bf"));
            statisticsPanel.setOpaque(true);

            // Define the statistics labels and their values
            int[] statisticsValues = {news.getViews(), news.getLikes(), news.getComments(), news.getReposts()};

            // Creating labels in a loop
            for (int i = 0; i < NewsProvider.getStatsValuesKeys().length; i++) {
                ImageIcon imageIcon = new ImageIcon(getLocalImage("assets/ui/icons/vk/" + NewsProvider.getStatsValuesKeys()[i] + ".png"));
                JLabel label = new JLabel(String.valueOf(statisticsValues[i]));
                label.setIcon(imageIcon);
                label.setForeground(Color.WHITE);
                statisticsPanel.add(label);
            }

            newsPanel.add(statisticsPanel);
            newsPanel.add(Box.createVerticalStrut(10));

            // Set the preferred size of newsInner based on its content
            newsInner.setPreferredSize(newsInner.getPreferredSize());

        } catch (IOException e) {
            Engine.LOGGER.error("Error loading community photo: " + e.getMessage());
        }

        return newsPanel;
    }

    private void adjustScrollPaneSensitivity(JScrollPane scrollPane) {
        scrollPane.addMouseWheelListener(e -> {
            Adjustable adj = scrollPane.getVerticalScrollBar();
            int scrollAmount = e.getUnitsToScroll() * adj.getBlockIncrement();
            adj.setValue(adj.getValue() + scrollAmount);
        });
    }

    private String formatDate(long unixTimestamp) {
        Timestamp stamp = new Timestamp(unixTimestamp * 1000L);
        Date date = new Date(stamp.getTime());
        SimpleDateFormat formatDate = new SimpleDateFormat("dd MMMM yyyy HH:mm");
        return formatDate.format(date);
    }
}
