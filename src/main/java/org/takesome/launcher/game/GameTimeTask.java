package org.takesome.launcher.game;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.server.ServerAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * GameTimeTask tracks the duration of a game session locally.
 *
 * <p>Remote play-time/status requests are disabled. Backend-managed telemetry should
 * be added through KaylasLauncherBackend when a new protocol is defined.</p>
 */
public class GameTimeTask {

    private static final int DEFAULT_DELAY_SECONDS = 15;
    private Launcher launcher;
    private final int delaySeconds;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService executorService;
    private final ServerAttributes serverAttributes;
    private final String userLogin;

    // Start time of the current (or resumed) timing period
    private volatile Instant startTime;
    // Accumulated time (excluding the current period)
    private volatile Duration accumulatedTime = Duration.ZERO;
    // Flags for session activity and pause state
    private volatile boolean running = false;
    private volatile boolean paused = false;
    private ScheduledFuture<?> scheduledFuture;
    private GameTimeListener listener;

    public GameTimeTask(ServerAttributes serverAttributes, String userLogin, ExecutorService executorService, Launcher launcher) {
        this(serverAttributes, userLogin, executorService, launcher, DEFAULT_DELAY_SECONDS);
    }

    public GameTimeTask(ServerAttributes serverAttributes, String userLogin, ExecutorService executorService, Launcher launcher, int delaySeconds) {
        this.launcher = launcher;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.executorService = executorService;
        this.serverAttributes = serverAttributes;
        this.userLogin = userLogin;
        this.delaySeconds = delaySeconds;
    }

    /**
     * Sets the game time listener for receiving updates.
     *
     * @param listener the listener to be notified
     */
    public void setGameTimeListener(GameTimeListener listener) {
        this.listener = listener;
    }

    /**
     * Starts the game time session.
     */
    public synchronized void start() {
        if (running) {
            Engine.LOGGER.warn("GameTimeTask is already running.");
            return;
        }
        running = true;
        paused = false;
        accumulatedTime = Duration.ZERO;
        startTime = Instant.now();
        Engine.LOGGER.info("Game session timing started locally for user={} server={}", userLogin, serverAttributes.getServerName());
        // Schedule periodic local play-time update. Remote status/playtime requests are disabled.
        scheduleTask();
        Engine.LOGGER.info("GameTimeTask started with an interval of {} seconds; remote player status check is disabled.", delaySeconds);
    }

    /**
     * Finishes the session and sends the final play time.
     */
    public synchronized void finishPlaying() {
        if (!running) {
            Engine.LOGGER.warn("GameTimeTask is not running.");
            return;
        }
        cancelScheduledTask();
        long totalElapsedSeconds = getTotalElapsedSeconds();
        running = false;
        Engine.LOGGER.info("Session finished locally. User={} server={} totalTimeSeconds={}",
                userLogin, serverAttributes.getServerName(), totalElapsedSeconds);
    }

    /**
     * Stops the task and shuts down the scheduler.
     */
    public synchronized void stop() {
        if (!running) {
            Engine.LOGGER.warn("GameTimeTask is not running.");
            return;
        }
        running = false;
        cancelScheduledTask();
        scheduler.shutdownNow();
        Engine.LOGGER.info("GameTimeTask has been stopped successfully.");
    }

    /**
     * Manually pauses the timer (without stopping the scheduler).
     * The elapsed time for the current period is accumulated automatically.
     */
    public synchronized void pause() {
        if (!running || paused) {
            Engine.LOGGER.warn("GameTimeTask is already paused or not running.");
            return;
        }
        accumulatedTime = accumulatedTime.plus(Duration.between(startTime, Instant.now()));
        paused = true;
        Engine.LOGGER.info("GameTimeTask paused. Accumulated time: {} seconds.", accumulatedTime.getSeconds());
    }

    /**
     * Resumes the timer without restarting the scheduler.
     */
    public synchronized void resume() {
        if (!running || !paused) {
            Engine.LOGGER.warn("GameTimeTask is not paused or not running.");
            return;
        }
        startTime = Instant.now();
        paused = false;
        Engine.LOGGER.info("GameTimeTask resumed.");
    }

    /**
     * Schedules the periodic task to update game time.
     */
    private void scheduleTask() {
        scheduledFuture = scheduler.scheduleAtFixedRate(this::updateGameTime, 0, delaySeconds, TimeUnit.SECONDS);
    }

    private void cancelScheduledTask() {
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
        }
    }

    /**
     * Returns the total accumulated time in seconds.
     * If the task is paused, returns the accumulated time;
     * otherwise, adds the time from the current period.
     *
     * @return total elapsed time in seconds
     */
    private long getTotalElapsedSeconds() {
        if (paused) {
            return accumulatedTime.getSeconds();
        } else {
            return accumulatedTime.plus(Duration.between(startTime, Instant.now())).getSeconds();
        }
    }

    /**
     * Periodically publishes elapsed play time locally without remote network requests.
     */
    private void updateGameTime() {
        try {
            if (!running || paused) {
                return;
            }

            long elapsedSeconds = getTotalElapsedSeconds();
            if (listener != null) {
                listener.onUpdate(new GameTimeResponse(elapsedSeconds));
            }
        } catch (Exception e) {
            Engine.LOGGER.error("Error updating game time", e);
            if (listener != null) {
                listener.onError(e);
            }
        }
    }

    /**
     * Resets the timer by zeroing the accumulated time and setting the current start time.
     */
    public synchronized void resetTiming() {
        startTime = Instant.now();
        accumulatedTime = Duration.ZERO;
        Engine.LOGGER.info("Timer reset.");
    }

    /**
     * Listener interface for receiving game time notifications.
     */
    public interface GameTimeListener {
        /**
         * Called when the game time is updated.
         *
         * @param response a GameTimeResponse containing the current elapsed time
         */
        void onUpdate(GameTimeResponse response);
        /**
         * Called when an error occurs during the update process.
         *
         * @param error the encountered error
         */
        void onError(Throwable error);
    }
}
