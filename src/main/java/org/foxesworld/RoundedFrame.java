package org.foxesworld;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class RoundedFrame extends JFrame {
    public RoundedFrame() {
        // Убираем украшения окна
        setUndecorated(true);

        // Задаем размер окна
        setSize(400, 300);

        // Задаем закругленную форму
        int radius = 30; // Радиус закругления
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius, radius));

        // Устанавливаем расположение окна по центру
        setLocationRelativeTo(null);

        // Устанавливаем видимость окна
        setVisible(true);
    }

    public static void main(String[] args) {
        // Создаем и отображаем окно
        SwingUtilities.invokeLater(RoundedFrame::new);
    }
}
