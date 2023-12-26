package org.foxesworld.engine.news;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.scrollBar.ScrollBarUI;
import org.foxesworld.engine.news.provider.NewsAttributes;
import org.foxesworld.engine.news.provider.NewsProvider;
import org.foxesworld.engine.utils.ImageUtils;

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
import static org.foxesworld.engine.utils.ImageUtils.getLocalImage;
import static org.foxesworld.engine.utils.ImageUtils.getRoundedImage;

public class NewsPanel extends JPanel {
    /*
    * TODO
    *  That's a sample and is hardcoded
    *  EXPERIMENTAL
    * */
    private final JScrollPane scrollPane;
    private final JPanel contentPanel;
    public NewsPanel(List<NewsAttributes> newsAttributesList) {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(true);
        contentPanel.setBackground(hexToColor("#1e1f2073"));

        for (NewsAttributes newsAttributes : newsAttributesList) {
            contentPanel.add(createNewsPanel(newsAttributes));
            contentPanel.add(Box.createVerticalStrut(10));
        }

        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        // Applying custom ScrollBarStyle
        scrollPane.getVerticalScrollBar().setUI(new ScrollBarUI());
        adjustScrollPaneSensitivity(scrollPane);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(scrollPane, BorderLayout.CENTER);
        setOpaque(false);
    }

    private JPanel createStatsLabel(String text, ImageIcon icon, Color textColor, int horizontalAlignment) {
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new FlowLayout(horizontalAlignment));
        labelPanel.setOpaque(false);

        JLabel label = new JLabel(text);
        label.setIcon(icon);
        label.setForeground(textColor);

        labelPanel.add(label);

        return labelPanel;
    }

    private JPanel createNewsPanel(NewsAttributes newsAttributes) {
        JPanel newsPanel = new JPanel();
        newsPanel.setLayout(new BoxLayout(newsPanel, BoxLayout.Y_AXIS));
        newsPanel.setOpaque(false);

        // Creating a separate panel for the upper part of the newsAttributes
        JPanel upperPanel = new JPanel();
        upperPanel.setOpaque(true);
        upperPanel.setBackground(hexToColor("#3366938a")); //assets/ui/img/title.png

        try {
            // Display the community photo with rounded corners
            ImageIcon communityIcon = new ImageIcon(new URL(newsAttributes.getCommunityPhotoUrl()));
            Image communityImage = communityIcon.getImage();
            communityIcon = new ImageIcon(getRoundedImage(communityImage, 50));

            JLabel communityLabel = new JLabel(communityIcon);
            communityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            upperPanel.add(communityLabel);

            // Display the community name
            JLabel communityNameLabel = new JLabel(newsAttributes.getCommunityName());
            communityNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            communityNameLabel.setForeground(Color.WHITE);
            upperPanel.add(communityNameLabel);

            // Add the publication date to the upper panel
            JLabel dateLabel = new JLabel("<html><body style='width: 240px; text-align: right; padding: 0px;'>" + formatDate(newsAttributes.getPublicationDate()) + "</body></html>");
            dateLabel.setForeground(Color.WHITE);
            upperPanel.add(dateLabel);

            // Add the upper panel to the main newsAttributes panel
            newsPanel.add(upperPanel);

            // Create a panel for the newsAttributes text
            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BorderLayout());

            // Display the newsAttributes text as a title
            String labelText = "<html><body style='width: 380px; text-align: left; padding: 0px; margin-left: 5px; margin-right: 5px;'>" + newsAttributes.getText() + "</body></html>";
            JLabel newsText = new JLabel(labelText);
            newsText.setFont(new Font("Arial", Font.BOLD, 11));
            newsText.setBorder(new EmptyBorder(5, 0, 5, 0));
            newsText.setForeground(Color.WHITE);

            // Place the text on the left side of the text panel
            textPanel.add(newsText, BorderLayout.WEST);
            newsPanel.add(textPanel);

            // Display photos without resizing or in full size if there's only one photo
            if (newsAttributes.getTooltipPhotoUrls().size() == 1) {
                try {
                    ImageIcon imageIcon = new ImageIcon(new URL(newsAttributes.getOriginalPhotoUrls().get(0)));
                    Image image = ImageUtils.getScaledImage(getRoundedImage(imageIcon.getImage(), 15), 465, 350);
                    ImageIcon fullSizeIcon = new ImageIcon(image);
                    JLabel photoLabel = new JLabel(fullSizeIcon);
                    photoLabel.setAlignmentX(CENTER_ALIGNMENT);
                    photoLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
                    newsPanel.add(photoLabel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                for (String photoUrl : newsAttributes.getTooltipPhotoUrls()) {
                    try {
                        ImageIcon imageIcon = new ImageIcon(new URL(photoUrl));
                        JLabel photoLabel = new JLabel(imageIcon);
                        newsPanel.add(photoLabel);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Display statistics (views, likes, comments) at the bottom-left
            JPanel statisticsPanel = new JPanel();
            statisticsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            statisticsPanel.setBackground(hexToColor("#df8b08bf"));
            statisticsPanel.setOpaque(true);

            // Define the statistics labels and their values
            int[] statisticsValues = {newsAttributes.getViews(), newsAttributes.getLikes(), newsAttributes.getComments(), newsAttributes.getReposts()};

            // Creating labels in a loop
            for (int i = 0; i < NewsProvider.getStatsValuesKeys().length; i++) {
                ImageIcon imageIcon = new ImageIcon(getLocalImage("assets/ui/icons/vk/" + NewsProvider.getStatsValuesKeys()[i] + ".png"));
                Color textColor = Color.WHITE;

                // Adjusting alignment for the last label (views)
                int horizontalAlignment = (i == NewsProvider.getStatsValuesKeys().length - 1) ? FlowLayout.RIGHT : FlowLayout.LEFT;

                JPanel labelPanel = createStatsLabel(String.valueOf(statisticsValues[i]), imageIcon, textColor, horizontalAlignment);
                statisticsPanel.add(labelPanel);
            }

            newsPanel.add(statisticsPanel);
            newsPanel.add(Box.createVerticalStrut(10));

            // Set the preferred size of newsInner based on its content
            // Note: If you were using newsInner for any specific purpose, you might need to adapt this part accordingly.

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
