package org.foxesworld.launcher.FileLoader;

public interface FileLoaderListener {

    void onFilesLoaded();
    void onNewFileFound(FilesAttributes file, String localPath, final long totalSizeFinal);
}
