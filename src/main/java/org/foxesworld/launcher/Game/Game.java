package org.foxesworld.launcher.Game;

import org.foxesworld.engine.action.ActionHandler;
import org.foxesworld.engine.gui.components.game.GameLauncher;
import org.foxesworld.launcher.FileLoader.FileGuard.FileGuardListener;
import org.foxesworld.launcher.FileLoader.FileLoader;
import org.foxesworld.launcher.FileLoader.FileLoaderListener;
import org.foxesworld.launcher.FileLoader.FileGuard.FileGuard;
import org.foxesworld.launcher.FileLoader.FilesArray;

import java.io.File;
import java.util.List;
import java.util.Random;

public class Game implements FileLoaderListener, FileGuardListener {

    private final ActionHandler actionHandler;
    private final List<FilesArray> filesArray;
    private final FileLoader fileLoader;
    private GameLauncher gameLauncher;

    public Game(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
        fileLoader = new FileLoader(actionHandler);
        fileLoader.setLoaderListener(this);
        this.filesArray = fileLoader.getFilesToDownload(actionHandler.getCurrentServer().getServerVersion(), actionHandler.getCurrentServer().getServerName());
    }

    public void start() {
        this.actionHandler.getEngine().getDiscord().discordRpcStart(this.actionHandler.getEngine().getLANG().getString("game.login") + this.actionHandler.getEngine().getUser().getLogin(), this.actionHandler.getEngine().getLANG().getString("game.playing") + actionHandler.getCurrentServer().getServerName(), "aiden");
        gameLauncher = new GameLauncher(actionHandler);
        if (!this.hasJre(gameLauncher.getCurrentJre())) {
            //If we don't have JRE download it the first
            filesArray.add(this.fileLoader.addJreToLoad(gameLauncher.getCurrentJre()));
        }
        //if(filesArray.size() == 0){
        //    gameLauncher.launchGame();
        //} else {
        this.fileLoader.downloadFiles(filesArray);
        //}
    }

    private boolean hasJre(String version) {
        return new File(gameLauncher.buildRuntimeDir() + File.separator + version).exists();
    }

    @Override
    public void onFilesLoaded() {
        this.actionHandler.getEngine().getLOGGER().debug("--==|Files loaded|==--");
        FileGuard fileGuard = new FileGuard(this.gameLauncher);
        fileGuard.setFileGuardListener(this);
        fileGuard.scanAndDeleteFilesInSubdirectories(this.fileLoader.getFilesToKeep());
    }

    @Override
    public void onNewFileFound(FilesArray file, String localPath, final long totalSizeFinal) {
        String fullPath = this.fileLoader.getHomeDir() + localPath;
        this.actionHandler.getEngine().getGuiBuilder().setLabelText("downloadFile", new File(localPath).getName());
        this.actionHandler.getEngine().getGuiBuilder().setLabelText("downloadDirectory", String.valueOf(new File(localPath).getParentFile()));

        if (fileLoader.isInvalidFile(new File(fullPath), file.hash, file.size)) {
            this.fileLoader.getDownloadUtils().downloader(file.filename, fullPath, totalSizeFinal);
        }

        if (fullPath.contains("runtime") && fullPath.contains("zip")) {
            this.fileLoader.getDownloadUtils().unpack(fullPath, new File(fullPath).getParentFile());
        }
    }

    @Override
    public void onFileCheck(File file) {

    }

    @Override
    public void onFilesChecked(int filesDeleted) {
        this.actionHandler.getEngine().getLOGGER().debug("--==|Files checked|==--");
        this.actionHandler.getEngine().getLOGGER().debug(filesDeleted + " removed");
        this.actionHandler.getEngine().getSOUND().playSound("start/start" + new Random().nextInt(4) + ".ogg");
        gameLauncher.launchGame();
    }
}
