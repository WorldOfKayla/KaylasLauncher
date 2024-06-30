package org.foxesworld.Notification;

import java.awt.image.BufferedImage;

interface NotificationDisplay {
    void display(String title, String description, BufferedImage image);
}