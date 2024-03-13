package org.foxesworld.launcher;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.fileLoader.FileAttributes;
import org.foxesworld.engine.fileLoader.FileLoader;
import org.foxesworld.engine.fileLoader.FileLoaderListener;
import org.foxesworld.engine.fileLoader.fileGuard.FileGuard;
import org.foxesworld.engine.fileLoader.fileGuard.FileGuardListener;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.launcher.game.GameLauncher;
import org.foxesworld.launcher.gui.ActionHandler;
import org.foxesworld.engine.game.GameListener;

import java.io.File;

public class Core implements FileLoaderListener, FileGuardListener, GameListener {
    private long startTime, timeElapsed;
    private FileGuard fileGuard;
    private final ActionHandler actionHandler;
    private final FileLoader fileLoader;
    private GameLauncher gameLauncher;

    public Core(ActionHandler actionHandler) {
        actionHandler.getEngine().getDiscord().discordRpcStart(actionHandler.getEngine().getLANG().getString("game.login") + actionHandler.getLauncher().getUser().getLogin(), actionHandler.getEngine().getLANG().getString("game.playing") + actionHandler.getCurrentServer().getServerName(), "aiden");
        this.actionHandler = actionHandler;
        fileLoader = new FileLoader(actionHandler);
        fileLoader.setLoaderListener(this);
		fileLoader.setReplaceMask("/uploads/files/clients/");
        Thread downloadThread = new Thread(fileLoader::getFilesToDownload);
        downloadThread.start();
    }
    private boolean hasJre(String version) {
        return new File(gameLauncher.buildRuntimeDir() + File.separator + version).exists();
    }
    @Override
    public void onFilesRead() {
        Engine.getLOGGER().debug("--==|Files are read|==--");
        gameLauncher = new GameLauncher(actionHandler);
        gameLauncher.setGameListener(this);
        if (!this.hasJre(gameLauncher.getCurrentJre())) {
            //If we don't have JRE download it
			fileLoader.setReplaceMask("/uploads/files/");
            fileLoader.addFileToDownload(this.fileLoader.addJreToLoad(gameLauncher.getCurrentJre()));
        }
        this.fileLoader.downloadFiles();
    }
    @Override
    public void onFilesLoaded() {
        Engine.getLOGGER().debug("--==|Files loaded|==--");
        this.fileGuard = new FileGuard(this.gameLauncher);
        fileGuard.setFileGuardListener(this);
        fileGuard.addIgnoreDirs(this.actionHandler.getCurrentServer().getIgnoreDirs());
        fileGuard.scanAndDeleteFilesInSubdirectories(this.fileLoader.getFilesToKeep());
        fileGuard.recursiveDelete(new File(this.gameLauncher.buildGameDir() + "/assets/skins"));
    }

    @Override
    public void onNewFileFound(FileAttributes file, String localPath, final long totalSizeFinal) {
        String fullPath = this.fileLoader.getHomeDir() + localPath;
        this.actionHandler.getEngine().getGuiBuilder().setLabelText("downloadFile", new File(localPath).getName());
        this.actionHandler.getEngine().getGuiBuilder().setLabelText("downloadDirectory", String.valueOf(new File(localPath).getParentFile()));

        if (fileLoader.isInvalidFile(new File(fullPath), file.getHash(), file.getSize())) {
            this.fileLoader.getDownloadUtils().downloader(file.getFilename().replace(" ", "%20"), fullPath, totalSizeFinal);
        }

        if (fullPath.contains("runtime") && fullPath.contains("zip")) {
            this.fileLoader.getDownloadUtils().unpack(fullPath, new File(fullPath).getParentFile());
        }
    }
    @Override
    public void onFilesChecked(int filesDeleted) {
        Engine.getLOGGER().debug("--==|Files checked|==--");
        Engine.getLOGGER().info(filesDeleted + " removed");
        this.actionHandler.getEngine().getSOUND().getSoundPlayer().stopAllSounds();
        gameLauncher.launchGame();
    }

    @Override
    public void onGameStart(ServerAttributes serverAttributes) {
        System.out.println("=== GAME CLIENT " + serverAttributes.getServerName()+ " STARTED by "+ this.gameLauncher.launcher.getUser().getLogin()+" ===");
        startTime = System.currentTimeMillis();
    }

    @Override
    public void onGameExit(org.foxesworld.engine.game.GameLauncher gameLauncher) {
        timeElapsed = (System.currentTimeMillis() - startTime) / 1000;
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
}
