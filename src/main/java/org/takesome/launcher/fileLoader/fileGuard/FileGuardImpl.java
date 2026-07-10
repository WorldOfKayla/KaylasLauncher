package org.takesome.launcher.fileLoader.fileGuard;

import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.fileLoader.fileGuard.FileGuardListener;
import org.takesome.launcher.Core;

import java.io.File;

public class FileGuardImpl implements FileGuardListener {
    private final Core core;

    public FileGuardImpl(Core core) {
        this.core = core;
    }

    @Override
    public void onFilesChecked(int filesDeleted) {
        core.getLauncher().getDiscordPresence().showLaunching(
                core.getActionHandler().getCurrentServer(),
                core.getLauncher().getUser().getLogin()
        );
        Engine.getLOGGER().debug("--==|Files checked|==--");
        Engine.getLOGGER().info("{} files removed", filesDeleted);
        core.getActionHandler().getEngine().getSOUND().getSoundPlayer().stopAllSounds();
        core.getGameLauncher().launchGame();
    }

    @Override
    public void onDirCheck(String s) {
        Engine.LOGGER.debug("Checking {}", s);
    }

    @Override
    public void onFileCheck(File file) {
    }
}

