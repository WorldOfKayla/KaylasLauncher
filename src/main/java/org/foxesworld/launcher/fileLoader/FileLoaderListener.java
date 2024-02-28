package org.foxesworld.launcher.fileLoader;

public interface FileLoaderListener {

    void onFilesRead();
    void onFilesLoaded();
    void onNewFileFound(FilesAttributes file, String localPath, final long totalSizeFinal);
}
