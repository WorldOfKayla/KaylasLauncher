package org.foxesworld.launcher.FileLoader;

import java.io.File;
import java.util.List;

public class FileLoaderScanner {

    private final String downloadDir;
    public  FileLoaderScanner(String downloadDir){
        this.downloadDir = downloadDir;
    }

    public void scanAndDeleteFilesInSubdirectories(List<FilesArray> filesToDownload) {
        scanAndDeleteFilesRecursively(new File(downloadDir), filesToDownload);
    }

    private void scanAndDeleteFilesRecursively(File directory, List<FilesArray> filesToDownload) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String filePath = file.getAbsolutePath();
                    String relativePath = filePath.replace(this.downloadDir, "");

                    // Check if the file is not in the list of files to download
                    boolean shouldDelete = true;
                    for (FilesArray fileToDownload : filesToDownload) {
                        String localPath = fileToDownload.filename.replace(fileToDownload.getReplaceMask(), "");
                        if (relativePath.equals(localPath)) {
                            shouldDelete = false;
                            break;
                        }
                    }

                    // If the file should be deleted, delete it
                    if (shouldDelete) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            System.out.println("Deleted unlisted file: " + relativePath);
                        } else {
                            System.out.println("Failed to delete invalid file: " + relativePath);
                        }
                    }
                } else if (file.isDirectory()) {
                    // If it's a directory, recursively scan and delete files in it
                    scanAndDeleteFilesRecursively(file, filesToDownload);
                }
            }
        }
    }


}
