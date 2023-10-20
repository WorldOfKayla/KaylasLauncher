package org.foxesworld.engine.gui.components.game;

import org.foxesworld.engine.AppFrame;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LibraryScanner {

    private AppFrame appFrame;
    public LibraryScanner(AppFrame appFrame) {
        this.appFrame = appFrame;
    }
    public List<String> findLibraryPaths(String librariesDirPath) {
        List<String> libraryPaths = new ArrayList<>();
        File librariesDir = new File(librariesDirPath);

        if (librariesDir.exists() && librariesDir.isDirectory()) {
            scanForJARs(librariesDir, libraryPaths);
        } else {
            System.err.println("Libraries directory does not exist or is not a directory.");
        }

        return libraryPaths;
    }

    private void scanForJARs(File directory, List<String> libraryPaths) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanForJARs(file, libraryPaths);
                } else if (file.getName().endsWith(".jar")) {
                    String libraryPath = file.getAbsolutePath();
                    libraryPaths.add(libraryPath);
                }
            }
        }
    }
}
