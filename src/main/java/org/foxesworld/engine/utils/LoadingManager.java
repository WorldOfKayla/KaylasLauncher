package org.foxesworld.engine.utils;

import org.foxesworld.engine.Engine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

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

    private static final int FRAME_WIDTH = 500;
    private static final int FRAME_HEIGHT = 150;

    public LoadingManager(Engine engine) {
        this.engine = engine;
        this.spriteAnimation = new SpriteAnimation("assets/ui/sprites/loaderGrid.png", 3, 5, 50, new Rectangle(30, 30, 64, 64));
        this.loadingTimer = new Timer(500, e -> loaderText.setText(loadingText));

        initializeLoadingFrame();
    }

    private void initializeLoadingFrame() {
        setUndecorated(true);
        setSize(FRAME_WIDTH, FRAME_HEIGHT);

        JPanel backgroundPanel = createBackgroundPanel();
        setContentPane(backgroundPanel);

        spriteAnimation.setBounds(spriteAnimation.getSpriteRect());
        backgroundPanel.add(spriteAnimation);

        titleLabel = createLabel(loadingTitle, 23, new Rectangle(120, 50, 300, 20), backgroundPanel);
        loaderText = createLabel(loadingText, 11, new Rectangle(120, 70, 400, 20), backgroundPanel);
        loaderText.setForeground(new Color(239, 165, 50));
        setAlwaysOnTop(true);

        addFrameComponentListener();

        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
    }

    private JPanel createBackgroundPanel() {
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
        backgroundPanel.setBounds(0, 0, getWidth(), getHeight());
        return backgroundPanel;
    }

    private JLabel createLabel(String text, int fontSize, Rectangle bounds, JPanel panel) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, fontSize));
        label.setBounds(bounds);
        panel.add(label);
        return label;
    }

    private void addFrameComponentListener() {
        engine.getFrame().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentMoved(java.awt.event.ComponentEvent evt) {
                updateLoadingFramePosition();
            }
        });
    }

    private void updateLoadingFramePosition() {
        SwingUtilities.invokeLater(() -> {
            Point mainFrameCenter = getCenterPoint(engine.getFrame());
            setLocation(mainFrameCenter.x - getWidth() / 2, mainFrameCenter.y - getHeight() / 2);
        });
    }

    private Point getCenterPoint(JFrame frame) {
        int centerX = frame.getX() + frame.getWidth() / 2;
        int centerY = frame.getY() + frame.getHeight() / 2;
        return new Point(centerX, centerY);
    }

    public void startLoading() {
        setVisible(true);
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
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
                    if (!loadingTimer.isRunning()) {
                        oscillate();
                    }
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

    public void toggleLoader() {
        if (loadingTimer.isRunning()) {
            stopLoading();
        } else {
            startLoading();
        }
    }
}
