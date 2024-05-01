package org.foxesworld.launcher.news;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.scrollBar.ScrollBarUI;
import org.foxesworld.engine.utils.FontUtils;
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
import static org.foxesworld.engine.utils.ImageUtils.getLocalImage;
import static org.foxesworld.engine.utils.ImageUtils.getRoundedImage;

public class NewsPanel extends JPanel {
    private final News news;
    private final FontUtils fontUtils;

    public NewsPanel(News news) {
        this.news = news;
        this.fontUtils = news.getLauncher().getFONTUTILS();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);

        JScrollPane scrollPane = createScrollPane();
        JPanel contentPanel = createContentPanel(news.getNewsProvider().fetchNews());
        scrollPane.setViewportView(contentPanel);

        add(scrollPane);
    }

    private JScrollPane createScrollPane() {
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new ScrollBarUI());
        adjustScrollPaneSensitivity(scrollPane);
        return scrollPane;
    }

    private JPanel createContentPanel(List<NewsAttributes> newsAttributesList) {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        for (NewsAttributes newsAttributes : newsAttributesList) {
            contentPanel.add(createNewsPanel(newsAttributes));
            //contentPanel.add(Box.createVerticalStrut(10));
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

        try {
            ImageIcon communityIcon = new ImageIcon(new URL(newsAttributes.getCommunityPhotoUrl()));
            Image communityImage = communityIcon.getImage();
            communityIcon = new ImageIcon(getRoundedImage(communityImage, 50));

            JLabel communityLabel = new JLabel(communityIcon);
            communityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            upperPanel.add(communityLabel);

            JLabel communityNameLabel = new JLabel(newsAttributes.getCommunityName());
            communityNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            communityNameLabel.setForeground(Color.WHITE);
            upperPanel.add(communityNameLabel);

            JLabel dateLabel = new JLabel("<html><body style='width: 230px; text-align: right; padding: 0px;'>" + formatDate(newsAttributes.getPublicationDate()) + "</body></html>");
            dateLabel.setForeground(Color.WHITE);
            upperPanel.add(dateLabel);
        } catch (IOException e) {
            Engine.LOGGER.error("Error loading community photo: " + e.getMessage());
        }

        return upperPanel;
    }

    private JPanel createTextPanel(NewsAttributes newsAttributes) {
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BorderLayout());

        String labelText = "<html><body style='width: 380px; text-align: left; padding: 0px; margin-left: 5px; margin-right: 5px;'>" + newsAttributes.getText() + "</body></html>";
        JLabel newsText = new JLabel(labelText);
        newsText.setFont(this.fontUtils.getFont("mcfont", 13));
        newsText.setBorder(new EmptyBorder(5, 0, 5, 0));
        newsText.setForeground(Color.WHITE);

        textPanel.add(newsText, BorderLayout.WEST);

        return textPanel;
    }

    private void addSinglePhoto(JPanel newsPanel, NewsAttributes newsAttributes) {
        BufferedImage img = (BufferedImage) ImageUtils.getRoundedImage(ImageUtils.getCachedUrlImg(newsAttributes.getOriginalPhotoUrls().get(0), "vk", ImageUtils.getLocalImage("")), 25);
        Image image = ImageUtils.getScaledImage(img, 470, 350);
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
        statisticsPanel.setBackground(hexToColor("#df8b08bf"));
        statisticsPanel.setOpaque(true);

        int[] statisticsValues = {newsAttributes.getViews(), newsAttributes.getLikes(), newsAttributes.getComments(), newsAttributes.getReposts()};

        for (int i = 0; i < NewsProvider.getStatsValuesKeys().length; i++) {
            ImageIcon imageIcon = new ImageIcon(ImageUtils.getScaledImage(getLocalImage("assets/ui/icons/vk/" + NewsProvider.getStatsValuesKeys()[i] + ".png"), 16, 16));
            Color textColor = Color.WHITE;
            int horizontalAlignment = (i == NewsProvider.getStatsValuesKeys().length - 1) ? FlowLayout.RIGHT : FlowLayout.LEFT;

            JPanel labelPanel = createStatsLabel(String.valueOf(statisticsValues[i]), imageIcon, textColor, horizontalAlignment);
            statisticsPanel.add(labelPanel);
        }

        return statisticsPanel;
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