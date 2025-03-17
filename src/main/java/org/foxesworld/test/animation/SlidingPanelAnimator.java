package org.foxesworld.test.animation;

import javax.swing.*;
import java.awt.Container;

/**
 * SlidingPanelAnimator выполняет горизонтальную анимацию сдвига JPanel.
 * <p>
 * При вызове метода {@code startAnimation(boolean slideIn, Runnable onCompletion)}
 * панель плавно перемещается по оси X:
 * <ul>
 *   <li>Если {@code slideIn == true}, то панель "выезжает" с левой стороны (начальная позиция: -panel.getWidth(), конечная: 0).</li>
 *   <li>Если {@code slideIn == false}, то панель "уезжает" влево (начальная позиция: текущая позиция, конечная: -panel.getWidth()).</li>
 * </ul>
 * Для создания эффекта используется функция сглаживания (ease-out).
 */
public class SlidingPanelAnimator {

    private final JPanel panel;
    private final Container parent;
    private final int duration; // длительность анимации в миллисекундах
    private Timer timer;
    private int startX;
    private int endX;
    private final int delay = 15; // интервал обновления в миллисекундах

    /**
     * Конструктор класса.
     *
     * @param panel    анимируемая панель. Не должна быть null.
     * @param duration длительность анимации в миллисекундах.
     * @throws IllegalArgumentException если панель или её контейнер равны null.
     */
    public SlidingPanelAnimator(JPanel panel, int duration) {
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
     * Запускает анимацию сдвига панели с вызовом {@code onCompletion} по завершении.
     *
     * @param slideIn      если true – панель появляется (выезжает), если false – панель скрывается (уезжает).
     * @param onCompletion действие, которое будет выполнено после завершения анимации.
     */
    public void startAnimation(boolean slideIn, Runnable onCompletion) {
        if (slideIn) {
            // Для появления панели начинаем за левым краем
            startX = -panel.getWidth();
            endX = 0;
            panel.setLocation(startX, panel.getY());
            panel.setVisible(true);
        } else {
            // Для скрытия панели используем текущую позицию, а конечная – за левым краем
            startX = panel.getX();
            endX = -panel.getWidth();
        }

        final long startTime = System.currentTimeMillis();

        // Останавливаем предыдущую анимацию, если она ещё выполняется
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }

        timer = new Timer(delay, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1f, (float) elapsed / duration);
            // Функция ease-out: замедление в конце анимации
            float easedProgress = 1 - (float) Math.pow(1 - progress, 2);
            int newX = (int) (startX + (endX - startX) * easedProgress);
            panel.setLocation(newX, panel.getY());
            parent.repaint();

            if (progress >= 1f) {
                timer.stop();
                if (!slideIn) {
                    panel.setVisible(false);
                }
                if (onCompletion != null) {
                    onCompletion.run();
                }
            }
        });
        timer.start();
    }

    /**
     * Перегруженный метод для запуска анимации без callback.
     *
     * @param slideIn если true – панель появляется, если false – скрывается.
     */
    public void startAnimation(boolean slideIn) {
        startAnimation(slideIn, null);
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