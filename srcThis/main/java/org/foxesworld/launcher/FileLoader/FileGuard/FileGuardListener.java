package org.foxesworld.launcher.FileLoader.FileGuard;

import java.io.File;

public interface FileGuardListener {

    void onFileCheck(File file);
    void  onFilesChecked(int filesDeleted);
}
