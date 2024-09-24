package org.foxesworld.launcher.gui;

import javax.swing.*;
import java.awt.*;

public class SplashScreenWindow {
    private final JWindow window;
    private final JLabel imageLabel;
    private final JLabel messageLabel;
    private Timer dotTimer;
    private String baseMessage = "Loading";

    public SplashScreenWindow() {
        window = new JWindow();
        imageLabel = new JLabel(new ImageIcon(getClass().getClassLoader().getResource("assets/ui/icons/fwBanner.png")));
        messageLabel = new JLabel(baseMessage, JLabel.CENTER);

        messageLabel.setFont(new Font("Arial", Font.BOLD, 16));
        messageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0,0,0));
        messageLabel.setForeground(Color.BLACK);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(imageLabel, BorderLayout.NORTH);
        content.add(messageLabel, BorderLayout.CENTER);

        window.getContentPane().add(content);
        window.setSize(500, 350);
        window.setLocationRelativeTo(null);
        window.setBackground(new Color(0, 0, 0, 0));
    }

    public void showSplashScreen() {
        window.setVisible(true);

        dotTimer = new Timer(500, e -> {
            String currentText = messageLabel.getText();
            if (currentText.endsWith("...")) {
                messageLabel.setText(baseMessage);
            } else {
                messageLabel.setText(currentText + ".");
            }
        });
        dotTimer.start();

        Timer timer = new Timer(3000, e -> {
            window.setVisible(false);
            window.dispose();
            dotTimer.stop();
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void dispose() {
        window.dispose();
    }

    public void setMessage(String message) {
        baseMessage = message;
        messageLabel.setText(baseMessage);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SplashScreenWindow splashScreen = new SplashScreenWindow();
            splashScreen.showSplashScreen();
        });
    }
}
