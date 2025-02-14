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

public class GameTimeTask {

    private static final int DEFAULT_DELAY_SECONDS = 15;
    private static final Gson gson = new Gson();

    private final int delaySeconds;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService executorService;
    private final HTTPrequest postRequest;
    private final ServerAttributes serverAttributes;
    private final String userLogin;

    // Время начала текущего (или возобновлённого) отсчёта
    private volatile Instant startTime;
    // Накопленное время (без учета текущего периода)
    private volatile Duration accumulatedTime = Duration.ZERO;
    // Флаг активности сессии и паузы
    private volatile boolean running = false;
    private volatile boolean paused = false;
    // Сохраняем последнее значение server startTimestamp для обнаружения новой сессии
    private volatile long lastServerStartTimestamp = 0;

    private ScheduledFuture<?> scheduledFuture;
    private GameTimeListener listener;

    public GameTimeTask(ServerAttributes serverAttributes, String userLogin,
                        ExecutorService executorService, Launcher launcher) {
        this(serverAttributes, userLogin, executorService, launcher, DEFAULT_DELAY_SECONDS);
    }

    public GameTimeTask(ServerAttributes serverAttributes, String userLogin,
                        ExecutorService executorService, Launcher launcher, int delaySeconds) {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.executorService = executorService;
        this.serverAttributes = serverAttributes;
        this.userLogin = userLogin;
        this.postRequest = launcher.getPOSTrequest();
        this.delaySeconds = delaySeconds;
    }

    public void setGameTimeListener(GameTimeListener listener) {
        this.listener = listener;
    }

    /**
     * Запускает сессию отсчёта времени.
     */
    public synchronized void start() {
        if (running) {
            Engine.LOGGER.warn("GameTimeTask уже запущен.");
            return;
        }
        running = true;
        paused = false;
        accumulatedTime = Duration.ZERO;
        startTime = Instant.now();
        // Отправляем запрос о начале сессии
        writePlayTime("startedPlaying", 0);
        // Запускаем периодическую проверку статуса и обновление времени
        scheduleTask();
        Engine.LOGGER.info("GameTimeTask запущен с интервалом " + delaySeconds + " секунд.");
    }

    /**
     * Завершает сессию и отправляет финальное время.
     */
    public synchronized void finishPlaying() {
        if (!running) {
            Engine.LOGGER.warn("GameTimeTask не запущен.");
            return;
        }
        cancelScheduledTask();
        long totalElapsedSeconds = getTotalElapsedSeconds();
        writePlayTime("donePlaying", totalElapsedSeconds);
        running = false;
        Engine.LOGGER.info("Сессия завершена. Итоговое время: " + totalElapsedSeconds + " сек.");
    }

    /**
     * Останавливает задачу и завершает работу планировщика.
     */
    public synchronized void stop() {
        if (!running) {
            Engine.LOGGER.warn("GameTimeTask не запущен.");
            return;
        }
        running = false;
        cancelScheduledTask();
        scheduler.shutdownNow();
        Engine.LOGGER.info("GameTimeTask успешно остановлен.");
    }

    /**
     * Ручное приостановление отсчёта (без остановки планировщика).
     * При вызове автоматически накапливается прошедшее время.
     */
    public synchronized void pause() {
        if (!running || paused) {
            Engine.LOGGER.warn("GameTimeTask уже приостановлен или не запущен.");
            return;
        }
        accumulatedTime = accumulatedTime.plus(Duration.between(startTime, Instant.now()));
        paused = true;
        Engine.LOGGER.info("GameTimeTask приостановлен. Накоплено времени: " + accumulatedTime.getSeconds() + " сек.");
    }

    /**
     * Ручное возобновление отсчёта (без перезапуска планировщика).
     */
    public synchronized void resume() {
        if (!running || !paused) {
            Engine.LOGGER.warn("GameTimeTask не находится в состоянии паузы или не запущен.");
            return;
        }
        startTime = Instant.now();
        paused = false;
        Engine.LOGGER.info("GameTimeTask возобновлён.");
    }

    /**
     * Планирует периодическую задачу обновления времени.
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
     * Возвращает общее накопленное время в секундах.
     * Если задача на паузе, возвращается накопленное время,
     * иначе добавляется время с момента последнего старта.
     */
    private long getTotalElapsedSeconds() {
        if (paused) {
            return accumulatedTime.getSeconds();
        } else {
            return accumulatedTime.plus(Duration.between(startTime, Instant.now())).getSeconds();
        }
    }

