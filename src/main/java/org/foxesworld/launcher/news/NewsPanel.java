package org.foxesworld.launcher.news;

import com.vdurmont.emoji.EmojiParser;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.scrollBar.ScrollBarUI;
import org.foxesworld.engine.gui.components.sprite.SpriteAnimation;
import org.foxesworld.engine.utils.FontUtils;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.IconUtils;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.launcher.news.provider.NewsAttributes;
import org.foxesworld.launcher.news.provider.NewsProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class NewsPanel extends JPanel implements NewsProvider.NewsFetchCallback {
    private static final Pattern EMOJI_ALIAS_PATTERN = Pattern.compile(":[a-zA-Z0-9_+\\-]+:");
    private final JFrame resizeFrame = new JFrame();
    private final News news;
    private final ImageUtils imageUtils;
    private final IconUtils iconUtils;
    private final FontUtils fontUtils;
    private final Map<String, String> emojiUrlCache = new ConcurrentHashMap<>();
    private final NewsComponents newsComponents;

    public NewsPanel(News news) {
        this.news = news;
        this.fontUtils = news.getLauncher().getFONTUTILS();
        this.imageUtils = news.getLauncher().getImageUtils();
        this.iconUtils = news.getLauncher().getIconUtils();
        this.newsComponents = new NewsComponents(this.news.getLauncher().getGuiBuilder(), "newsForm", List.of(SpriteAnimation.class));
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        resizeFrame.setLocationRelativeTo(this.news.getLauncher());
        news.getNewsProvider().fetchNews(this);

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

        newsPanel.add(createUpperPanel(newsAttributes));
        newsPanel.add(createTextPanel(newsAttributes));

        if (newsAttributes.getTooltipPhotoUrls().size() == 1) {
            addSinglePhoto(newsPanel, newsAttributes);
        } else {
            addMultiplePhotos(newsPanel, newsAttributes);
        }

        newsPanel.add(createStatisticsPanel(newsAttributes));

        return newsPanel;
    }

    private JPanel createUpperPanel(NewsAttributes newsAttributes) {
        // Load the background image
        ImageIcon backgroundImageIcon = new ImageIcon(this.imageUtils.getLocalImage("assets/ui/img/newsPanel/box-top.png"));
        BackgroundPanel upperPanel = new BackgroundPanel(backgroundImageIcon.getImage());
        upperPanel.setOpaque(false); // Make sure it's not opaque

        try {
            JLabel communityLabel = createCommunityLabel(newsAttributes);
            upperPanel.add(communityLabel);
            upperPanel.add(Box.createHorizontalGlue());
            upperPanel.setBorder(new EmptyBorder(30, 10, 0, 0));

            // Create and add the date label
            JLabel dateLabel = new JLabel(formatDate(newsAttributes.getPublicationDate()));
            dateLabel.setForeground(Color.black);
            dateLabel.setFont(this.fontUtils.getFont("mcfont", 13));
            dateLabel.setBorder(new EmptyBorder(30, 0, 0, 40));
            upperPanel.add(dateLabel);
        } catch (IOException e) {
            Engine.LOGGER.error("Error loading community photo: " + e.getMessage());
        }

        return upperPanel;
    }



    private JLabel createCommunityLabel(NewsAttributes newsAttributes) throws IOException {
        BufferedImage communityImg = this.imageUtils.getCachedUrlImg(newsAttributes.getCommunityPhotoUrl(), "vk", this.imageUtils.getLocalImage("assets/ui/icons/srvIcons/forge.png"));
        ImageIcon communityIcon = new ImageIcon(imageUtils.getRoundedImage(imageUtils.getScaledImage(communityImg, 52, 52), 50));

        JLabel communityLabel = new JLabel(communityIcon);
        communityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        communityLabel.setText(newsAttributes.getCommunityName());
        communityLabel.setForeground(hexToColor("#ac6343"));
        communityLabel.setBorder(new EmptyBorder(30, 10, 0, 0));
        communityLabel.setFont(this.fontUtils.getFont("mcfontBold", 13));
        return communityLabel;
    }

    private String getEmoticonUrl(String key) {
        return emojiUrlCache.computeIfAbsent(key, k -> {
            HTTPrequest httPrequest = this.news.getLauncher().getPOSTrequest();
            Map<String, Object> emoData = Map.of("sysRequest", "getEmoticon", "emoKey", k);

            CompletableFuture<String> future = new CompletableFuture<>();

            httPrequest.sendAsync(emoData,
                    response -> future.complete(String.valueOf(response)),
                    error -> {
                        future.completeExceptionally(error);
                        Engine.LOGGER.error("Failed to fetch emoticon URL: " + error.getMessage());
                    }
            );

            try {
                return future.get(); // Blocking call to get the result of the async operation
            } catch (InterruptedException | ExecutionException e) {
                Engine.LOGGER.error("Error completing emoticon URL fetch task: " + e.getMessage());
                return null; // Return null or a default value in case of an error
            }
        });
    }

    private JPanel createTextPanel(NewsAttributes newsAttributes) {
        // Load the background image
        ImageIcon backgroundImageIcon = new ImageIcon(this.imageUtils.getLocalImage("assets/ui/img/newsPanel/box-body.png")); // Replace with your image path
        BackgroundPanel textPanel = new BackgroundPanel(backgroundImageIcon.getImage());
        textPanel.setOpaque(false); // Make sure it's not opaque

        String originalText = EmojiParser.parseToAliases(newsAttributes.getText());
        String[] lines = originalText.split("\n");
        StringBuilder processedText = new StringBuilder("<html><body style='width: 365px; text-align: left; margin-left: 5px; margin-right: 5px;'>");

        for (String line : lines) {
            Matcher matcher = EMOJI_ALIAS_PATTERN.matcher(line);
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                String alias = matcher.group();
                String imageUrl = this.getEmoticonUrl(alias);
                String imgTag = "<img src=\"" + this.news.getLauncher().getEngineData().getBindUrl() + imageUrl + "\" width=\"32\" height=\"32\" alt=\"" + alias + "\" />";
                matcher.appendReplacement(result, imgTag);
            }
            matcher.appendTail(result);

            if (line.contains("#")) {
                processedText.append("<span style='color: #2e95d3'>").append(result).append("</span><br>");
            } else {
                processedText.append(result).append("<br>");
            }
        }

        processedText.append("</body></html>");
        JLabel newsText = new JLabel(processedText.toString());
        newsText.setFont(this.fontUtils.getFont("mcfont", 13));
        newsText.setForeground(Color.black);
        newsText.setBorder(new EmptyBorder(5, 25, 5, 0));

        textPanel.add(newsText, BorderLayout.CENTER);

        return textPanel;
    }


    private void addSinglePhoto(JPanel newsPanel, NewsAttributes newsAttributes) {
        BufferedImage image = this.imageUtils.getRoundedImage(this.imageUtils.getCachedUrlImg(newsAttributes.getOriginalPhotoUrls().get(0), "vk", this.imageUtils.getLocalImage("")), 15);

        int maxWidth = 450;
        int maxHeight = 400;

        double aspectRatio = (double) image.getWidth() / image.getHeight();
        int scaledWidth = Math.min(maxWidth, (int) (maxHeight * aspectRatio));
        int scaledHeight = Math.min(maxHeight, (int) (maxWidth / aspectRatio));

        Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        JLabel photoLabel = new JLabel(scaledIcon);
        photoLabel.setPreferredSize(new Dimension(scaledWidth, scaledHeight));

        BackgroundPanel photoPanel = new BackgroundPanel(this.imageUtils.getLocalImage("assets/ui/img/newsPanel/box-body.png"));
        photoPanel.setLayout(new BoxLayout(photoPanel, BoxLayout.X_AXIS));
        photoPanel.setOpaque(false);
        photoPanel.add(Box.createHorizontalGlue());
        photoPanel.add(photoLabel);
        photoPanel.add(Box.createHorizontalGlue());

        photoPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        newsPanel.add(photoPanel);
        newsPanel.setBackground(new Color(0, 0, 0, 0));
    }


    private void addMultiplePhotos(JPanel newsPanel, NewsAttributes newsAttributes) {
        BackgroundPanel multiPhotoPanel = new BackgroundPanel(this.imageUtils.getLocalImage("assets/ui/img/newsPanel/box-body.png"));
        multiPhotoPanel.setLayout(new GridLayout(0, 3, 0, 1));
        multiPhotoPanel.setOpaque(false);

        List<String> photoUrls = newsAttributes.getOriginalPhotoUrls();
        List<String> tooltipPhotoUrls = newsAttributes.getTooltipPhotoUrls();

        for (int i = 0; i < photoUrls.size(); i++) {
            String photoUrl = photoUrls.get(i);
            BufferedImage tooltipImage = this.imageUtils.getCachedUrlImg(tooltipPhotoUrls.get(i), "vk/tooltips", this.imageUtils.getLocalImage(""));
            BufferedImage fullImage = this.imageUtils.getCachedUrlImg(photoUrl, "vk", this.imageUtils.getLocalImage(""));

            ImageIcon photoIcon = new ImageIcon(resizeImage(tooltipImage));
            JLabel photoLabel = new JLabel(photoIcon);
            photoLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            photoLabel.setBackground(new Color(0, 0, 0, 0));
            photoLabel.setOpaque(false);

            photoLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    openFullPhoto(fullImage);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    photoLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    photoLabel.setBorder(null);
                }
            });

            multiPhotoPanel.add(photoLabel);
        }

        multiPhotoPanel.setBorder(new EmptyBorder(0, 10, 10, 0));
        multiPhotoPanel.setBackground(new Color(0, 0, 0, 0));
        newsPanel.add(multiPhotoPanel);
    }



    private BufferedImage resizeImage(BufferedImage originalImage) {
        int targetWidth = originalImage.getWidth();
        int targetHeight = originalImage.getHeight();
        BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = scaledImage.createGraphics();
        graphics.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return scaledImage;
    }

    private void openFullPhoto(BufferedImage photo) {
        resizeFrame.getContentPane().removeAll();
        resizeFrame.revalidate();
        resizeFrame.repaint();

        resizeFrame.setLayout(new BorderLayout());
        resizeFrame.setResizable(false);

        ImageIcon imageIcon = new ImageIcon(photo);
        JLabel fullPhotoLabel = new JLabel(imageIcon);
        resizeFrame.add(fullPhotoLabel, BorderLayout.CENTER);

        resizeFrame.setSize(photo.getWidth() + 40, photo.getHeight() + photo.getHeight() / 4);
        resizeFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        resizeFrame.setVisible(true);
    }

    private JPanel createStatisticsPanel(NewsAttributes newsAttributes) {
        ImageIcon backgroundImageIcon = new ImageIcon(this.imageUtils.getLocalImage("assets/ui/img/newsPanel/box-bottom.png"));
        BackgroundPanel statisticsPanel = new BackgroundPanel(backgroundImageIcon.getImage());
        statisticsPanel.setOpaque(false);
        statisticsPanel.setLayout(new BoxLayout(statisticsPanel, BoxLayout.X_AXIS));

        // Define statistics values
        int[] statisticsValues = {
                newsAttributes.getViews(),
                newsAttributes.getLikes(),
                newsAttributes.getComments(),
                newsAttributes.getReposts()
        };

        // Define corresponding icons for each stat
        String[] statIcons = NewsProvider.getStatsValuesKeys();

        // Create a panel for each statistic
        for (int i = 0; i < statIcons.length; i++) {
            ImageIcon icon = this.iconUtils.getVectorIcon("assets/ui/icons/vk/" + statIcons[i] + ".svg", 16, 16);
            Color textColor = hexToColor("#626d7a");

            // Create label panel for the statistic
            JPanel statLabelPanel = createStatsLabel(String.valueOf(statisticsValues[i]), icon, textColor, FlowLayout.LEFT);
            statisticsPanel.add(statLabelPanel);
            //statisticsPanel.add(Box.createHorizontalStrut(15)); // Add space between stats
        }

        statisticsPanel.setBorder(new EmptyBorder(0, 40, 80, 0)); // Adjust the padding
        return statisticsPanel;
    }
    public JPanel createStatsLabel(String text, ImageIcon icon, Color textColor, int horizontalAlignment) {
        JPanel labelPanel = new JPanel(new FlowLayout(horizontalAlignment, 0, 0));
        labelPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(icon);
        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(textColor);
        textLabel.setFont(fontUtils.getFont("mcfontBold", 14));
        labelPanel.setLayout(new FlowLayout() {
            @Override
            public void layoutContainer(Container parent) {
                Component[] components = parent.getComponents();
                int iconWidth = components[0].getPreferredSize().width;
                int textWidth = components[1].getPreferredSize().width;
                components[0].setBounds(0, 0, iconWidth, parent.getHeight());
                components[1].setBounds(iconWidth + 5, 3, textWidth, parent.getHeight());
            }
        });

        labelPanel.add(iconLabel);
        labelPanel.add(textLabel);

        return labelPanel;
    }

    private String formatDate(long timestamp) {
        Timestamp stamp = new Timestamp(timestamp * 1000L);
        Date date = new Date(stamp.getTime());
        String[] locale = this.news.getLauncher().getLANG().getLocalesSet()[this.news.getLauncher().getLANG().getLocaleIndex()].split("_");
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy HH:mm", new Locale(locale[0], locale[1]));
        return formatter.format(date);
    }


    private void adjustScrollPaneSensitivity(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    }

    @Override
    public void onNewsFetched(List<NewsAttributes> newsAttributesList) {
        JScrollPane scrollPane = createScrollPane();
        JPanel contentPanel = createContentPanel(newsAttributesList);
        scrollPane.setViewportView(contentPanel);
        add(scrollPane);
        this.newsComponents.turnOffLoader();
        repaint();
        revalidate();
    }
}