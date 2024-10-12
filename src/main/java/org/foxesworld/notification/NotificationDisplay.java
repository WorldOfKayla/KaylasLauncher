package org.foxesworld.notification;

import javax.swing.*;

interface NotificationDisplay {
    void display(String title, String description, ImageIcon image);
}