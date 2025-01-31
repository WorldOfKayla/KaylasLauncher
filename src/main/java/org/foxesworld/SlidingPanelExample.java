package org.foxesworld;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SlidingPanelExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(SlidingPanelExample::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Sliding Panel Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(null);

        // Создаем выезжающую панель
        JPanel slidingPanel = new JPanel();
        slidingPanel.setBackground(Color.WHITE);
        slidingPanel.setBounds(-250, 0, 250, frame.getHeight());
        slidingPanel.setLayout(null);

        // Добавляем аватар
        JLabel avatar = new JLabel();
        avatar.setBounds(85, 20, 80, 80);
        avatar.setIcon(new ImageIcon(new ImageIcon("path/to/avatar.jpg").getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH)));
        slidingPanel.add(avatar);

        // Добавляем имя пользователя
        JLabel usernameLabel = new JLabel("AidenFox", SwingConstants.CENTER);
        usernameLabel.setBounds(50, 110, 150, 20);
        usernameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        slidingPanel.add(usernameLabel);

        // Добавляем кнопки
        String[] options = {"Настройки", "Загрузить скин", "Сайт RedServer", "Выйти из аккаунта"};
        Icon[] icons = {
                UIManager.getIcon("OptionPane.informationIcon"),
                UIManager.getIcon("OptionPane.questionIcon"),
                UIManager.getIcon("OptionPane.warningIcon"),
                UIManager.getIcon("OptionPane.errorIcon")
        };

        for (int i = 0; i < options.length; i++) {
            JButton button = new JButton(options[i], icons[i]);
            button.setBounds(20, 150 + i * 50, 210, 40);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setFocusPainted(false);
            button.setBackground(Color.WHITE);
            slidingPanel.add(button);
        }

        JLabel footerLabel = new JLabel("RedLauncher\nВерсия 3.6.1", SwingConstants.CENTER);
        footerLabel.setBounds(20, frame.getHeight() - 60, 210, 40);
        footerLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        slidingPanel.add(footerLabel);

        frame.add(slidingPanel);

        // Кнопка для управления панелью
        JButton toggleButton = new JButton(" Меню");
        toggleButton.setBounds(10, 10, 100, 30);
        frame.add(toggleButton);

        // Кнопка для закрытия панели
        JButton closeButton = new JButton("Закрыть панель");
        closeButton.setBounds(120, 10, 120, 30);
        frame.add(closeButton);

        // Анимация выдвижения и задвижения панели
        Timer slideOutTimer = new Timer(5, null);
        Timer slideInTimer = new Timer(5, null);

        slideOutTimer.addActionListener(new ActionListener() {
            int x = slidingPanel.getX();

            @Override
            public void actionPerformed(ActionEvent e) {
                if (x < 0) {
                    x += 5;
                    slidingPanel.setBounds(x, 0, 250, frame.getHeight());
                } else {
                    slideOutTimer.stop();
                }
            }
        });

        slideInTimer.addActionListener(new ActionListener() {
            int x = slidingPanel.getX();

            @Override
            public void actionPerformed(ActionEvent e) {
                if (x > -250) {
                    x -= 5;
                    slidingPanel.setBounds(x, 0, 250, frame.getHeight());
                } else {
                    slideInTimer.stop();
                }
            }
        });

        // Обработчик клика кнопки для открытия панели
        toggleButton.addActionListener(e -> {
            if (slidingPanel.getX() == -250) {
                slideOutTimer.start();
            } else if (slidingPanel.getX() == 0) {
                slideInTimer.start();
            }
        });

        // Обработчик клика кнопки для закрытия панели
        closeButton.addActionListener(e -> {
            if (slidingPanel.getX() == 0) {
                slideInTimer.start();
            }
        });

        frame.setVisible(true);
    }
}
