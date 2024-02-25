package org.foxesworld.engine.utils;

import org.foxesworld.engine.Engine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class LoadingManager extends JFrame {

    private final SpriteAnimation spriteAnimation;
    private final Engine engine;
    private String loadingText = "loading.msg";
    private String loadingTitle = "loading.title";
    private final Timer loadingTimer;
    private final int dotLimit = 4;
    private JLabel loaderText, titleLabel;
    private final int animationSpeed = 5;
    private boolean isAnimating = false;

    public LoadingManager(Engine engine) {
        this.engine = engine;
        this.spriteAnimation = new SpriteAnimation("assets/ui/sprites/loader.png", 15, 90, 500, new Rectangle(30, 30, 64, 64));
        this.loadingTimer = new Timer(500, e -> {
            loaderText.setText(loadingText);
        });

        initializeLoadingFrame();
    }

    private void initializeLoadingFrame() {
        setUndecorated(true);
        setSize(500, 150);

        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ImageIcon backgroundIcon = new ImageIcon(ImageUtils.getScaledImage(ImageUtils.getLocalImage("assets/ui/img/bg/season/spring.png"), getWidth(), getHeight()));
                g.drawImage(backgroundIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
                g.setColor(new Color(127, 139, 149, 166));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(null);

        setContentPane(backgroundPanel);

        backgroundPanel.setBounds(0, 0, getWidth(), getHeight());

        spriteAnimation.setBounds(spriteAnimation.getSpriteRect());
        backgroundPanel.add(spriteAnimation);

        titleLabel = createLabel(loadingTitle, 23, new Rectangle(120, 50, 300, 20), backgroundPanel);
        loaderText = createLabel(loadingText, 11, new Rectangle(120, 70, 400, 20), backgroundPanel);
        loaderText.setForeground(new Color(239, 165, 50));
        setAlwaysOnTop(true);

        engine.getFrame().addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentMoved(java.awt.event.ComponentEvent evt) {
                updateLoadingFramePosition();
            }
        });

        int cornerRadius = 20;
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
    }

    private JLabel createLabel(String text, int fontSize, Rectangle bounds, JPanel panel) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, fontSize));
        label.setBounds(bounds);
        panel.add(label);
        return label;
    }

    private void updateLoadingFramePosition() {
        SwingUtilities.invokeLater(() -> {
            Point mainFrameCenter = getCenterPoint(engine.getFrame());
            int offsetX = mainFrameCenter.x - getWidth() / 2;
            int offsetY = mainFrameCenter.y - getHeight() / 2;
            setLocation(offsetX, offsetY);
        });
    }

    private Point getCenterPoint(JFrame frame) {
        int centerX = frame.getX() + frame.getWidth() / 2;
        int centerY = frame.getY() + frame.getHeight() / 2;
        return new Point(centerX, centerY);
    }

    public void startLoading() {
        setVisible(true);
        if (!isAnimating) {
            startAnimation();
        }
    }

    private void startAnimation() {
        isAnimating = true;
        animateDown();
    }

    private void animateDown() {
        loadingTimer.start();
        updateLoadingFramePosition();

        int targetY = engine.getFrame().getY() + engine.getFrame().getHeight() / 2 - getHeight() / 2;
        int startY = engine.getFrame().getY();

        Timer downTimer = new Timer(animationSpeed, new ActionListener() {
            int currentY = startY;
            final double acceleration = 0.12;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentY < targetY) {
                    setLocation(getX(), currentY);
                    currentY += acceleration * (targetY - currentY);

                    float opacity = (currentY - startY) / (float) (targetY - startY);
                    setOpacity(opacity);
                } else {
                    setLocation(getX(), targetY);
                    setOpacity(1.0f);
                    ((Timer) e.getSource()).stop();
                    oscillate();
                }
            }
        });
        downTimer.start();
    }

    private void oscillate() {
        int startX = getX();
        int deltaY = 5;
        int oscillationSpeed = 50;

        Timer oscillationTimer = new Timer(oscillationSpeed, new ActionListener() {
            int direction = 1;

            @Override
            public void actionPerformed(ActionEvent e) {
                setLocation(startX, getY() + direction * deltaY);
                direction *= -1;

                if (direction == 1) {
                    setOpacity(1.0f);
                }
            }
        });
        oscillationTimer.setRepeats(false);
        oscillationTimer.start();
    }

    private void animateUp() {
        loadingTimer.start();
        updateLoadingFramePosition();

        Timer upTimer = new Timer(animationSpeed, new ActionListener() {
            int targetHeight = 0;
            float targetOpacity = 0.0f;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (getHeight() > targetHeight || getOpacity() > targetOpacity) {
                    int newHeight = (int) (getHeight() * (1 - 0.12));
                    float newOpacity = Math.max(0.0f, getOpacity() - 0.12f);

                    setBounds(getX(), getY() - (getHeight() - newHeight), getWidth(), newHeight);
                    setOpacity(newOpacity);
                } else {
                    ((Timer) e.getSource()).stop();
                    stopLoading();
                    setVisible(false);
                }
            }
        });

        upTimer.start();
    }

    public void setOpacity(float opacity) {
        float clampedOpacity = Math.max(0.0f, Math.min(1.0f, opacity));
        super.setOpacity(clampedOpacity);
        repaint();
    }

    public void setLoadingText(String loadingText, String loadingTitle, int sleep) {
        this.loadingText = engine.getLANG().getString(loadingText);
        this.loadingTitle = engine.getLANG().getString(loadingTitle);
        loaderText.setText(this.loadingText);
        titleLabel.setText(this.loadingTitle + ".".repeat(dotLimit));

        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopLoading() {
        if (isAnimating) {
            animateUp();
            loadingTimer.stop();
            isAnimating = false;
        }
    }

    public Timer getLoadingTimer() {
        return loadingTimer;
    }

    public static class SpriteAnimation extends JComponent {
        private final BufferedImage spriteSheet;
        private int imgSize, totalFrames, currentFrame = 0;
        private final Rectangle spriteRect;

        public SpriteAnimation(String path, int frames, int delay, int imgSize, Rectangle spriteRect) {
            this.imgSize = imgSize;
            this.spriteSheet = ImageUtils.getLocalImage(path);
            this.totalFrames = frames;
            this.spriteRect = spriteRect;

            Timer timer = new Timer(delay, e -> {
                currentFrame = (currentFrame + 1) % totalFrames;
                repaint();
            });
            timer.start();
        }

        public Rectangle getSpriteRect() {
            return spriteRect;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int scaledWidth = getWidth();
            int scaledHeight = getHeight();

            g.drawImage(
                    ImageUtils.getByIndex(spriteSheet, this.imgSize, currentFrame),
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
