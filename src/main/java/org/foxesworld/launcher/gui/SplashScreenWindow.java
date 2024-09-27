package org.foxesworld.launcher.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class SplashScreenWindow extends JWindow {
    private final ImageIcon imageIcon;
    private final ImageIcon backgroundImage;
    private final JLabel imageLabel;
    private float opacity = 1f; // Start with full transparency
    private final int fadeDuration = 2000;
    private final int fadeInterval = 40;

    public SplashScreenWindow() {
        backgroundImage = new ImageIcon(getClass().getClassLoader().getResource("assets/ui/img/bg/launch.jpg"));
        imageIcon = new ImageIcon(getClass().getClassLoader().getResource("assets/ui/icons/fwBanner.png"));
        imageLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                g2d.drawImage(imageIcon.getImage(), 0, 0, getWidth(), getHeight(), null);
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

        content.setBackground(Color.LIGHT_GRAY);
        content.add(imageLabel, BorderLayout.CENTER);

        getContentPane().add(content);
        setSize(500, 350);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0)); // Transparent background
    }

    private void createBackgroundWithOverlayAndRoundedCorners(Graphics g, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();

        // Create a rounded rectangle for the clipping mask
        Shape roundedRect = new RoundRectangle2D.Double(0, 0, width, height, 30, 30);
        g2d.setClip(roundedRect);

        // Draw the background image
        g2d.drawImage(backgroundImage.getImage(), 0, 0, width, height, null);

        // Create the dimming overlay with rounded corners
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); // Semi-transparent overlay
        g2d.setColor(Color.BLACK); // Dimming color
        g2d.fill(roundedRect); // Fill the overlay with rounded corners

        g2d.dispose();
    }

    public void showSplashScreen() {
        setVisible(true);

        int steps = fadeDuration / fadeInterval;
        float opacityStep = 1.0f / steps;

        SwingWorker<Void, Float> fadeWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                for (int i = 0; i <= steps; i++) {
                    opacity += opacityStep;
                    if (opacity > 1f) {
                        opacity = 1f;
                    }
                    publish(opacity);
                    try {
                        Thread.sleep(fadeInterval); // Sleep for the duration of the fade interval
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Float> chunks) {
                float latestOpacity = chunks.get(chunks.size() - 1);
                imageLabel.repaint();
            }

            @Override
            protected void done() {
                Timer closeTimer = new Timer(fadeDuration + fadeInterval, e -> {
                    setVisible(false);
                    dispose();
                });
                closeTimer.setRepeats(false);
                closeTimer.start();
            }
        };
        fadeWorker.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SplashScreenWindow splashScreen = new SplashScreenWindow();
            splashScreen.showSplashScreen();
        });
    }
}
