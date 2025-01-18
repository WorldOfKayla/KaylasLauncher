package org.foxesworld.launcher.game;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class GameTimeTask {
    private final int delay = 20;
    private long lastElapsedTimeSent = 0;
    private final ScheduledExecutorService scheduler;
    private final ServerAttributes serverAttributes;
    private final String userLogin;
    private final ExecutorService executorService;
    private final HTTPrequest postRequest;
    private long startTime;

    public GameTimeTask(ServerAttributes serverAttributes,
                        String userLogin,
                        ExecutorService executorService,
                        HTTPrequest postRequest) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.serverAttributes = serverAttributes;
        this.userLogin = userLogin;
        this.executorService = executorService;
        this.postRequest = postRequest;
    }

    public void start() {
        startTime = System.currentTimeMillis();

        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = (currentTime - startTime) / 1000;

            writePlayTime("playing", elapsedTime, null);
            lastElapsedTimeSent = elapsedTime;
        }, 0, delay, TimeUnit.SECONDS);
    }

    public void stop() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
            Engine.LOGGER.info("GameTimeTask stopped successfully.");
        }
    }

    public void writePlayTime(String action, long elapsedTime, CountDownLatch latch) {
        Runnable task = () -> {
            System.out.println(elapsedTime);
            try {
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("serverName", serverAttributes.getServerName());
                playerData.put("login", userLogin);
                playerData.put("playTime", elapsedTime);
                playerData.put("sysRequest", action);

                postRequest.sendAsync(playerData,
                        response -> {
                            Engine.LOGGER.info("Response: " + response);
                            if (latch != null) {
                                latch.countDown();
                            }
                        },
                        error -> {
                            Engine.LOGGER.error("Error sending play time", error);
                            if (latch != null) {
                                latch.countDown();
                            }
                        });
            } catch (Exception e) {
                Engine.LOGGER.error("Unexpected error in writePlayTime", e);
                if (latch != null) {
                    latch.countDown();
                }
            }
        };

        try {
            if (!executorService.isShutdown()) {
                executorService.submit(task);
            } else {
                Engine.LOGGER.warn("Executor service is shutting down, cannot submit task.");
                if (latch != null) {
                    latch.countDown();
                }
            }
        } catch (RejectedExecutionException e) {
            Engine.LOGGER.error("Task rejected by executor service", e);
            if (latch != null) {
                latch.countDown();
            }
        }
    }
}
