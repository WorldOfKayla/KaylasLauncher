package org.foxesworld.launcher;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.io.IOException;

public class Sandbox {

    public Sandbox() {
        Frame frame = new Frame("Button Example");
        Button button = new Button("Click Here");
        button.setBounds(50, 100, 80, 30);
        frame.add(button);
        frame.setSize(400, 400);
        frame.setLayout(null);
        frame.setVisible(true);

        button.addActionListener(e -> {
            if (SystemTray.isSupported()) {
                try {
                    NotificationHelper notificationHelper = new NotificationHelper();
                    notificationHelper.setTrayIconTitle("Custom Title");
                    notificationHelper.setTrayIconStatus("Application Running");
                    notificationHelper.showNotification("Hello, World", "Java Notification Demo In Windows", MessageType.INFO);
                } catch (AWTException ex) {
                    ex.printStackTrace();
                }
            } else {
                System.err.println("System tray not supported!");
            }
        });
    }

    public static void main(String[] args) {
        new Sandbox();
    }
}

class NotificationHelper {

    private TrayIcon trayIcon;

    public NotificationHelper() throws AWTException {
        initializeTrayIcon();
    }

    private void initializeTrayIcon() throws AWTException {
        SystemTray tray = SystemTray.getSystemTray();
        Image image = null;
        try {
            image = ImageIO.read(NotificationHelper.class.getClassLoader().getResourceAsStream("assets/ui/icons/logo.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        trayIcon = new TrayIcon(image, "Java AWT Tray Demo");
        trayIcon.setImageAutoSize(true);
        tray.add(trayIcon);
    }

    public void setTrayIconTitle(String title) {
        trayIcon.setToolTip(title);
    }

    public void setTrayIconStatus(String status) {
        trayIcon.setToolTip(status);
    }

    public void showNotification(String title, String message, MessageType messageType) {
        trayIcon.displayMessage(title, message, messageType);
    }


}
