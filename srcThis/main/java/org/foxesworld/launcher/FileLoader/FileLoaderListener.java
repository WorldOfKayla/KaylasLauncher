package org.foxesworld.launcher.FileLoader;

public interface FileLoaderListener {

    void onFilesLoaded();

    void onNewFileFound(FilesArray file, String localPath, final long totalSizeFinal);
}
