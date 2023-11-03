package org.foxesworld.launcher.FileLoader;

import org.apache.logging.log4j.Logger;
import org.foxesworld.engine.gui.components.game.GameLauncher;

import java.io.File;
import java.util.Set;

public class FileLoaderScanner {

    private final String downloadDir;
    private final GameLauncher gameLauncher;
    private  final Logger logger;
    public  FileLoaderScanner(GameLauncher gameLauncher){
        this.gameLauncher = gameLauncher;
        this.downloadDir = gameLauncher.buildClientDir();
        this.logger = this.gameLauncher.getActionHandler().getEngine().getLOGGER();
    }

    public void scanAndDeleteFilesInSubdirectories(Set<String> filesToKeep) {
        scanAndDeleteFilesRecursively(new File(downloadDir), filesToKeep);
    }

    private void scanAndDeleteFilesRecursively(File directory, Set<String> filesToKeep) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String filePath = file.getAbsolutePath();
                    String relativePath = filePath.replace(this.downloadDir, "");
                    String checkPath = "clients" + File.separator + this.gameLauncher.getSelectedServer().serverName + relativePath;
                    checkPath = checkPath.replace("\\", "/");
                    if (!filesToKeep.contains(checkPath)) {
                        boolean deleted = file.delete();
                        if (deleted) {
                           logger.debug("Deleted unlisted file: " + checkPath);
                        } else {
                            logger.debug("Failed to delete invalid file: " + checkPath);
                        }
                    }
                } else if (file.isDirectory()) {
                    scanAndDeleteFilesRecursively(file, filesToKeep);
                }
            }
        }
    }

}
