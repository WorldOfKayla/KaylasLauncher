package org.foxesworld.launcher.gui.loadingManager;

import javax.swing.*;
import java.awt.*;
import java.util.TimerTask;

public class HearthstoneProgressBar extends JComponent {
    // Прогресс от 0.0 до 1.0
    private double progress = 0.0;

    /**
     * Устанавливает значение прогресса и перерисовывает компонент.
     * @param progress значение прогресса (0.0...1.0)
     */
    public void setProgress(double progress) {
        this.progress = Math.max(0.0, Math.min(1.0, progress));
        repaint();
    }

    /**
     * Возвращает текущее значение прогресса.
     */
    public double getProgress() {
        return progress;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Включаем сглаживание
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int arc = 20; // скругление углов

        // Рисуем фон (например, тёмно-серый)
        g2.setColor(new Color(40, 40, 40));
        g2.fillRoundRect(0, 0, width, height, arc, arc);

        // Вычисляем ширину заполненной области
        int fillWidth = (int) (width * progress);

        // Рисуем заливку с градиентом (например, от зелёного к жёлтому)
        if (fillWidth > 0) {
            GradientPaint gradient = new GradientPaint(0, 0, new Color(0, 200, 0), fillWidth, 0, new Color(255, 215, 0));
            g2.setPaint(gradient);
            g2.fillRoundRect(0, 0, fillWidth, height, arc, arc);
        }

        // Рисуем обводку
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);

        // Рисуем "искру" — небольшой круг, который двигается по передней границе заливки
        if (progress > 0 && progress < 1) {
            int sparkSize = 20;
            // Координата X — в конце заполненной области, но с поправкой, чтобы "искра" была по центру
            int sparkX = Math.min(fillWidth, width - sparkSize) - sparkSize / 2;
            int sparkY = height / 2 - sparkSize / 2;
            // Рисуем яркий круг с эффектом свечения
            RadialGradientPaint sparkPaint = new RadialGradientPaint(
                    new Point(sparkX + sparkSize / 2, sparkY + sparkSize / 2),
                    sparkSize / 2,
                    new float[]{0f, 1f},
                    new Color[]{Color.WHITE, new Color(255, 255, 255, 0)}
            );
            g2.setPaint(sparkPaint);
            g2.fillOval(sparkX, sparkY, sparkSize, sparkSize);
        }

        g2.dispose();
    }


    public static void main(String[] args) {
        // Создаём окно JFrame
        JFrame frame = new JFrame("Hearthstone Progress Bar Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 150);
        frame.setLocationRelativeTo(null);

        // Создаём экземпляр кастомного прогресс-бара
        HearthstoneProgressBar progressBar = new HearthstoneProgressBar();
        progressBar.setPreferredSize(new Dimension(350, 50));

        // Настраиваем панель и добавляем прогресс-бар
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 20));
        panel.add(progressBar);
        frame.add(panel);

        frame.setVisible(true);

        // Таймер для плавного увеличения прогресса
        Timer timer = new Timer(20, e -> {
            double currentProgress = progressBar.getProgress();
            if (currentProgress >= 1.0) {
                ((Timer) e.getSource()).stop(); // Останавливаем таймер, когда прогресс достиг 100%
            } else {
                progressBar.setProgress(currentProgress + 0.01);
            }
        });
        timer.start();
    }
}
