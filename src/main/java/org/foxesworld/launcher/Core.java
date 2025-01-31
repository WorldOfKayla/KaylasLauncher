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
                this.launcher.getPOSTrequest()
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdownSequence();
            } catch (Exception e) {
                Engine.LOGGER.error("Error during shutdown sequence: ", e);
            }
        }));
    }

    private void shutdownSequence() {
        CountDownLatch latch = new CountDownLatch(1);
        timeElapsed = (System.currentTimeMillis() - startTime) / 1000;
        gameTimeTask.writePlayTime("donePlaying", timeElapsed, latch);
        gameTimeTask.stop();
        try {
            latch.await();
            Engine.LOGGER.info("Successfully saved playtime: " + timeElapsed + " seconds.");
        } catch (InterruptedException e) {
            Engine.LOGGER.error("Error waiting for playtime write completion: ", e);
        }
    }


    @Override
    public void onGameStart(ServerAttributes serverAttributes) {
        this.getActionHandler().getLauncher().getSOUND().playSound("other", "start");
        Engine.LOGGER.info("=== GAME CLIENT " + serverAttributes.getServerName() + " STARTED by " + this.launcher.getUser().getLogin() + " ===");
        startTime = System.currentTimeMillis();
        if (getLauncher().getLoadingManager().isVisible()) {
            getLauncher().getLoadingManager().toggleVisibility();
        }

        CountDownLatch latch = new CountDownLatch(1);
        gameTimeTask.writePlayTime("startedPlaying", 0, latch);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        gameTimeTask.start();
    }

    @Override
    public void onGameExit(ServerAttributes serverAttributes) {
        timeElapsed = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("Time elapsed: " + timeElapsed + " seconds by " + this.launcher.getUser().getLogin());


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

    public static int getOsArchitecture() {
        if (OS_TYPE == JVMHelper.OS.WIN) {
            String programFiles = System.getenv("ProgramFiles(x86)");
            return programFiles != null ? 32 : 64;
        }
        String osArch = System.getProperty("os.arch");
        return osArch.contains("64") ? 64 : 32;
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
