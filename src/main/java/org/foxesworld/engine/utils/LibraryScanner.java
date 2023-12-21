package org.foxesworld.engine.utils;

import org.foxesworld.engine.Engine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class LibraryScanner {
    private final Engine engine;
    public LibraryScanner(Engine engine) {
        this.engine = engine;
    }
    public List<String> findLibraryPaths(String librariesDirPath) {
        List<String> libraryPaths = new ArrayList<>();
        File librariesDir = new File(librariesDirPath);

        if (librariesDir.exists() && librariesDir.isDirectory()) {
            scanForJARs(librariesDir, libraryPaths);
        } else {
            engine.getLOGGER().error("Libraries directory does not exist or is not a directory.");
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
