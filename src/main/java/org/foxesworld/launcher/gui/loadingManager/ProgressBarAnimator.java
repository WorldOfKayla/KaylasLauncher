package org.foxesworld.launcher.gui.loadingManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import org.foxesworld.Launcher;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
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
    private AnimationConfig animationConfig;

    public ProgressBarAnimator(Launcher launcher, JProgressBar progressBar, JLabel progressText) {
        this.launcher = launcher;
        this.progressBar = progressBar;
        this.progressText = progressText;
        this.originalBounds = progressBar.getBounds();
        loadMessagesFromJson("assets/messages.json");
        loadAnimationConfig("assets/animation_config.json");
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

    private void loadAnimationConfig(String filePath) {
        try (Reader reader = new InputStreamReader(
                ProgressBarAnimator.class.getClassLoader().getResourceAsStream(filePath),
                StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            animationConfig = gson.fromJson(reader, AnimationConfig.class);

            animationConfig.entrance.sort(Comparator.comparingDouble(k -> k.time));
            animationConfig.exit.sort(Comparator.comparingDouble(k -> k.time));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки конфигурации анимации: " + e.getMessage());
            animationConfig = new AnimationConfig();
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
            Timer timer = new Timer(10, null);
            timer.setInitialDelay(0);
            timer.setCoalesce(true);
            timer.addActionListener(e -> {
                long elapsed = System.currentTimeMillis() - startTime;
                double ratio = Math.min(1.0, (double) elapsed / duration);
                updater.accept(ratio);
                if (ratio >= 1.0) {
                    timer.stop();
                    if (onComplete != null) {
                        onComplete.run();
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

            animateProgressBarEntrance(this::updateRandomMessage);

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
                    Thread.sleep(55);
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

    private void animateProgressBarEntrance(Runnable onComplete) {
        if (animationConfig.entrance == null || animationConfig.entrance.isEmpty()) {
            onComplete.run();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            progressBar.setVisible(true);
            animateWithTimeline(500, animationConfig.entrance, onComplete);
        });
    }

    private void animateProgressBarExit(Runnable onComplete) {
        if (animationConfig.exit == null || animationConfig.exit.isEmpty()) {
            onComplete.run();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            animateWithTimeline(500, animationConfig.exit, () -> {
                progressBar.setVisible(false);
                onComplete.run();
            });
        });
    }

    private class AnimationConfig {
        List<AnimationKeyFrame> entrance;
        List<AnimationKeyFrame> exit;
    }

    private class AnimationKeyFrame {
        double time;
        double scaleX;
        double scaleY;
        int offsetX;
        int offsetY;
        String interpolation;
    }


    private void animateWithTimeline(int duration, List<AnimationKeyFrame> keyFrames, Runnable onComplete) {
        SwingUtilities.invokeLater(() -> {
            final long startTime = System.currentTimeMillis();
            Timer timer = new Timer(10, null);
            timer.setInitialDelay(0);
            timer.setCoalesce(true);

            timer.addActionListener(e -> {
                long elapsed = System.currentTimeMillis() - startTime;
                double progress = Math.min(1.0, (double) elapsed / duration);

                // Находим текущий и следующий ключевые кадры
                AnimationKeyFrame currentFrame = null;
                AnimationKeyFrame nextFrame = null;
                double segmentProgress = 0;

                for (int i = 0; i < keyFrames.size() - 1; i++) {
                    if (progress >= keyFrames.get(i).time && progress <= keyFrames.get(i+1).time) {
                        currentFrame = keyFrames.get(i);
                        nextFrame = keyFrames.get(i+1);
                        segmentProgress = (progress - currentFrame.time) /
                                (nextFrame.time - currentFrame.time);
                        break;
                    }
                }

                if (currentFrame != null && nextFrame != null) {
                    double scaleX = interpolate(
                            currentFrame.scaleX,
                            nextFrame.scaleX,
                            segmentProgress,
                            nextFrame.interpolation
                    );

                    double scaleY = interpolate(
                            currentFrame.scaleY,
                            nextFrame.scaleY,
                            segmentProgress,
                            nextFrame.interpolation
                    );

                    int offsetX = (int) interpolate(
                            currentFrame.offsetX,
                            nextFrame.offsetX,
                            segmentProgress,
                            nextFrame.interpolation
                    );

                    int offsetY = (int) interpolate(
                            currentFrame.offsetY,
                            nextFrame.offsetY,
                            segmentProgress,
                            nextFrame.interpolation
                    );

                    // Применяем вычисленные значения
                    int newWidth = (int) (originalBounds.width * scaleX);
                    int newHeight = (int) (originalBounds.height * scaleY);
                    int newX = calculateCenteredX(newWidth) + offsetX;
                    int newY = originalBounds.y + offsetY;

                    progressBar.setBounds(newX, newY, newWidth, newHeight);
                }

                if (progress >= 1.0) {
                    timer.stop();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
            timer.start();
        });
    }

    private void applyAnimationFrame(AnimationKeyFrame frame, double ratio) {
        int newWidth = (int) (originalBounds.width * frame.scaleX);
        int newHeight = (int) (originalBounds.height * frame.scaleY);
        int newX = calculateCenteredX(newWidth) + frame.offsetX;
        int newY = originalBounds.y + frame.offsetY;

        progressBar.setBounds(newX, newY, newWidth, newHeight);
    }
    private double interpolate(double start, double end, double ratio, String interpolation) {
        switch (interpolation != null ? interpolation : "linear") {
            case "easeIn":
                return start + (end - start) * (1 - Math.cos(ratio * Math.PI / 2));
            case "easeOut":
                return start + (end - start) * Math.sin(ratio * Math.PI / 2);
            case "easeInOut":
                return start + (end - start) * (0.5 - Math.cos(ratio * Math.PI)/2);
            default: // linear
                return start + (end - start) * ratio;
        }
    }

}
