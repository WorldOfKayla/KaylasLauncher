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
import org.foxesworld.launcher.gui.ActionHandler;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;

import static org.foxesworld.engine.utils.helper.JVMHelper.OS_TYPE;

public class Core implements GameListener {
    private long startTime;
    private final Launcher launcher;
    private FileGuard fileGuard;
    private final ActionHandler actionHandler;
    private final FileLoader fileLoader;
    private GameLauncher gameLauncher;

    public Core(ActionHandler actionHandler, boolean forceUpdate) {
        ServerAttributes currentServer = actionHandler.getCurrentServer();
        actionHandler.getEngine().getDiscord().setSmallImageText(actionHandler.getCurrentServer().getServerDescription());
        actionHandler.getEngine().getDiscord().discordRpcStart(
                actionHandler.getEngine().getLANG().getStringWithKey("game.login", new String[]{"login"}, new String[]{actionHandler.getLauncher().getUser().getLogin()}),
                actionHandler.getEngine().getLANG().getStringWithKey("game.playing", new String[]{"server"}, new String[]{currentServer.getServerName() + ' ' + currentServer.getServerVersion()}),
                currentServer.getServerName().toLowerCase(Locale.ROOT)
        );
        this.actionHandler = actionHandler;
        this.launcher = actionHandler.getLauncher();
        fileLoader = new FileLoader(actionHandler, actionHandler.getLauncher().getConfig().getHomeDir());
        FileLoaderImpl fileLoaderImpl = new FileLoaderImpl(this);
        fileLoaderImpl.setReplaceMasks(actionHandler.getEngine().getEngineData().getDownloadManager().getReplaceMasks());
        fileLoader.setLoaderListener(fileLoaderImpl);

        this.launcher.getExecutorServiceProvider().submitTask(() -> {fileLoader.getFilesToDownload(forceUpdate);}, "downloadFiles");
        //Thread downloadThread = new Thread(() -> fileLoader.getFilesToDownload(forceUpdate));
        //downloadThread.start();
    }

    @Override
    public void onGameStart(ServerAttributes serverAttributes) {
        this.getActionHandler().getLauncher().getSOUND().playSound("other", "start");
        System.out.println("=== GAME CLIENT " + serverAttributes.getServerName() + " STARTED by " + this.gameLauncher.launcher.getUser().getLogin() + " ===");
        startTime = System.currentTimeMillis();
        if (getLauncher().getLoadingManager().isVisible()) {
            getLauncher().getLoadingManager().toggleLoader();
        }
        CountDownLatch latch = new CountDownLatch(1);
        writePlayTime(serverAttributes, this.gameLauncher.launcher.getUser().getLogin(), "startedPlaying", 0, latch);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGameExit(ServerAttributes serverAttributes) {

        long timeElapsed = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("Time elapsed: " + timeElapsed + " seconds by " + this.gameLauncher.launcher.getUser().getLogin());
        CountDownLatch latch = new CountDownLatch(1);
        writePlayTime(serverAttributes, this.gameLauncher.launcher.getUser().getLogin(),  "donePlaying", timeElapsed, latch);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (this.actionHandler.getLauncher().getConfig().isLaunchAC()) {
            if (!new File(this.actionHandler.getEngine().appPath()).isDirectory() || this.launcher.appPath().equals("IDE")) {
                this.launcher.getExecutorServiceProvider().shutdown();
                this.actionHandler.getLauncher().restartApplication(2048, this.actionHandler.getLauncher().getEngineData().getProgramRuntime() + "-x" + getCorrectOSArch());
            } else {
                Engine.getLOGGER().error("Launcher can't be a directory!");
            }
        }
        this.launcher.getExecutorServiceProvider().shutdown();
        System.exit(0);
    }

    public void writePlayTime(ServerAttributes serverAttributes, String login, String request, long time, CountDownLatch latch) {
        Runnable task = () -> {
            try {
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("serverName", serverAttributes.getServerName());
                playerData.put("login", login);
                playerData.put("playTime", time);
                playerData.put("sysRequest", request);

                this.launcher.getPOSTrequest().sendAsync(playerData,
                        response -> {
                            System.out.println("Response: " + response);
                            latch.countDown();
                        },
                        error -> {
                            error.printStackTrace();
                            latch.countDown();
                        });
            } catch (Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        };

        try {
            if (!this.launcher.getExecutorServiceProvider().getExecutorService().isShutdown()) {
                this.launcher.getExecutorServiceProvider().submitTask(task, "writePlayTime." + request);
            } else {
                System.err.println("Executor service is shutting down, cannot submit task.");
                latch.countDown();
            }
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
            latch.countDown();
        }
    }
    public static int getCorrectOSArch() {
        if (OS_TYPE == JVMHelper.OS.WIN) {
            return System.getenv("ProgramFiles(x86)") == null ? 32 : 64;
        }
        return System.getProperty("os.arch").contains("64") ? 64 : 32;
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