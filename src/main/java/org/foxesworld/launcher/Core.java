package org.foxesworld.launcher;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.fileLoader.FileLoader;
import org.foxesworld.engine.fileLoader.fileGuard.FileGuard;
import org.foxesworld.engine.game.GameListener;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.launcher.game.GameLauncher;
import org.foxesworld.launcher.gui.ActionHandler;

import java.io.File;

public class Core implements GameListener {
    private long startTime;
    private FileGuard fileGuard;
    private final ActionHandler actionHandler;
    private final FileLoader fileLoader;
    private GameLauncher gameLauncher;

    public Core(ActionHandler actionHandler) {
        actionHandler.getEngine().getDiscord().discordRpcStart(actionHandler.getEngine().getLANG().getString("game.login") + actionHandler.getLauncher().getUser().getLogin(), actionHandler.getEngine().getLANG().getString("game.playing") + actionHandler.getCurrentServer().getServerName(), "aiden");
        this.actionHandler = actionHandler;
        fileLoader = new FileLoader(actionHandler);
        fileLoader.setLoaderListener(new org.foxesworld.launcher.fileLoader.FileLoader(this));
		fileLoader.setReplaceMask("/uploads/files/clients/");
        Thread downloadThread = new Thread(fileLoader::getFilesToDownload);
        downloadThread.start();
    }
    @Override
    public void onGameStart(ServerAttributes serverAttributes) {
        System.out.println("=== GAME CLIENT " + serverAttributes.getServerName()+ " STARTED by "+ this.gameLauncher.launcher.getUser().getLogin()+" ===");
        startTime = System.currentTimeMillis();
    }

    @Override
    public void onGameExit(org.foxesworld.engine.game.GameLauncher gameLauncher) {
        long timeElapsed = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("Time elapsed: " + timeElapsed + " seconds by " + this.gameLauncher.launcher.getUser().getLogin());
        if(this.actionHandler.getLauncher().getConfig().isLaunchAC()) {
            if(!new File(this.actionHandler.getEngine().appPath()).isDirectory()) {
                this.actionHandler.getLauncher().restartApplication(128);
            } else {
                Engine.getLOGGER().error("Launcher can't be a directory!");
                System.exit(0);
            }
        } else {
            System.exit(0);
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

    public void setGameLauncher(GameLauncher gameLauncher) {
        this.gameLauncher = gameLauncher;
    }
}
