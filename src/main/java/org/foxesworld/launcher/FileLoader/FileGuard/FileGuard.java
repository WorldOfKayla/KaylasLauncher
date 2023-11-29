package org.foxesworld.launcher.FileLoader.FileGuard;

import org.apache.logging.log4j.Logger;
import org.foxesworld.engine.game.GameLauncher;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class FileGuard {
    private FileGuardListener fileGuardListener;
    private final List<String> checkList;
    private final GameLauncher gameLauncher;
    private final Logger logger;
    private int totalFiles = 0;
    private int checkedFiles = 0;
    private int filesDeleted = 0;

    public FileGuard(GameLauncher gameLauncher) {
        this.gameLauncher = gameLauncher;
        //Directories we check
        this.checkList = Arrays.asList(
                gameLauncher.buildClientDir(),
                gameLauncher.buildVersionDir(),
                gameLauncher.buildLibrariesPath(),
                gameLauncher.buildNativesPath()
        );

        this.logger = this.gameLauncher.getLogger();
    }

    public void scanAndDeleteFilesInSubdirectories(Set<String> filesToKeep) {
        totalFiles = countTotalFiles();
        checkedFiles = 0;
        filesDeleted = 0;

        for (String dir : checkList) {
            logger.debug("Checking Dir " + dir);
            scanAndDeleteFilesRecursively(new File(dir), filesToKeep);
        }

        fileGuardListener.onFilesChecked(filesDeleted);
    }

    private int countTotalFiles() {
        int total = 0;
        for (String dir : checkList) {
            File directory = new File(dir);
            if (directory.exists()) {
                total += countFilesInDirectory(directory);
            }
        }
        return total;
    }

    private int countFilesInDirectory(File directory) {
        File[] files = directory.listFiles();
        int count = 0;

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    count++;
                } else if (file.isDirectory()) {
                    count += countFilesInDirectory(file);
                }
            }
        }

        return count;
    }

    private void scanAndDeleteFilesRecursively(File directory, Set<String> filesToKeep) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                fileGuardListener.onFileCheck(file);
                if (file.isFile()) {
                    String checkPath = file.getPath().replace(this.gameLauncher.buildGameDir(), "");
                    checkPath = checkPath.replace("\\", "/");
                    if (!filesToKeep.contains(checkPath) && !this.isUserConfig(file)) {
                        // Removing unlisted file
                        boolean deleted = file.delete();
                        if (deleted) {
                            logger.debug("Deleted unlisted file: " + checkPath);
                            filesDeleted++;
                        } else {
                            logger.error("Failed to delete invalid file: " + checkPath);
                        }
                    } else {
                        logger.debug(checkPath + " is checked");
                    }
                    checkedFiles++;
                } else if (file.isDirectory()) {
                    scanAndDeleteFilesRecursively(file, filesToKeep);
                }
            }
        } else {
            logger.error(directory + " is not found!");
        }
    }

    private boolean isUserConfig(File file) {
        return file.getName().endsWith(".txt");
    }

    public void setFileGuardListener(FileGuardListener fileGuardListener) {
        this.fileGuardListener = fileGuardListener;
    }
}
