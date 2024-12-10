package org.foxesworld.launcher.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class SplashScreenWindow extends JWindow {
    private final ImageIcon imageIcon;
    private final ImageIcon backgroundImage;
    private final JLabel imageLabel;
    private float opacity = 0f;
    private float scale = 0.8f; // Initial scale factor
    private final int fadeDuration = 600;
    private final int fadeInterval = 40;

    public SplashScreenWindow() {
        backgroundImage = new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("assets/ui/img/bg/launch.jpg")));
        imageIcon = new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("assets/ui/icons/fwBanner.png")));
        imageLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

                // Apply scaling effect smoothly
                int width = (int) (getWidth() * scale);
                int height = (int) (getHeight() * scale);
                int x = (getWidth() - width) / 2;
                int y = (getHeight() - height) / 2;

                g2d.drawImage(imageIcon.getImage(), x, y, width, height, null);
                g2d.dispose();
            }
        };

        JPanel content = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                createBackgroundWithOverlayAndRoundedCorners(g, getWidth(), getHeight());
            }

            @Override
            public void setOpaque(boolean isOpaque) {
                super.setOpaque(false);
            }
        };

        content.add(imageLabel, BorderLayout.CENTER);
        getContentPane().add(content);
        setSize(500, 350);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));
    }

    private void createBackgroundWithOverlayAndRoundedCorners(Graphics g, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();
        Shape roundedRect = new RoundRectangle2D.Double(0, 0, width, height, 30, 30);
        g2d.setClip(roundedRect);
        g2d.drawImage(backgroundImage.getImage(), 0, 0, width, height, null);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2d.setColor(hexToColor("#1e201eeb"));
        g2d.fill(roundedRect);
        g2d.dispose();
    }

    public void showSplashScreen() {
        setVisible(true);
        fadeInWithScale();
    }

    private void fadeInWithScale() {
        int steps = fadeDuration / fadeInterval;
        float opacityStep = 1.0f / steps;
        float scaleStep = (1.0f - scale) / steps; // Gradual scaling

        SwingWorker<Void, Void> fadeWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                for (int i = 0; i <= steps; i++) {
                    // Easing function for smooth scaling (ease-in effect)
                    float easedScale = easeInOut(i / (float) steps);
                    opacity = Math.min(1f, opacity + opacityStep);
                    scale = Math.min(1.0f, scale + scaleStep);

                    publish(); // Trigger repaint
                    try {
                        Thread.sleep(fadeInterval);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Void> chunks) {
                imageLabel.repaint();
            }

            @Override
            protected void done() {
                try {
                    get();
                    Timer closeTimer = new Timer(fadeDuration + fadeInterval, e -> {
                        // Delay before fading out
                        Timer delayTimer = new Timer(300, event -> {
                            setVisible(false);
                            dispose();
                        });
                        delayTimer.setRepeats(false);
                        delayTimer.start();
                    });
                    closeTimer.setRepeats(false);
                    closeTimer.start();
                } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                }
            }
        };
        fadeWorker.execute();
    }

    // Easing function for smooth transitions
    private float easeInOut(float t) {
        return t < 0.5f ? 2f * t * t : -1f + (4f - 2f * t) * t;
    }
}
