package org.foxesworld.launcher.news;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.scrollBar.ScrollBarUI;
import org.foxesworld.engine.utils.FontUtils;
import org.foxesworld.engine.utils.IconUtils;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.launcher.news.provider.NewsAttributes;
import org.foxesworld.launcher.news.provider.NewsProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class NewsPanel extends JPanel {
    private final News news;
    private final ImageUtils imageUtils;
    private final IconUtils iconUtils;
    private final FontUtils fontUtils;

    public NewsPanel(News news) {
        this.news = news;
        this.fontUtils = news.getLauncher().getFONTUTILS();
        this.imageUtils = news.getLauncher().getImageUtils();
        this.iconUtils = news.getLauncher().getIconUtils();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);

        if (this.news.getLauncher().getConfig().isLoadNews()) {
            JScrollPane scrollPane = createScrollPane();
            JPanel contentPanel = createContentPanel(news.getNewsProvider().fetchNews());
            scrollPane.setViewportView(contentPanel);
            add(scrollPane);
        }
    }

    private JScrollPane createScrollPane() {
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new ScrollBarUI(this.news.getLauncher()));
        adjustScrollPaneSensitivity(scrollPane);
        return scrollPane;
    }

    private JPanel createContentPanel(List<NewsAttributes> newsAttributesList) {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        for (NewsAttributes newsAttributes : newsAttributesList) {
            contentPanel.add(createNewsPanel(newsAttributes));
        }

        return contentPanel;
    }

    private JPanel createNewsPanel(NewsAttributes newsAttributes) {
        JPanel newsPanel = new JPanel();
        newsPanel.setLayout(new BoxLayout(newsPanel, BoxLayout.Y_AXIS));
        newsPanel.setOpaque(false);

        JPanel upperPanel = createUpperPanel(newsAttributes);
        newsPanel.add(upperPanel);

        JPanel textPanel = createTextPanel(newsAttributes);
        newsPanel.add(textPanel);

        if (newsAttributes.getTooltipPhotoUrls().size() == 1) {
            addSinglePhoto(newsPanel, newsAttributes);
        } else {
            addMultiplePhotos(newsPanel, newsAttributes);
        }

        JPanel statisticsPanel = createStatisticsPanel(newsAttributes);
        newsPanel.add(statisticsPanel);
        newsPanel.add(Box.createVerticalStrut(10));

        return newsPanel;
    }

    private JPanel createUpperPanel(NewsAttributes newsAttributes) {
        JPanel upperPanel = new JPanel();
        upperPanel.setOpaque(true);
        upperPanel.setBackground(hexToColor("#4a4c4f"));
        upperPanel.setLayout(new BoxLayout(upperPanel, BoxLayout.LINE_AXIS));

        try {
            ImageIcon communityIcon = new ImageIcon(new URL(newsAttributes.getCommunityPhotoUrl()));
            Image communityImage = communityIcon.getImage();
            communityIcon = new ImageIcon(imageUtils.getRoundedImage(communityImage, 50));

            JLabel communityLabel = new JLabel(communityIcon);
            communityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            communityLabel.setText(newsAttributes.getCommunityName());
            communityLabel.setForeground(Color.WHITE);
            communityLabel.setBorder(new EmptyBorder(5, 10, 0, 0));
            communityLabel.setFont(this.fontUtils.getFont("mcfontBold", 13));

            JPanel communityPanel = new JPanel();
            communityPanel.setOpaque(false);
            communityPanel.setLayout(new BoxLayout(communityPanel, BoxLayout.LINE_AXIS));
            communityPanel.add(communityLabel);
            communityPanel.add(Box.createHorizontalGlue()); // Fills the remaining space

            JLabel dateLabel = new JLabel(formatDate(newsAttributes.getPublicationDate()));
            dateLabel.setForeground(Color.WHITE);
            dateLabel.setFont(this.fontUtils.getFont("mcfont", 13));

            // Create a panel for the date label
            JPanel datePanel = new JPanel();
            datePanel.setOpaque(false);
            datePanel.setLayout(new BoxLayout(datePanel, BoxLayout.X_AXIS));
            datePanel.add(Box.createHorizontalGlue());
            datePanel.setBorder(new EmptyBorder(0, 0, 0, 50));
            datePanel.add(dateLabel);

            upperPanel.add(communityPanel);
            upperPanel.add(datePanel);
        } catch (IOException e) {
            Engine.LOGGER.error("Error loading community photo: " + e.getMessage());
        }

        return upperPanel;
    }


    private JPanel createTextPanel(NewsAttributes newsAttributes) {
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BorderLayout());

        String labelText = "<html><body style='width: 370px; text-align: left; margin-left: 5px; margin-right: 5px;'>" + newsAttributes.getText() + "</body></html>";
        JLabel newsText = new JLabel(labelText);
        newsText.setFont(this.fontUtils.getFont("mcfont", 13));
        //newsText.setBorder(new EmptyBorder(5, 0, 5, 0));
        newsText.setForeground(Color.WHITE);

        textPanel.add(newsText, BorderLayout.WEST);

        return textPanel;
    }

    private void addSinglePhoto(JPanel newsPanel, NewsAttributes newsAttributes) {
        BufferedImage img = imageUtils.getRoundedImage(imageUtils.getCachedUrlImg(newsAttributes.getOriginalPhotoUrls().get(0), "vk", imageUtils.getLocalImage("")), 25);
        Image image = imageUtils.getScaledImage(img, 470, 350);
        ImageIcon fullSizeIcon = new ImageIcon(image);
        JLabel photoLabel = new JLabel(fullSizeIcon);
        photoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        photoLabel.setBorder(new EmptyBorder(5, 5, 5, 50));
        newsPanel.add(photoLabel);
        newsPanel.setBackground(hexToColor("#0707079e"));
        newsPanel.setOpaque(true);
    }

    private void addMultiplePhotos(JPanel newsPanel, NewsAttributes newsAttributes) {
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

    private JPanel createStatisticsPanel(NewsAttributes newsAttributes) {
        JPanel statisticsPanel = new JPanel();
        statisticsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        statisticsPanel.setBackground(hexToColor("#9a9fa5"));
        statisticsPanel.setOpaque(true);
        int[] statisticsValues = {newsAttributes.getViews(), newsAttributes.getLikes(), newsAttributes.getComments(), newsAttributes.getReposts()};

        for (int i = 0; i < NewsProvider.getStatsValuesKeys().length; i++) {
            ImageIcon imageIcon = this.iconUtils.getVectorIcon("assets/ui/icons/vk/" + NewsProvider.getStatsValuesKeys()[i] + ".svg", 16, 16);
            Color textColor = Color.WHITE;
            int horizontalAlignment = (i == NewsProvider.getStatsValuesKeys().length - 1) ? FlowLayout.RIGHT : FlowLayout.LEFT;

            JPanel labelPanel = createStatsLabel(String.valueOf(statisticsValues[i]), imageIcon, textColor, 0);
            statisticsPanel.add(labelPanel);
        }

        return statisticsPanel;
    }

    public JPanel createStatsLabel(String text, ImageIcon icon, Color textColor, int horizontalAlignment) {
        JPanel labelPanel = new JPanel(new FlowLayout(horizontalAlignment, 0, 0));
        labelPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(icon); // Create a label for the icon
        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(textColor);
        textLabel.setFont(fontUtils.getFont("mcfontBold", 14));

        // Use an anonymous inner class to create a custom layout manager for the labels
        labelPanel.setLayout(new FlowLayout() {
            @Override
            public void layoutContainer(Container parent) {
                // Get the components
                Component[] components = parent.getComponents();

                // Calculate preferred sizes
                int iconWidth = components[0].getPreferredSize().width;
                int textWidth = components[1].getPreferredSize().width;

                // Set positions for components
                components[0].setBounds(0, 0, iconWidth, parent.getHeight());
                components[1].setBounds(iconWidth + 5, 3, textWidth, parent.getHeight());
            }
        });

        labelPanel.add(iconLabel);
        labelPanel.add(textLabel);

        return labelPanel;
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