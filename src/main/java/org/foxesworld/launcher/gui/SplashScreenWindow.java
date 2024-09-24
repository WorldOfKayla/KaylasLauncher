package org.foxesworld.launcher.gui;

import javax.swing.*;
import java.awt.*;

public class SplashScreenWindow {
    private final JWindow window;
    private final ImageIcon imageIcon;
    private final JLabel imageLabel;
    private float opacity = 1f;
    private final int fadeDuration = 2000;
    private final int fadeInterval = 40;

    public SplashScreenWindow() {
        window = new JWindow();
        imageIcon = new ImageIcon(getClass().getClassLoader().getResource("assets/ui/icons/fwBanner.png"));
        imageLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                g2d.drawImage(imageIcon.getImage(), 0, 0, getWidth(), getHeight(), null);
                g2d.dispose();
            }
        };

        JPanel content = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2d.dispose();
            }

            @Override
            public void setOpaque(boolean isOpaque) {
                super.setOpaque(false);
            }
        };

        content.setBackground(Color.LIGHT_GRAY);
        content.add(imageLabel, BorderLayout.CENTER);

        window.getContentPane().add(content);
        window.setSize(500, 350);
        window.setLocationRelativeTo(null);
        window.setBackground(new Color(0, 0, 0, 0));
    }

    public void showSplashScreen() {
        window.setVisible(true);

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
                return null; // Return null as the result is not needed
            }

            @Override
            protected void process(java.util.List<Float> chunks) {
                float latestOpacity = chunks.get(chunks.size() - 1);
                imageLabel.repaint();
            }

            @Override
            protected void done() {
                Timer closeTimer = new Timer(fadeDuration + fadeInterval, e -> {
                    window.setVisible(false);
                    window.dispose();
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