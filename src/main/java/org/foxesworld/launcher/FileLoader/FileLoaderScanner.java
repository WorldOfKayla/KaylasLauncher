package org.foxesworld.launcher.FileLoader;

import org.foxesworld.engine.gui.components.game.GameLauncher;

import java.io.File;
import java.util.Set;

public class FileLoaderScanner {

    private final String downloadDir;
    private final GameLauncher gameLauncher;
    public  FileLoaderScanner(GameLauncher gameLauncher){
        this.gameLauncher = gameLauncher;
        this.downloadDir = gameLauncher.buildClientDir();
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

                    // Проверьте, содержится ли относительный путь в наборе хэш-значений
                    String checkPath = "clients" + File.separator + this.gameLauncher.getSelectedServer().serverName + relativePath;
                    checkPath = checkPath.replace("\\", "/");
                    if (!filesToKeep.contains(checkPath)) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            System.out.println("Deleted unlisted file: " + checkPath);
                        } else {
                            System.out.println("Failed to delete invalid file: " + checkPath);
                        }
                    }
                } else if (file.isDirectory()) {
                    // Если это директория, рекурсивно сканируйте и удаляйте файлы в ней
                    scanAndDeleteFilesRecursively(file, filesToKeep);
                }
            }
        }
    }

}
