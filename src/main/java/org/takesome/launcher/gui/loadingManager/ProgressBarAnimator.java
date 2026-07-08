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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Drives the loading-screen progress bar without using the engine-wide background executor.
 *
 * <p>The progress indicator is intentionally conservative: one Swing timer updates the value and
 * message text at a low frequency. Geometry/keyframe animation is not used in the loading hot path,
 * because resizing and repositioning Swing components while sprites and overlays repaint can starve
 * the EDT.</p>
 */
public class ProgressBarAnimator {

    private static final int PROGRESS_UPDATE_MS = 250;

    public interface ProgressListener {
        void onStart();
        void onProgress(int value);
        void onComplete();
    }

    private final JProgressBar progressBar;
    private final JLabel progressText;
    private final Rectangle originalBounds;

    private List<String> messages;
    private ProgressListener progressListener;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Timer progressTimer;
    private int progressValue = 0;

    public ProgressBarAnimator(Launcher launcher, JProgressBar progressBar, JLabel progressText) {
        this.progressBar = progressBar;
        this.progressText = progressText;
        this.originalBounds = progressBar.getBounds();
        loadMessagesFromJson("assets/messages.json");
        loadAnimationConfig("assets/animation_config.json");
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

    /**
     * Loads the config resource only to preserve diagnostics for missing or malformed config files.
     *
     * <p>The loading UI no longer applies geometry keyframes from this file at runtime. Keeping the
     * read makes broken resource packaging visible without adding repaint-heavy animation work.</p>
     */
    private void loadAnimationConfig(String filePath) {
        try (InputStream inputStream = ProgressBarAnimator.class.getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) {
                return;
            }

            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                new Gson().fromJson(reader, Object.class);
            }
        } catch (Exception e) {
            Engine.getLOGGER().warn("[LOAD-UI] Unable to load progress animation config: {}", e.getMessage());
        }
    }

    /** Selects a random loading message for the progress label. */
    private void updateRandomMessage() {
        if (messages != null && !messages.isEmpty()) {
            int randomIndex = (int) (Math.random() * messages.size());
            progressText.setText(messages.get(randomIndex));
        }
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
            progressBar.setBounds(originalBounds);
            progressBar.setVisible(true);
            updateRandomMessage();

            if (progressListener != null) {
                progressListener.onStart();
            }

            startProgressTimer();
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
                progressValue = 0;
                progressBar.setValue(0);
                updateRandomMessage();
                if (progressListener != null) {
                    progressListener.onComplete();
                }
            }
        });
        progressTimer.setInitialDelay(0);
        progressTimer.setCoalesce(true);
        progressTimer.start();
    }

    /** Stops the progress loop and restores the progress bar to its original bounds. */
    public void stop() {
        running.set(false);
        SwingUtilities.invokeLater(() -> {
            stopProgressTimer();
            progressBar.setBounds(originalBounds);
            progressBar.setValue(0);
            progressBar.setVisible(false);
            Engine.getLOGGER().debug("[LOAD-UI] progress animator stopped");
        });
    }

    private void stopProgressTimer() {
        if (progressTimer != null) {
            progressTimer.stop();
            progressTimer = null;
        }
    }
}
