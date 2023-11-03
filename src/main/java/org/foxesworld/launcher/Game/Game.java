package org.foxesworld.launcher.Game;

import org.foxesworld.engine.action.ActionHandler;
import org.foxesworld.engine.gui.components.game.GameLauncher;
import org.foxesworld.launcher.FileLoader.FileLoader;
import org.foxesworld.launcher.FileLoader.FileLoaderListener;
import org.foxesworld.launcher.FileLoader.FileLoaderScanner;
import org.foxesworld.launcher.FileLoader.FilesArray;

import java.io.File;
import java.util.List;

public class Game implements FileLoaderListener {

    private final ActionHandler actionHandler;
    private final List<FilesArray> filesArray;
    private final FileLoader fileLoader;
    private  GameLauncher gameLauncher;

    public Game(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
        fileLoader = new FileLoader(actionHandler);
        fileLoader.setLoaderListener(this);
        this.filesArray = fileLoader.getFilesToDownload(actionHandler.getCurrentServer().serverVersion, actionHandler.getCurrentServer().serverName);
    }

    public void start(){
        this.actionHandler.getEngine().getDiscord().discordRpcStart(this.actionHandler.getEngine().getLANG().getString("game.login") + this.actionHandler.getEngine().getUser().getLogin(),this.actionHandler.getEngine().getLANG().getString("game.playing")+actionHandler.getCurrentServer().serverName,"aiden");
        gameLauncher = new GameLauncher(actionHandler);
        if(!this.hasJre(gameLauncher.getCurrentJre())) {
            //If we don't have JRE download it the first
            filesArray.add(this.fileLoader.addJreToLoad(gameLauncher.getCurrentJre()));
        }
        if(filesArray.size() == 0 ){
            gameLauncher.launchGame();
        } else {
            this.fileLoader.downloadFiles(filesArray);
        }
    }

    private  boolean hasJre(String version){
        return  new File(gameLauncher.buildRuntimeDir()+ File.separator + version).exists();
    }

    @Override
    public void onFilesLoaded() {
        System.out.println("Files loaded!!!");
        FileLoaderScanner fileLoaderScanner = new FileLoaderScanner(this.gameLauncher);
        fileLoaderScanner.scanAndDeleteFilesInSubdirectories(this.fileLoader.getFilesToKeep());
        gameLauncher.launchGame();
    }

    @Override
    public void onNewFileFound(FilesArray file, String localPath, final long totalSizeFinal) {
        if (!fileLoader.checkFile(new File(localPath), file.hash, file.size)) {
            this.fileLoader.getDownloadUtils().downloader(file.filename, localPath, totalSizeFinal);
        }

        if (localPath.contains(".zip")) {
            this.fileLoader.getDownloadUtils().unpack(localPath, new File(localPath).getParentFile());
        }
    }
}
