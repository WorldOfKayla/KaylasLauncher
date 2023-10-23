package org.foxesworld.launcher.Game;

import org.foxesworld.engine.action.ActionHandler;
import org.foxesworld.engine.gui.components.game.GameLauncher;
import org.foxesworld.launcher.FileLoader.FileLoader;
import org.foxesworld.launcher.FileLoader.FilesArray;

import java.util.List;

public class Game {

    private ActionHandler actionHandler;
    private List<FilesArray> filesArray;
    private FileLoader fileLoader;

    public Game(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
        fileLoader = new FileLoader(actionHandler);
        this.filesArray = fileLoader.getFilesToDownload(actionHandler.getCurrentServer().serverVersion, actionHandler.getCurrentServer().serverName);
    }

    public void start(){
        if(filesArray.size() == 0){
            GameLauncher game = new GameLauncher(actionHandler);
            game.launchGame();
        } else {
            this.fileLoader.downloadFiles(filesArray);
        }
    }
}
