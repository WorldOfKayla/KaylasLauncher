package org.foxesworld.launcher.fileLoader.fileGuard;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.fileLoader.fileGuard.FileGuardListener;
import org.foxesworld.launcher.Core;

import java.io.File;

public class FileGuard  implements FileGuardListener {

    private final Core core;

    public FileGuard(Core core) {
        this.core = core;
    }

    @Override
    public void onFilesChecked(int filesDeleted) {
        Engine.getLOGGER().debug("--==|Files checked|==--");
        Engine.getLOGGER().info(filesDeleted + " removed");
        core.getActionHandler().getEngine().getSOUND().getSoundPlayer().stopAllSounds();
        core.getGameLauncher().launchGame();
    }

    @Override
    public void onDirChecking(String s) {
        System.out.println(s);
    }

    @Override
    public void onFileCheck(File file) {
    }
}

