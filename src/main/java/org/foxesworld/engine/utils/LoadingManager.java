package org.foxesworld.engine.utils;

import org.foxesworld.engine.Engine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class LoadingManager {
    /* TODO
    *   Rewrite this temporary class
    **/

    private final SpriteAnimation spriteAnimation;
    private final Rectangle spriteRect = new Rectangle(20, 45, 32, 32);
    private final Engine engine;
    private String loadingText = "loading.msg";
    private String loadingTitle = "loading.title";
    private final JFrame loadingFrame;
    private final Timer loadingTimer;
    private int dotCount = 0;
    private final int dotLimit = 3;
    private JLabel loaderText;
    private JLabel titleLabel;

    public LoadingManager(Engine engine) {
        this.engine = engine;
        this.spriteAnimation = new SpriteAnimation("assets/ui/sprites/spinner.png", 50, 60, spriteRect);
        this.loadingFrame = new JFrame("Loading");
        this.loadingTimer = new Timer(500, e -> {
            dotCount = (dotCount + 1) % dotLimit;
            loaderText.setText(loadingText);
        });

        initializeLoadingFrame();
    }

    private void initializeLoadingFrame() {
        loadingFrame.setUndecorated(true);
        loadingFrame.setSize(500, 150);

        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ImageIcon backgroundIcon = new ImageIcon(ImageUtils.getScaledImage(ImageUtils.getLocalImage("assets/ui/img/bg/season/spring.png"), getWidth(), getHeight()));
                g.drawImage(backgroundIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
                g.setColor(new Color(215, 205, 205, 165));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(null);

        loadingFrame.setContentPane(backgroundPanel);

        backgroundPanel.setBounds(0, 0, loadingFrame.getWidth(), loadingFrame.getHeight());

        spriteAnimation.setBounds(spriteRect);
        backgroundPanel.add(spriteAnimation);

        titleLabel = createLabel(loadingTitle, 16, new Rectangle(90, 40, 200, 20), backgroundPanel);
        loaderText = createLabel(loadingText, 11, new Rectangle(90, 55, 400, 20), backgroundPanel);

        loadingFrame.setAlwaysOnTop(true);

        engine.getFrame().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                updateLoadingFramePosition();
            }
        });

        int cornerRadius = 20;
        loadingFrame.setShape(new RoundRectangle2D.Double(0, 0, loadingFrame.getWidth(), loadingFrame.getHeight(), cornerRadius, cornerRadius));
    }

    private JLabel createLabel(String text, int fontSize, Rectangle bounds, JPanel panel) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, fontSize));
        label.setBounds(bounds);
        panel.add(label);
        return label;
    }

    private void updateLoadingFramePosition() {
        Point mainFrameCenter = getCenterPoint(engine.getFrame());
        int offsetX = mainFrameCenter.x - loadingFrame.getWidth() / 2;
        int offsetY = mainFrameCenter.y - loadingFrame.getHeight() / 2;
        loadingFrame.setLocation(offsetX, offsetY);
    }

    private Point getCenterPoint(JFrame frame) {
        int centerX = frame.getX() + frame.getWidth() / 2;
        int centerY = frame.getY() + frame.getHeight() / 2;
        return new Point(centerX, centerY);
    }

    public void startLoading() {
        loadingTimer.start();
        updateLoadingFramePosition();
        loadingFrame.setVisible(true);
    }

    public void setLoadingText(String loadingText, String loadingTitle) {
        this.loadingText = loadingText;
        this.loadingTitle = engine.getLANG().getString(loadingTitle);
        loaderText.setText(this.loadingText);
        titleLabel.setText(this.loadingTitle);
        loadingFrame.repaint();
    }

    public void stopLoading() {
        loadingTimer.stop();
        loadingFrame.setVisible(false);
    }

    public Timer getLoadingTimer() {
        return loadingTimer;
    }

    public static class SpriteAnimation extends JComponent {
        private final BufferedImage spriteSheet;
        private final int totalFrames;
        private final int animationDelay;
        private int currentFrame = 0;

        public SpriteAnimation(String path, int frames, int delay, Rectangle spriteRect) {
            this.spriteSheet = ImageUtils.getLocalImage(path);
            this.totalFrames = frames;
            this.animationDelay = delay;
            this.setBounds(spriteRect);

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

            g.drawImage(
                    ImageUtils.getByIndex(spriteSheet, 200, currentFrame),
                    0,
                    0,
                    scaledWidth,
                    scaledHeight,
                    this
            );

            g.dispose();
        }
    }
}
