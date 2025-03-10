package org.foxesworld.launcher;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.fileLoader.FileLoader;
import org.foxesworld.engine.fileLoader.fileGuard.FileGuard;
import org.foxesworld.engine.game.GameListener;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.helper.JVMHelper;
import org.foxesworld.launcher.fileLoader.FileLoaderImpl;
import org.foxesworld.launcher.game.GameLauncher;
import org.foxesworld.launcher.game.GameTimeTask;
import org.foxesworld.launcher.gui.ActionHandler;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import static org.foxesworld.engine.utils.helper.JVMHelper.OS_TYPE;
import static org.foxesworld.engine.utils.helper.JVMHelper.getCorrectOSArch;

public class Core implements GameListener {
    private long startTime;
    private final Launcher launcher;
    private FileGuard fileGuard;
    private final ActionHandler actionHandler;
    private final FileLoader fileLoader;
    private GameLauncher gameLauncher;
    private boolean forceUpdate = false;
    long timeElapsed;

    private final GameTimeTask gameTimeTask;

    public Core(ActionHandler actionHandler, boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
        ServerAttributes currentServer = actionHandler.getCurrentServer();
        actionHandler.getEngine().getDiscord().setSmallImageText(currentServer.getServerDescription());
        actionHandler.getEngine().getDiscord().discordRpcStart(
                actionHandler.getEngine().getLANG().getStringWithKey("game.login",
                        new String[]{"login"},
                        new String[]{actionHandler.getLauncher().getUser().getLogin()}),
                actionHandler.getEngine().getLANG().getStringWithKey("game.playing",
                        new String[]{"server"},
                        new String[]{currentServer.getServerName() + ' ' + currentServer.getServerVersion()}),
                currentServer.getServerName().toLowerCase(Locale.ROOT)
        );
        this.actionHandler = actionHandler;
        this.launcher = actionHandler.getLauncher();
        fileLoader = new FileLoader(actionHandler, actionHandler.getLauncher().getConfig().getHomeDir());
        FileLoaderImpl fileLoaderImpl = new FileLoaderImpl(this);
        fileLoaderImpl.setReplaceMasks(actionHandler.getEngine().getEngineData().getDownloadManager().getReplaceMasks());
        fileLoader.setLoaderListener(fileLoaderImpl);

        this.launcher.getExecutorServiceProvider().submitTask(() -> fileLoader.getFilesToDownload(forceUpdate), "downloadFiles");

        this.gameTimeTask = new GameTimeTask(
                currentServer,
                this.launcher.getUser().getLogin(),
                this.launcher.getExecutorServiceProvider().getExecutorService(),
                this.launcher
        );
    }


    @Override
    public void onGameStart(ServerAttributes serverAttributes) {
        // Скрываем окно лаунчера и воспроизводим звук старта
        this.launcher.getFrame().setVisible(false);
        this.getActionHandler().getLauncher().getSOUND().playSound("other", "start");

        Engine.LOGGER.info("=== GAME CLIENT " + serverAttributes.getServerName()
                + " STARTED by " + this.launcher.getUser().getLogin() + " ===");

        // Если отображается экран загрузки, скрываем его
        if (getLauncher().getLoadingManager().isVisible()) {
            getLauncher().getLoadingManager().toggleVisibility();
        }

        // Запускаем задачу учёта времени (отправка "startedPlaying" выполняется внутри start())
        gameTimeTask.start();
    }

    @Override
    public void onGameExit(ServerAttributes serverAttributes) {
        // Завершаем сессию. Метод finishPlaying() вычисляет итоговое время сессии и отправляет запрос "donePlaying".
        gameTimeTask.finishPlaying();
        Engine.LOGGER.info("Сессия игры завершена для пользователя " + this.launcher.getUser().getLogin());

        // Если требуется перезапуск приложения, можно выполнить соответствующую логику:
        if (this.actionHandler.getLauncher().getConfig().isLaunchAC()) {
            File appDirectory = new File(this.actionHandler.getEngine().appPath());
            if (!appDirectory.isDirectory() || this.launcher.appPath().equals("IDE")) {
                this.launcher.getExecutorServiceProvider().shutdown();
                this.actionHandler.getLauncher().restartApplication(
                        2048,
                        this.actionHandler.getLauncher().getEngineData().getProgramRuntime()
                                + '-' + getOSPrefix() + "-x" + getCorrectOSArch()
                );
            } else {
                Engine.getLOGGER().error("Launcher can't be a directory!");
            }
        }
        this.launcher.getExecutorServiceProvider().shutdown();
        System.exit(0);
    }


    public static String getOSPrefix() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "win";
        } else if (osName.contains("mac")) {
            return "mac";
        } else {
            return "unix";
        }
    }

    public ActionHandler getActionHandler() {
        return actionHandler;
    }

    public GameLauncher getGameLauncher() {
        return gameLauncher;
    }

    public FileLoader getFileLoader() {
        return fileLoader;
    }

    public void setFileGuard(FileGuard fileGuard) {
        this.fileGuard = fileGuard;
    }

    public FileGuard getFileGuard() {
        return fileGuard;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public void setGameLauncher(GameLauncher gameLauncher) {
        this.gameLauncher = gameLauncher;
    }
}