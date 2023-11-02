package org.foxesworld.launcher.Game;

import org.foxesworld.engine.action.ActionHandler;
import org.foxesworld.engine.gui.components.game.GameLauncher;
import org.foxesworld.launcher.FileLoader.FileLoader;
import org.foxesworld.launcher.FileLoader.FilesArray;

import java.io.File;
import java.util.List;

public class Game {

    private final ActionHandler actionHandler;
    private final List<FilesArray> filesArray;
    private final FileLoader fileLoader;
    private  GameLauncher gameLauncher;

    public Game(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
        fileLoader = new FileLoader(actionHandler);
        this.filesArray = fileLoader.getFilesToDownload(actionHandler.getCurrentServer().serverVersion, actionHandler.getCurrentServer().serverName);
    }

    public void start(){
        gameLauncher = new GameLauncher(actionHandler);
        if(!this.hasJre(gameLauncher.getCurrentJre())) {
            //If we don't have JRE download it the first
            filesArray.add(this.fileLoader.addJreToLoad(gameLauncher.getCurrentJre()));
            this.fileLoader.setDownloadMask("/uploads/files/");
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
}
