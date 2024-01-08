package org.foxesworld.launcher.FileLoader.FileGuard;

import org.apache.logging.log4j.Logger;
import org.foxesworld.engine.game.GameLauncher;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileGuard {
    private FileGuardListener fileGuardListener;
    private final List<String> checkList;
    private final Set<String> ignoreList;
    private final String[] basicIgnoreDirs = {"saves", "resourcepacks", "shaderpacks", "logs", "config"};
    private final GameLauncher gameLauncher;
    private final Logger logger;

    private int totalFiles = 0;
    private int checkedFiles = 0;
    private int filesDeleted = 0;

    public FileGuard(GameLauncher gameLauncher) {
        this.gameLauncher = gameLauncher;
        // Directories we check
        this.checkList = Arrays.asList(
                gameLauncher.buildClientDir(),
                gameLauncher.buildVersionDir(),
                gameLauncher.buildLibrariesPath(),
                gameLauncher.buildNativesPath()
        );

        this.ignoreList = new HashSet<>();
        this.buildBasicIgnoreList();
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
            //this.gameLauncher.getEngine().getFrame().getLoadingManager().startLoading();
            for (File file : files) {
                if (file.isFile()) {
                    String checkPath = file.getPath().replace(this.gameLauncher.buildGameDir(), "");
                    checkPath = checkPath.replace("\\", "/");
                    // Skip deletion if the file or its parent directory is in the ignoreList
                    if (!filesToKeep.contains(checkPath) && !this.isUserConfig(file) && !isInIgnoreList(file)) {
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
                    // Recursively scan and delete files in subdirectories
                    scanAndDeleteFilesRecursively(file, filesToKeep);
                }
                this.gameLauncher.getEngine().getLoadingManager().setLoadingText(file.getName(), "checkingFiles", 1);
            }
        } else {
            logger.error(directory + " is not found!");
        }
    }

    private boolean isInIgnoreList(File file) {
        String filePath = file.getPath().replace(this.gameLauncher.buildGameDir(), "").replace("\\", "/");
        for(String mask: this.ignoreList){
            if(filePath.startsWith(mask.replace("\\", "/"))){
                return true;
            }
        }
        return  false;
    }
    private void buildBasicIgnoreList(){
        for(String dir: this.basicIgnoreDirs){
            String thisDir = gameLauncher.buildClientDir().replace(gameLauncher.buildGameDir(), "") + File.separator + dir;
            this.ignoreList.add(thisDir);
        }
    }

    public void addIgnoreDirs(String dirs){
        if(dirs != null) {
            for (String dir : dirs.split(",")) {
                String thisDir = gameLauncher.buildClientDir().replace(gameLauncher.buildGameDir(), "") + File.separator + dir;
                this.ignoreList.add(thisDir);
            }
        }
    }

    public void recursiveDelete(File file) {
        try {
            if (!file.exists())
                return;
            if (file.isDirectory()) {
                for (File f : file.listFiles())
                    recursiveDelete(f);
                file.delete();
            } else
                file.delete();
        } catch (Exception ignored) {
        }
    }

    private boolean isUserConfig(File file) {
        return file.getName().endsWith(".txt");
    }

    public void setFileGuardListener(FileGuardListener fileGuardListener) {
        this.fileGuardListener = fileGuardListener;
    }
}
