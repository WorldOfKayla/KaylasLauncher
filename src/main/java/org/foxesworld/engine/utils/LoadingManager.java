package org.foxesworld.engine.utils;

import org.foxesworld.engine.Engine;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class LoadingManager {

    private SpriteAnimation spriteAnimation;
    private final Engine engine;
    private String loadingText = "";
    private String loadingTitle = "";
    private JFrame loadingFrame;
    private Timer loadingTimer;
    private int dotCount = 0;
    private int dotLimit = 3;
    JLabel loaderText;
    JLabel titleLabel;

    public LoadingManager(Engine engine) {
        this.engine = engine;
        this.spriteAnimation = new SpriteAnimation("assets/ui/sprites/wait.png", 11, 40);
        initializeLoadingFrame();
    }

    private void initializeLoadingFrame() {
        loadingFrame = new JFrame("Loading");
        loadingFrame.setUndecorated(true);
        loadingFrame.setSize(400, 150);

        JPanel contentPane = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        spriteAnimation.setPreferredSize(new Dimension(32, 32));
        contentPane.add(spriteAnimation, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(0, 20, 0, 0);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);

        titleLabel = new JLabel(loadingTitle);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        textPanel.add(titleLabel);

        loaderText = new JLabel(loadingText);
        loaderText.setFont(new Font("Arial", Font.BOLD, 11));
        textPanel.add(loaderText);
        contentPane.add(textPanel, gbc);
        loadingFrame.setContentPane(contentPane);

        loadingTimer = new Timer(500, e -> {
            dotCount++;
            if (dotCount > dotLimit) {
                dotCount = 0;
            }
            loaderText.setText(loadingText + ".".repeat(dotCount));
        });
    }

    public void startLoading() {
        loadingTimer.start();
        loadingFrame.setLocationRelativeTo(null);
        loadingFrame.setVisible(true);
    }

    public void setLoadingText(String loadingText, String loadingTitle) {
        this.loadingText = loadingText;
        this.loadingTitle = loadingTitle;
        spriteAnimation.setLoadingText(loadingText);
        loaderText.setText(loadingText);
        titleLabel.setText(loadingTitle);
        loadingFrame.repaint();
    }

    public void setSpriteVisible(boolean visible) {
        spriteAnimation.setVisible(visible);
        spriteAnimation.repaint();
    }

    public void stopLoading() {
        loadingTimer.stop();
        loadingFrame.setVisible(false);
    }

    public static class SpriteAnimation extends JComponent {
        private BufferedImage spriteSheet;
        private int totalFrames;
        private int animationDelay;
        private int currentFrame = 0;
        private String loadingText = "";

        public SpriteAnimation(String path, int frames, int delay) {
            this.spriteSheet = ImageUtils.getLocalImage(path);
            this.totalFrames = frames;
            this.animationDelay = delay;

            Timer timer = new Timer(animationDelay, e -> {
                currentFrame = (currentFrame + 1) % totalFrames;
                repaint();
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int scaledWidth = getWidth();
            int scaledHeight = getHeight();

            // Draw the scaled image
            g.drawImage(
                    ImageUtils.getByIndex(spriteSheet, 128, currentFrame),
                    0,
                    0,
                    scaledWidth,
                    scaledHeight,
                    this
            );

            g.dispose();
        }

        public void setLoadingText(String loadingText) {
            this.loadingText = loadingText;
            repaint();
        }
    }
}
