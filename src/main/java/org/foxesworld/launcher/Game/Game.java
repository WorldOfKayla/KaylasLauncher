package org.foxesworld.launcher.Game;

import org.foxesworld.launcher.action.ActionHandler;
import org.foxesworld.engine.game.GameLauncher;
import org.foxesworld.engine.game.GameListener;
import org.foxesworld.launcher.FileLoader.FileGuard.FileGuardListener;
import org.foxesworld.launcher.FileLoader.FileLoader;
import org.foxesworld.launcher.FileLoader.FileLoaderListener;
import org.foxesworld.launcher.FileLoader.FileGuard.FileGuard;
import org.foxesworld.launcher.FileLoader.FilesAttributes;
import org.foxesworld.launcher.Server.ServerAttributes;

import java.io.File;

public class Game implements FileLoaderListener, FileGuardListener, GameListener {

    private final ActionHandler actionHandler;
    //private List<FilesAttributes> filesAttributes;
    private final FileLoader fileLoader;
    private GameLauncher gameLauncher;

    public Game(ActionHandler actionHandler) {
        actionHandler.getEngine().getDiscord().discordRpcStart(actionHandler.getEngine().getLANG().getString("game.login") + actionHandler.getEngine().getUser().getLogin(), actionHandler.getEngine().getLANG().getString("game.playing") + actionHandler.getCurrentServer().getServerName(), "aiden");
        this.actionHandler = actionHandler;
        fileLoader = new FileLoader(actionHandler);
        fileLoader.setLoaderListener(this);
        Thread downloadThread = new Thread(fileLoader::getFilesToDownload);
        downloadThread.start();
    }
    private boolean hasJre(String version) {
        return new File(gameLauncher.buildRuntimeDir() + File.separator + version).exists();
    }
    @Override
    public void onFilesRead() {
        this.actionHandler.getEngine().getLOGGER().debug("--==|Files are read|==--");
        gameLauncher = new GameLauncher(actionHandler);
        gameLauncher.setGameListener(this);
        if (!this.hasJre(gameLauncher.getCurrentJre())) {
            //If we don't have JRE download it
            fileLoader.addFileToDownload(this.fileLoader.addJreToLoad(gameLauncher.getCurrentJre()));
        }
        this.fileLoader.downloadFiles();
    }
    @Override
    public void onFilesLoaded() {
        this.actionHandler.getEngine().getLOGGER().debug("--==|Files loaded|==--");
        FileGuard fileGuard = new FileGuard(this.gameLauncher);
        fileGuard.setFileGuardListener(this);
        fileGuard.addIgnoreDirs(this.actionHandler.getCurrentServer().getIgnoreDirs());
        fileGuard.scanAndDeleteFilesInSubdirectories(this.fileLoader.getFilesToKeep());
    }
    @Override
    public void onNewFileFound(FilesAttributes file, String localPath, final long totalSizeFinal) {
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
        this.actionHandler.getEngine().getLOGGER().debug("--==|Files checked|==--");
        this.actionHandler.getEngine().getLOGGER().info(filesDeleted + " removed");
        this.actionHandler.getEngine().getSOUND().stopAllSounds();
        gameLauncher.launchGame();
    }
    @Override
    public void onGameStart(ServerAttributes serverAttributes) {
        System.out.println("=== GAME " + serverAttributes.getServerName() + " STARTED ===");
    }
    @Override
    public void onGameExit(int exitCode) {
        System.exit(0);
    }
}
