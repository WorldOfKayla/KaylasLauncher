package org.takesome.launcher;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.fileLoader.fileGuard.FileGuard;
import org.takesome.kaylasEngine.game.GameListener;
import org.takesome.kaylasEngine.server.ServerAttributes;
import org.takesome.launcher.fileLoader.FileLoaderImpl;
import org.takesome.launcher.fileLoader.LauncherFileLoader;
import org.takesome.launcher.game.GameLauncher;
import org.takesome.launcher.game.GameTimeTask;
import org.takesome.launcher.gui.ActionHandler;

import javax.swing.SwingUtilities;
import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.takesome.kaylasEngine.utils.helper.JVMHelper.getCorrectOSArch;

public class Core implements GameListener {
    private final Launcher launcher;
    private final ActionHandler actionHandler;
    private final LauncherFileLoader fileLoader;
    private final GameTimeTask gameTimeTask;
    private final AtomicBoolean sessionFinished = new AtomicBoolean(false);

    private FileGuard fileGuard;
    private GameLauncher gameLauncher;

    public Core(ActionHandler actionHandler, boolean forceUpdate) {
        this.actionHandler = Objects.requireNonNull(actionHandler, "actionHandler");
        this.launcher = this.actionHandler.getLauncher();

        ServerAttributes currentServer = Objects.requireNonNull(this.actionHandler.getCurrentServer(), "currentServer");
        bindDiscordPresence(currentServer);

        this.fileLoader = new LauncherFileLoader(this.actionHandler, this.launcher.getConfig().getHomeDir());
        FileLoaderImpl fileLoaderImpl = new FileLoaderImpl(this);
        fileLoaderImpl.setReplaceMasks(this.actionHandler.getEngine().getEngineData().getDownloadManager().getReplaceMasks());
        this.fileLoader.setLoaderListener(fileLoaderImpl);

        this.launcher.getExecutorServiceProvider().submitTask(() -> fileLoader.getFilesToDownload(forceUpdate), "downloadFiles");

        this.gameTimeTask = new GameTimeTask(
                currentServer,
                this.launcher.getUser().getLogin(),
                this.launcher.getExecutorServiceProvider().getExecutorService(),
                this.launcher
        );
    }

    private void bindDiscordPresence(ServerAttributes currentServer) {
        this.actionHandler.getEngine().getDiscord().setSmallImageText(currentServer.getServerDescription());
        this.actionHandler.getEngine().getDiscord().discordRpcStart(
                this.actionHandler.getEngine().getLANG().getStringWithKey("game.login",
                        new String[]{"login"},
                        new String[]{this.launcher.getUser().getLogin()}),
                this.actionHandler.getEngine().getLANG().getStringWithKey("game.playing",
                        new String[]{"server"},
                        new String[]{safe(currentServer.getServerName()) + ' ' + safe(currentServer.getServerVersion())}),
                safe(currentServer.getServerName()).toLowerCase(Locale.ROOT)
        );
    }

    @Override
    public void onGameStart(ServerAttributes serverAttributes) {
        SwingUtilities.invokeLater(() -> {
            this.launcher.getFrame().setVisible(false);
            this.getActionHandler().getLauncher().getSOUND().playSound("other", "start");
            if (getLauncher().getLoadingManager().isVisible()) {
                getLauncher().getLoadingManager().toggleVisibility();
            }
        });

        Engine.LOGGER.info("=== GAME CLIENT {} STARTED by {} ===",
                safe(serverAttributes.getServerName()),
                this.launcher.getUser().getLogin());
        logGamePaths();
        gameTimeTask.start();
    }

    private void logGamePaths() {
        if (this.gameLauncher == null) {
            return;
        }
        try {
            Path assetsPath = this.gameLauncher.getPathBuilders().buildAssetsPath();
            Path librariesPath = this.gameLauncher.getPathBuilders().buildLibrariesPath();
            Path clientDir = this.gameLauncher.getPathBuilders().buildClientDir();
            Engine.LOGGER.debug("Game paths ready: assets={}, libraries={}, clientDir={}", assetsPath, librariesPath, clientDir);
        } catch (RuntimeException error) {
            Engine.LOGGER.warn("Unable to resolve game paths for diagnostics: {}", error.getMessage(), error);
        }
    }

    @Override
    public void onGameExit(ServerAttributes serverAttributes) {
        finishGameSession(serverAttributes, 0);
    }

    @Override
    public void onGameFailed(ServerAttributes serverAttributes, Throwable throwable, int exitCode) {
        int normalizedExitCode = exitCode <= 0 ? 1 : exitCode;
        Engine.LOGGER.error("=== GAME CLIENT {} FAILED by {} exitCode={} ===",
                serverAttributes == null ? "unknown" : safe(serverAttributes.getServerName()),
                this.launcher.getUser().getLogin(),
                normalizedExitCode,
                throwable);
        finishGameSession(serverAttributes, normalizedExitCode);
    }

    private void finishGameSession(ServerAttributes serverAttributes, int exitCode) {
        if (!sessionFinished.compareAndSet(false, true)) {
            Engine.LOGGER.debug("Game session finish already processed for {}.",
                    serverAttributes == null ? "unknown" : safe(serverAttributes.getServerName()));
            return;
        }

        try {
            gameTimeTask.finishPlaying();
        } catch (RuntimeException error) {
            Engine.LOGGER.warn("Unable to finish play-time task cleanly: {}", error.getMessage(), error);
        }

        if (exitCode == 0 && this.actionHandler.getLauncher().getConfig().isLaunchAC()) {
            restartLauncherRuntimeIfNeeded();
        }

        this.launcher.getExecutorServiceProvider().shutdown();
        System.exit(exitCode);
    }

    private void restartLauncherRuntimeIfNeeded() {
        File appDirectory = new File(this.actionHandler.getEngine().appPath());
        if (!appDirectory.isDirectory() || this.launcher.appPath().equals("IDE")) {
            this.actionHandler.getLauncher().restartApplication(
                    2048,
                    this.actionHandler.getLauncher().getEngineData().getProgramRuntime() + "-x" + getCorrectOSArch()
            );
        } else {
            Engine.getLOGGER().error("Launcher can't be a directory!");
        }
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

    public LauncherFileLoader getFileLoader() {
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
