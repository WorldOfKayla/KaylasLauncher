package org.foxesworld.test.animation;

import javax.swing.*;
import java.awt.Container;

/**
 * DirectionalPanelAnimator позволяет выполнять анимацию перемещения панели в заданном направлении.
 * <p>
 * Реализованы два сценария:
 * <ul>
 *   <li><b>При появлении</b>: если slideIn == true и direction == VERTICAL, панель выезжает сверху.
 *       Начальная позиция: (targetX, -panel.getHeight()), конечная позиция: (targetX, targetY), где targetY – исходное значение Y панели.</li>
 *   <li><b>При скрытии</b>: если slideIn == false и direction == HORIZONTAL, панель уезжает направо.
 *       Начальная позиция: (currentX, targetY), конечная позиция: (parent.getWidth(), targetY).</li>
 * </ul>
 * спользуется функция сглаживания (ease-out) для плавного завершения анимации.
 */
public class DirectionalPanelAnimator {

    public enum Direction {
        VERTICAL,
        HORIZONTAL
    }

    private final JPanel panel;
    private final Container parent;
    private Timer timer;
    private final int duration; // длительность анимации в миллисекундах
    private final int delay = 15; // интервал обновления в миллисекундах

    /**
     * Конструктор.
     *
     * @param panel    анимируемая панель; не должна быть null.
     * @param duration длительность анимации в мс.
     * @throws IllegalArgumentException если панель или её родительский контейнер равны null.
     */
    public DirectionalPanelAnimator(JPanel panel, int duration) {
        if (panel == null) {
            throw new IllegalArgumentException("Панель не должна быть null.");
        }
        this.panel = panel;
        this.duration = duration;
        this.parent = panel.getParent();
        if (this.parent == null) {
            throw new IllegalArgumentException("Родительский контейнер панели не должен быть null.");
        }
    }

    /**
     * Запускает анимацию перемещения панели.
     *
     * @param slideIn      если true – анимация появления, если false – скрытия.
     * @param direction    направление анимации: VERTICAL или HORIZONTAL.
     * @param onCompletion действие, выполняемая по завершении анимации.
     */
    public void startAnimation(boolean slideIn, Direction direction, Runnable onCompletion) {
        // Перед запуском анимации убедимся, что размеры панели заданы
        if (panel.getWidth() == 0 || panel.getHeight() == 0) {
            panel.setSize(panel.getPreferredSize());
        }

        final long startTime = System.currentTimeMillis();
        // Сохраняем целевые координаты (предполагаем, что панель уже расположена в нужном месте)
        final int targetX = panel.getLocation().x;
        final int targetY = panel.getLocation().y;
        int startX, startY, endX, endY;

        if (slideIn) {
            if (direction == Direction.VERTICAL) {
                // Появление: выезд сверху
                startX = targetX;
                startY = -panel.getHeight();
                endX = targetX;
                endY = targetY;
            } else { // Direction.HORIZONTAL
                // Появление: выезд слева (если потребуется)
                startX = -panel.getWidth();
                startY = targetY;
                endX = targetX;
                endY = targetY;
            }
            panel.setLocation(startX, startY);
            panel.setVisible(true);
        } else {
            if (direction == Direction.HORIZONTAL) {
                // Скрытие: уезд направо
                startX = panel.getLocation().x;
                startY = targetY;
                endX = parent.getWidth();
                endY = targetY;
            } else { // Direction.VERTICAL
                // Скрытие: уезд вниз (если потребуется)
                startX = targetX;
                startY = panel.getLocation().y;
                endX = targetX;
                endY = parent.getHeight();
            }
        }

        // Остановка предыдущей анимации
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }

        timer = new Timer(delay, null);
        timer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1f, (float) elapsed / duration);
            float easedProgress = 1 - (float) Math.pow(1 - progress, 2);

            int newX = (int) (startX + (endX - startX) * easedProgress);
            int newY = (int) (startY + (endY - startY) * easedProgress);

            panel.setLocation(newX, newY);
            parent.repaint();

            if (progress >= 1f) {
                timer.stop();
                if (!slideIn) {
                    panel.setVisible(false);
                }
                parent.revalidate();
                if (onCompletion != null) {
                    onCompletion.run();
                }
            }
        });
        timer.start();
    }

    /**
     * Прерывает текущую анимацию, если она выполняется.
     */
    public void stopAnimation() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
    }
}