package org.takesome.launcher.gui.loadingManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Drives the loading-screen progress bar without using the engine-wide background executor.
 *
 * <p>This animation is intentionally Swing-timer based. It is a UI-only loop and must not be
 * submitted to the shared executor, because long-lived UI loops can delay executor shutdown and
 * freeze the EDT if shutdown is requested from UI code.</p>
 */
public class ProgressBarAnimator {

    private static final int ANIMATION_FRAME_MS = 16;
    private static final int PROGRESS_UPDATE_MS = 100;

    public interface ProgressListener {
        void onStart();
        void onProgress(int value);
        void onComplete();
    }

    private final JProgressBar progressBar;
    private final JLabel progressText;
    private final Rectangle originalBounds;
    private final List<Timer> activeTimers = new CopyOnWriteArrayList<>();

    private List<String> messages;
    private ProgressListener progressListener;
    private final ParticleEffect particleEffect;
    private AnimationConfig animationConfig;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Timer progressTimer;
    private int progressValue = 0;

    public ProgressBarAnimator(Launcher launcher, JProgressBar progressBar, JLabel progressText) {
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
        try (InputStream inputStream = ProgressBarAnimator.class.getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) {
                messages = new ArrayList<>();
                return;
            }

            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
                messages = new ArrayList<>();
                for (int i = 0; i < jsonArray.size(); i++) {
                    messages.add(jsonArray.get(i).getAsString());
                }
            }
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            Engine.getLOGGER().warn("[LOAD-UI] Unable to load progress messages: {}", e.getMessage());
            messages = new ArrayList<>();
        }
    }

    private void loadAnimationConfig(String filePath) {
        try (InputStream inputStream = ProgressBarAnimator.class.getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) {
                animationConfig = new AnimationConfig();
                return;
            }

            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                animationConfig = gson.fromJson(reader, AnimationConfig.class);

                if (animationConfig == null) {
                    animationConfig = new AnimationConfig();
                    return;
                }

                if (animationConfig.entrance != null) {
                    animationConfig.entrance.sort(Comparator.comparingDouble(k -> k.time));
                }

                if (animationConfig.exit != null) {
                    animationConfig.exit.sort(Comparator.comparingDouble(k -> k.time));
                }
            }
        } catch (Exception e) {
            Engine.getLOGGER().warn("[LOAD-UI] Unable to load progress animation config: {}", e.getMessage());
            animationConfig = new AnimationConfig();
        }
    }

    /** Selects a random loading message for the progress label. */
    private void updateRandomMessage() {
        if (messages != null && !messages.isEmpty()) {
            int randomIndex = (int) (Math.random() * messages.size());
            progressText.setText(messages.get(randomIndex));
        }
    }

    /** Returns an x-coordinate that keeps a resized progress bar centered around its original bounds. */
    private int calculateCenteredX(int newWidth) {
        return originalBounds.x - (newWidth - originalBounds.width) / 2;
    }

    /**
     * Runs a one-shot animation on the EDT.
     *
     * @param duration animation duration in milliseconds
     * @param updater receives normalized progress in the {@code 0..1} range
     * @param onComplete optional completion callback executed on the EDT
     */
    private void animate(final int duration, final java.util.function.Consumer<Double> updater, final Runnable onComplete) {
        SwingUtilities.invokeLater(() -> {
            final long startTime = System.currentTimeMillis();
            Timer timer = new Timer(ANIMATION_FRAME_MS, null);
            timer.setInitialDelay(0);
            timer.setCoalesce(true);
            timer.addActionListener(e -> {
                long elapsed = System.currentTimeMillis() - startTime;
                double ratio = Math.min(1.0, (double) elapsed / duration);
                updater.accept(ratio);
                if (ratio >= 1.0) {
                    stopTrackedTimer(timer);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
            startTrackedTimer(timer);
        });
    }

    /**
     * Starts the synthetic loading progress loop.
     *
     * <p>The loop is a Swing timer, not a background task. This keeps progress animation ownership
     * inside Swing and prevents a long-lived {@code pbTest} task from blocking executor shutdown.</p>
     */
    public void startProgressTest() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (!running.get()) {
                return;
            }

            Engine.getLOGGER().debug("[LOAD-UI] progress animator start: source=SwingTimer, updateMs={}", PROGRESS_UPDATE_MS);
            progressValue = 0;
            if (progressListener != null) {
                progressListener.onStart();
            }
            animateProgressBarEntrance(() -> {
                updateRandomMessage();
                startProgressTimer();
            });
        });
    }

    private void startProgressTimer() {
        if (!running.get()) {
            return;
        }

        stopProgressTimer();
        final int maxValue = Math.max(1, progressBar.getMaximum());

        progressTimer = new Timer(PROGRESS_UPDATE_MS, event -> {
            if (!running.get()) {
                stopProgressTimer();
                return;
            }

            progressBar.setValue(progressValue);
            if (progressListener != null) {
                progressListener.onProgress(progressValue);
            }

            progressValue++;

            if (progressValue > maxValue) {
                stopProgressTimer();
                animateProgressBarExit(() -> {
                    progressBar.setBounds(originalBounds);
                    progressBar.setValue(0);
                    progressValue = 0;
                    if (progressListener != null) {
                        progressListener.onComplete();
                    }
                    if (running.get()) {
                        animateProgressBarEntrance(() -> {
                            updateRandomMessage();
                            startProgressTimer();
                        });
                    }
                });
            }
        });
        progressTimer.setInitialDelay(0);
        progressTimer.setCoalesce(true);
        startTrackedTimer(progressTimer);
    }

    /** Stops the progress loop and all in-flight progress-bar timeline timers. */
    public void stop() {
        running.set(false);
        SwingUtilities.invokeLater(() -> {
            stopProgressTimer();
            stopAllAnimationTimers();
            progressBar.setBounds(originalBounds);
            progressBar.setValue(0);
            Engine.getLOGGER().debug("[LOAD-UI] progress animator stopped");
        });
    }

    private void stopProgressTimer() {
        if (progressTimer != null) {
            stopTrackedTimer(progressTimer);
            progressTimer = null;
        }
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

    private static class AnimationConfig {
        List<AnimationKeyFrame> entrance;
        List<AnimationKeyFrame> exit;
    }

    private static class AnimationKeyFrame {
        double time;
        double scaleX;
        double scaleY;
        int offsetX;
        int offsetY;
        String interpolation;
    }

    /**
     * Applies keyframe-based geometry animation to the progress bar.
     *
     * @param duration animation duration in milliseconds
     * @param keyFrames sorted animation keyframes
     * @param onComplete optional completion callback executed on the EDT
     */
    private void animateWithTimeline(int duration, List<AnimationKeyFrame> keyFrames, Runnable onComplete) {
        SwingUtilities.invokeLater(() -> {
            final long startTime = System.currentTimeMillis();
            Timer timer = new Timer(ANIMATION_FRAME_MS, null);
            timer.setInitialDelay(0);
            timer.setCoalesce(true);

            timer.addActionListener(e -> {
                long elapsed = System.currentTimeMillis() - startTime;
                double progress = Math.min(1.0, (double) elapsed / duration);

                AnimationKeyFrame currentFrame = null;
                AnimationKeyFrame nextFrame = null;
                double segmentProgress = 0;

                for (int i = 0; i < keyFrames.size() - 1; i++) {
                    if (progress >= keyFrames.get(i).time && progress <= keyFrames.get(i + 1).time) {
                        currentFrame = keyFrames.get(i);
                        nextFrame = keyFrames.get(i + 1);
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

                    int newWidth = (int) (originalBounds.width * scaleX);
                    int newHeight = (int) (originalBounds.height * scaleY);
                    int newX = calculateCenteredX(newWidth) + offsetX;
                    int newY = originalBounds.y + offsetY;

                    progressBar.setBounds(newX, newY, newWidth, newHeight);
                }

                if (progress >= 1.0) {
                    stopTrackedTimer(timer);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
            startTrackedTimer(timer);
        });
    }

    private void applyAnimationFrame(AnimationKeyFrame frame, double ratio) {
        int newWidth = (int) (originalBounds.width * frame.scaleX);
        int newHeight = (int) (originalBounds.height * frame.scaleY);
        int newX = calculateCenteredX(newWidth) + frame.offsetX;
        int newY = originalBounds.y + frame.offsetY;

        progressBar.setBounds(newX, newY, newWidth, newHeight);
    }

    private void startTrackedTimer(Timer timer) {
        activeTimers.add(timer);
        timer.start();
    }

    private void stopTrackedTimer(Timer timer) {
        if (timer == null) {
            return;
        }
        timer.stop();
        activeTimers.remove(timer);
    }

    private void stopAllAnimationTimers() {
        for (Timer timer : activeTimers) {
            timer.stop();
        }
        activeTimers.clear();
    }

    private double interpolate(double start, double end, double ratio, String interpolation) {
        return switch (interpolation != null ? interpolation : "linear") {
            case "easeIn" -> start + (end - start) * (1 - Math.cos(ratio * Math.PI / 2));
            case "easeOut" -> start + (end - start) * Math.sin(ratio * Math.PI / 2);
            case "easeInOut" -> start + (end - start) * (0.5 - Math.cos(ratio * Math.PI) / 2);
            default -> start + (end - start) * ratio;
        };
    }
}
