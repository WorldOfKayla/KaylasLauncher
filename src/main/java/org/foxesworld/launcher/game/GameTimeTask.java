package org.foxesworld.launcher.game;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * GameTimeTask tracks the duration of a game session and sends periodic updates.
 * It monitors the player's status and automatically pauses/resumes the timer based on the player's activity.
 * If a new session is detected (via a change in the server's start timestamp), the timer is reset.
 */
public class GameTimeTask {

    private static final int DEFAULT_DELAY_SECONDS = 15;
    private Launcher launcher;
    private static final Gson gson = new Gson();

    private final int delaySeconds;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService executorService;
    private final HTTPrequest postRequest;
    private final ServerAttributes serverAttributes;
    private final String userLogin;

    // Start time of the current (or resumed) timing period
    private volatile Instant startTime;
    // Accumulated time (excluding the current period)
    private volatile Duration accumulatedTime = Duration.ZERO;
    // Flags for session activity and pause state
    private volatile boolean running = false;
    private volatile boolean paused = false;
    // Stores the last server startTimestamp to detect a new session
    private volatile long lastServerStartTimestamp = 0;

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
        this.postRequest = new HTTPrequest(launcher, "POST");
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
        // Send request indicating session start
        writePlayTime("startedPlaying", 0);
        // Schedule periodic status check and time update
        scheduleTask();
        Engine.LOGGER.info("GameTimeTask started with an interval of {} seconds.", delaySeconds);
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
        writePlayTime("donePlaying", totalElapsedSeconds);
        running = false;
        Engine.LOGGER.info("Session finished. Total time: {} seconds.", totalElapsedSeconds);
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
     * Periodically checks the player's status.
     * If the player is online, the timer is updated.
     * When a new server startTimestamp is detected, the timer resets.
     * If the player goes offline, the timer is paused.
     */
    private void updateGameTime() {
        try {
            PlayerStatusResponse statusResponse = getStatus();
            if (statusResponse == null) {
                Engine.LOGGER.warn("Failed to retrieve player status.");
                return;
            }

            if (statusResponse.isPlaying()) {
                // Если обнаружен новый запуск сессии, сбрасываем таймер и обновляем timestamp
                if (statusResponse.getStartTimestamp() != lastServerStartTimestamp) {
                    Engine.LOGGER.info("New session detected (server startTimestamp: {}). Resetting timer.",
                            statusResponse.getStartTimestamp());
                    resetTiming();
                    lastServerStartTimestamp = statusResponse.getStartTimestamp();
                    writePlayTime("startedPlaying", 0);
                } else if (paused) { // Если не новый сеанс, но таймер на паузе — возобновляем его
                    Engine.LOGGER.info("Player returned to the server. Resuming session.");
                    resume();
                }
                long elapsedSeconds = getTotalElapsedSeconds();
                writePlayTime("playing", elapsedSeconds);
                if (listener != null) {
                    listener.onStatusUpdate(statusResponse);
                    listener.onUpdate(new GameTimeResponse(elapsedSeconds));
                }
            } else {
                if (!paused) {
                    pause();
                    Engine.LOGGER.info("Player left the server. Timer paused.");
                }
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
     * Builds the player data payload for sending.
     *
     * @param elapsedTime elapsed time in seconds
     * @param action      the action to be reported
     * @return a map containing player data
     */
    private Map<String, Object> buildPlayerData(long elapsedTime, String action) {
        Map<String, Object> playerData = new HashMap<>();
        playerData.put("serverName", serverAttributes.getServerName());
        playerData.put("login", userLogin);
        playerData.put("playTime", elapsedTime);
        playerData.put("sysRequest", action);
        playerData.put("serverIp", serverAttributes.getHost());
        playerData.put("serverPort", serverAttributes.getPort());
        return playerData;
    }

    /**
     * Sends game time data asynchronously.
     *
     * @param action      the action indicating the state (e.g., "playing", "donePlaying")
     * @param elapsedTime the elapsed time in seconds
     */
    private void writePlayTime(String action, long elapsedTime) {
        executorService.submit(() -> {
            try {
                Map<String, Object> playerData = buildPlayerData(elapsedTime, action);
                postRequest.sendAsync(playerData,
                        response -> Engine.LOGGER.info("Game time sent: {}", response),
                        error -> Engine.LOGGER.error("Error sending game time", error));
            } catch (Exception e) {
                Engine.LOGGER.error("Unexpected error in writePlayTime", e);
            }
        });
    }

    /**
     * Synchronously checks the player's status by sending a request.
     *
     * @return the player's status response or null if an error occurs
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public PlayerStatusResponse getStatus() throws InterruptedException {
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("sysRequest", "checkStatus");
        statusData.put("login", userLogin);
        statusData.put("serverIp", serverAttributes.getHost());
        statusData.put("serverPort", serverAttributes.getPort());

        Engine.LOGGER.debug("Sending status check request: {}", statusData);

        final PlayerStatusResponse[] responseHolder = new PlayerStatusResponse[1];
        CountDownLatch latch = new CountDownLatch(1);

        postRequest.sendAsync(statusData,
                response -> {
                    try {
                        Engine.LOGGER.debug("Received status response: {}", response);
                        if (response instanceof String) {
                            PlayerStatusResponse statusResponse = gson.fromJson((String) response, PlayerStatusResponse.class);
                            if (statusResponse != null) {
                                Engine.LOGGER.info("Player status: {}, isPlaying: {}{}",
                                        statusResponse.getMessage(),
                                        statusResponse.isPlaying(),
                                        statusResponse.isPlaying() ? ", startTimestamp: " + statusResponse.getStartTimestamp() : "");
                                responseHolder[0] = statusResponse;
                            } else {
                                Engine.LOGGER.warn("JSON parsing of status returned null.");
                            }
                        } else {
                            Engine.LOGGER.warn("Unexpected response type: {}", response.getClass().getName());
                        }
                    } catch (JsonSyntaxException e) {
                        Engine.LOGGER.error("Error parsing JSON status response", e);
                    } finally {
                        latch.countDown();
                    }
                },
                error -> {
                    Engine.LOGGER.error("Error checking player status", error);
                    latch.countDown();
                }
        );

        latch.await();
        return responseHolder[0];
    }

    /**
     * Listener interface for receiving notifications about game time updates and status changes.
     */
    public interface GameTimeListener {
        /**
         * Called when the game time is updated.
         *
         * @param response a GameTimeResponse containing the current elapsed time
         */
        void onUpdate(GameTimeResponse response);

        /**
         * Called when a status update is received.
         *
         * @param statusResponse the current player status response
         */
        void onStatusUpdate(PlayerStatusResponse statusResponse);

        /**
         * Called when an error occurs during the update process.
         *
         * @param error the encountered error
         */
        void onError(Throwable error);
    }
}
