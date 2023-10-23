package org.foxesworld.engine.utils.Download;

public interface DownloadListener {
    void onDownloadProgress(int percent);
    void onDownloadComplete();
    void onDownloadError(Exception e);
}
