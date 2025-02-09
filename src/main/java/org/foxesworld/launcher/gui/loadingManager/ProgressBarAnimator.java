package org.foxesworld.launcher.gui.loadingManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import org.foxesworld.Launcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ProgressBarAnimator {

    public interface ProgressListener {
        void onStart();
        void onProgress(int value);
        void onComplete();
    }

    private final Launcher launcher;
    private final JProgressBar progressBar;
    private final JLabel progressText;
    private final Rectangle originalBounds;
    private List<String> messages;
    private ProgressListener progressListener;
    private final ParticleEffect particleEffect;

    public ProgressBarAnimator(Launcher launcher, JProgressBar progressBar, JLabel progressText) {
        this.launcher = launcher;
        this.progressBar = progressBar;
        this.progressText = progressText;
        this.originalBounds = progressBar.getBounds();
        loadMessagesFromJson("assets/messages.json");
        this.particleEffect = new ParticleEffect();
    }

    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    private void loadMessagesFromJson(String filePath) {
        try (Reader reader = new InputStreamReader(
                ProgressBarAnimator.class.getClassLoader().getResourceAsStream(filePath),
                StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            messages = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                messages.add(jsonArray.get(i).getAsString());
            }
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            System.err.println("Ошибка загрузки сообщений: " + e.getMessage());
        }
    }

    /**
     * Метод обновляет текст метки, выбирая случайное сообщение.
     */
    private void updateRandomMessage() {
        if (messages != null && !messages.isEmpty()) {
            int randomIndex = (int) (Math.random() * messages.size());
            progressText.setText(messages.get(randomIndex));
        }
    }

    /**
     * Вычисляет новое значение координаты X так, чтобы компонент оставался по центру относительно originalBounds.
     */
    private int calculateCenteredX(int newWidth) {
        return originalBounds.x - (newWidth - originalBounds.width) / 2;
    }

    /**
     * Универсальный метод анимации, который использует таймер и лямбду для обновления параметров компонента.
     *
     * @param duration   длительность анимации в миллисекундах
     * @param updater    функция обновления, которая принимает ratio (от 0 до 1)
     * @param onComplete действие по завершению анимации
     */
    private void animate(final int duration, final java.util.function.Consumer<Double> updater, final Runnable onComplete) {
        SwingUtilities.invokeLater(() -> {
            final long startTime = System.currentTimeMillis();
            // Используем интервал 10 мс для более частых обновлений
            Timer timer = new Timer(10, null);
            timer.setInitialDelay(0);
            timer.setCoalesce(true);
            timer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double ratio = Math.min(1.0, (double) elapsed / duration);
                    updater.accept(ratio);
                    if (ratio >= 1.0) {
                        timer.stop();
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                }
            });
            timer.start();
        });
    }

    public void startProgressTest() {
        launcher.getExecutorServiceProvider().submitTask(() -> {
            final int MAX_VALUE = progressBar.getMaximum();
            int currentValue = 0;

            if (progressListener != null) {
                progressListener.onStart();
            }

            animateProgressBarEntrance(() -> updateRandomMessage());

            while (!Thread.currentThread().isInterrupted()) {
                final int valueToUpdate = currentValue;
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(valueToUpdate);
                    if (progressListener != null) {
                        progressListener.onProgress(valueToUpdate);
                    }
                });

                try {
                    // Уменьшено время ожидания для более частых обновлений
                    Thread.sleep(45);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                currentValue++;

                if (currentValue > MAX_VALUE) {
                    animateProgressBarExit(() -> {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setBounds(originalBounds);
                            progressBar.setValue(0);
                        });
                        animateProgressBarEntrance(this::updateRandomMessage);
                        if (progressListener != null) {
                            progressListener.onComplete();
                        }
                    });
                    currentValue = 0;
                }
            }
        }, "pbTest");
    }

    /**
     * Анимация входа прогресс-бара с уменьшением масштаба.
     *
     * @param onComplete действие по завершению анимации (например, обновление сообщения)
     */
    private void animateProgressBarEntrance(Runnable onComplete) {
        SwingUtilities.invokeLater(() -> {
            final int duration = 500;
            final double startScale = 1.2;
            final double endScale = 1.0;
            final int yOffset = 60;

            int initialWidth = (int) (originalBounds.width * startScale);
            int initialHeight = (int) (originalBounds.height * startScale);
            int initialX = calculateCenteredX(initialWidth);
            int initialY = originalBounds.y - yOffset;
            progressBar.setBounds(progressBar.getX(), initialY, initialWidth, initialHeight);
            progressBar.setVisible(true);

            animate(duration, ratio -> {
                double ease = 1 - Math.pow(1 - ratio, 2);
                if (ratio < 0.5) {
                    int newY = (int) ((originalBounds.y - yOffset) + yOffset * ease);
                    progressBar.setBounds(progressBar.getX(), newY, progressBar.getWidth(), progressBar.getHeight());
                } else {
                    double scale = startScale + (endScale - startScale) * ease;
                    int newWidth = (int) (originalBounds.width * scale);
                    int newHeight = (int) (originalBounds.height * scale);
                    int newX = calculateCenteredX(newWidth);
                    progressBar.setBounds(newX, progressBar.getY(), newWidth, newHeight);
                }
            }, onComplete);
        });
    }

    /**
     * Анимация выхода прогресс-бара с изменением масштаба и сдвигом по оси Y.
     *
     * @param onComplete действие по завершению анимации
     */
    private void animateProgressBarExit(Runnable onComplete) {
        SwingUtilities.invokeLater(() -> {
            final int duration = 500;
            final double startScale = 1.0;
            final double midScale = 0.85;
            final double endScale = 0.85;
            final int exitYOffset = 100;

            animate(duration, ratio -> {
                if (ratio < 0.5) {
                    double ease = 1 - Math.pow(1 - (ratio * 2), 2);
                    double scale = startScale + (midScale - startScale) * ease;
                    int newWidth = (int) (originalBounds.width * scale);
                    int newHeight = (int) (originalBounds.height * scale);
                    int newX = calculateCenteredX(newWidth);
                    progressBar.setBounds(newX, originalBounds.y, newWidth, newHeight);
                } else {
                    double ease = Math.pow((ratio - 0.5) * 2, 2);
                    double scale = midScale + (endScale - midScale) * ease;
                    int newWidth = (int) (originalBounds.width * scale);
                    int newHeight = (int) (originalBounds.height * scale);
                    int newY = (int) (originalBounds.y + exitYOffset * ease);
                    int newX = calculateCenteredX(newWidth);
                    progressBar.setBounds(newX, newY, newWidth, newHeight);
                }
            }, () -> {
                progressBar.setVisible(false);
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        });
    }
}