    /**
     * Периодически проверяет статус игрока.
     * Если игрок оффлайн — ставит отсчёт на паузу,
     * если игрок возвращается — возобновляет отсчёт.
     * Также, при обнаружении нового server startTimestamp происходит сброс таймера.
     */
    private void updateGameTime() {
        try {
            PlayerStatusResponse statusResponse = getStatus();
            if (statusResponse == null) {
                Engine.LOGGER.warn("Не удалось получить статус игрока.");
                return;
            }

            if (statusResponse.isPlaying()) {
                if (paused) {
                    // Вместо простого resume(), отправляем новый запрос начала сессии
                    Engine.LOGGER.info("Игрок вернулся на сервер. Начинаем новую сессию.");
                    resetTiming(); // Обнуляем локальный таймер
                    // Отправляем запрос, сигнализирующий о новом старте
                    writePlayTime("startedPlaying", 0);
                    lastServerStartTimestamp = statusResponse.getStartTimestamp();
                } else if (statusResponse.getStartTimestamp() != lastServerStartTimestamp) {
                    Engine.LOGGER.info("Обнаружен новый старт сессии (server startTimestamp: "
                            + statusResponse.getStartTimestamp() + "). Таймер сбрасывается.");
                    resetTiming();
                    lastServerStartTimestamp = statusResponse.getStartTimestamp();
                }
                writePlayTime("playing", getTotalElapsedSeconds());
            } else {
                if (!paused) {
                    pause();
                    Engine.LOGGER.info("Игрок покинул сервер. Таймер приостановлен.");
                }
            }
        } catch (Exception e) {
            Engine.LOGGER.error("Ошибка при обновлении времени игры", e);
        }
    }


    /**
     * Сбрасывает таймер (обнуляет накопленное время и устанавливает текущее время старта).
     */
    public synchronized void resetTiming() {
        startTime = Instant.now();
        accumulatedTime = Duration.ZERO;
        Engine.LOGGER.info("Таймер сброшен.");
    }

    /**
     * Формирует данные для отправки.
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
     * Отправляет данные о времени игры асинхронно.
     */
    private void writePlayTime(String action, long elapsedTime) {
        executorService.submit(() -> {
            try {
                Map<String, Object> playerData = buildPlayerData(elapsedTime, action);
                postRequest.sendAsync(playerData,
                        response -> Engine.LOGGER.info("Время игры отправлено: " + response),
                        error -> Engine.LOGGER.error("Ошибка при отправке времени игры", error));
            } catch (Exception e) {
                Engine.LOGGER.error("Непредвиденная ошибка в writePlayTime", e);
            }
        });
    }

    /**
     * Синхронно проверяет статус игрока.
     * Отправляется запрос с параметрами для получения статуса.
     */
    public PlayerStatusResponse getStatus() throws InterruptedException {
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("sysRequest", "checkStatus");
        statusData.put("login", userLogin);
        statusData.put("serverIp", serverAttributes.getHost());
        statusData.put("serverPort", serverAttributes.getPort());

        Engine.LOGGER.debug("Отправка запроса проверки статуса: " + statusData);

        final PlayerStatusResponse[] responseHolder = new PlayerStatusResponse[1];
        CountDownLatch latch = new CountDownLatch(1);

        postRequest.sendAsync(statusData,
                response -> {
                    try {
                        Engine.LOGGER.debug("Получен ответ статуса: " + response);
                        if (response instanceof String) {
                            PlayerStatusResponse statusResponse = gson.fromJson((String) response, PlayerStatusResponse.class);
                            if (statusResponse != null) {
                                Engine.LOGGER.info("Статус игрока: " + statusResponse.getMessage() +
                                        ", isPlaying: " + statusResponse.isPlaying() +
                                        (statusResponse.isPlaying() ? ", startTimestamp: " + statusResponse.getStartTimestamp() : ""));
                                responseHolder[0] = statusResponse;
                            } else {
                                Engine.LOGGER.warn("Парсинг JSON статуса вернул null.");
                            }
                        } else {
                            Engine.LOGGER.warn("Неожиданный тип ответа: " + response.getClass().getName());
                        }
                    } catch (JsonSyntaxException e) {
                        Engine.LOGGER.error("Ошибка парсинга JSON ответа статуса", e);
                    } finally {
                        latch.countDown();
                    }
                },
                error -> {
                    Engine.LOGGER.error("Ошибка проверки статуса игрока", error);
                    latch.countDown();
                }
        );

        latch.await();
        return responseHolder[0];
    }

    /**
     * Интерфейс слушателя для уведомлений о статусе и обновлении времени игры.
     */
    public interface GameTimeListener {
        void onUpdate(GameTimeResponse response);
        void onStatusUpdate(PlayerStatusResponse statusResponse);
        void onError(Throwable error);
    }
}
