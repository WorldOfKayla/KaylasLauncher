package org.takesome.launcher.game;

import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.server.ServerAttributes;
import org.takesome.kaylasEngine.service.ScheduledTaskService;
import org.takesome.kaylasEngine.service.SessionTimer;

import java.time.Duration;
import java.util.Objects;

/**
 * Launcher adapter that binds game-session metadata to the engine-owned {@link SessionTimer}.
 */
public final class GameTimeTask implements AutoCloseable {
    private static final int DEFAULT_DELAY_SECONDS = 15;

    private final ServerAttributes serverAttributes;
    private final String userLogin;
    private final SessionTimer sessionTimer;
    private volatile GameTimeListener listener;

    public GameTimeTask(
            ServerAttributes serverAttributes,
            String userLogin,
            ScheduledTaskService scheduledTaskService
    ) {
        this(serverAttributes, userLogin, scheduledTaskService, DEFAULT_DELAY_SECONDS);
    }

    public GameTimeTask(
            ServerAttributes serverAttributes,
            String userLogin,
            ScheduledTaskService scheduledTaskService,
            int delaySeconds
    ) {
        this.serverAttributes = Objects.requireNonNull(serverAttributes, "serverAttributes");
        this.userLogin = userLogin == null ? "" : userLogin.trim();
        this.sessionTimer = new SessionTimer(
                Objects.requireNonNull(scheduledTaskService, "scheduledTaskService"),
                Duration.ofSeconds(Math.max(1, delaySeconds)),
                "game-time-" + safeTaskName(serverAttributes.getServerName())
        );
        this.sessionTimer.setUpdateListener(this::publishUpdate);
        this.sessionTimer.setErrorListener(this::publishError);
    }

    public void setGameTimeListener(GameTimeListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (!sessionTimer.start()) {
            Engine.LOGGER.warn("GameTimeTask is already running.");
            return;
        }
        Engine.LOGGER.info(
                "Game session timing started locally for user={} server={}",
                userLogin,
                serverAttributes.getServerName()
        );
    }

    public void finishPlaying() {
        if (!sessionTimer.isRunning()) {
            Engine.LOGGER.warn("GameTimeTask is not running.");
            return;
        }
        long totalElapsedSeconds = sessionTimer.finish().getSeconds();
        Engine.LOGGER.info(
                "Session finished locally. User={} server={} totalTimeSeconds={}",
                userLogin,
                serverAttributes.getServerName(),
                totalElapsedSeconds
        );
    }

    public void stop() {
        if (!sessionTimer.stop()) {
            Engine.LOGGER.warn("GameTimeTask is not running.");
            return;
        }
        Engine.LOGGER.info("GameTimeTask stopped.");
    }

    public void pause() {
        if (!sessionTimer.pause()) {
            Engine.LOGGER.warn("GameTimeTask is already paused or not running.");
            return;
        }
        Engine.LOGGER.info(
                "GameTimeTask paused. Accumulated time: {} seconds.",
                sessionTimer.elapsed().getSeconds()
        );
    }

    public void resume() {
        if (!sessionTimer.resume()) {
            Engine.LOGGER.warn("GameTimeTask is not paused or not running.");
            return;
        }
        Engine.LOGGER.info("GameTimeTask resumed.");
    }

    public void resetTiming() {
        sessionTimer.reset();
        Engine.LOGGER.info("Game session timer reset.");
    }

    public long getTotalElapsedSeconds() {
        return sessionTimer.elapsed().getSeconds();
    }

    public boolean isRunning() {
        return sessionTimer.isRunning();
    }

    public boolean isPaused() {
        return sessionTimer.isPaused();
    }

    @Override
    public void close() {
        sessionTimer.close();
    }

    private void publishUpdate(Duration elapsed) {
        GameTimeListener currentListener = listener;
        if (currentListener != null) {
            currentListener.onUpdate(new GameTimeResponse(elapsed.getSeconds()));
        }
    }

    private void publishError(Throwable error) {
        Engine.LOGGER.error("Error updating game time", error);
        GameTimeListener currentListener = listener;
        if (currentListener != null) {
            currentListener.onError(error);
        }
    }

    private static String safeTaskName(String value) {
        String normalized = value == null || value.isBlank() ? "unknown" : value.trim();
        return normalized.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    public interface GameTimeListener {
        void onUpdate(GameTimeResponse response);

        void onError(Throwable error);
    }
}
