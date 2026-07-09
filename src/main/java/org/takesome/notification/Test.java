package org.takesome.notification;

import com.formdev.flatlaf.FlatIntelliJLaf;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Test {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            BufferedImage image = loadImageFromFile("/assets/ui/icons/logo.png");
            FlatIntelliJLaf.setup();
            NotificationPopup notification = new NotificationPopup();
            notification.display("Привет, как дела?", "Давай сегодня сходим в бар!", new ImageIcon(image));
        });
    }

    private static BufferedImage loadImageFromFile(String filePath) {
        try {
            return ImageIO.read(NotificationPopup.class.getResourceAsStream(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
