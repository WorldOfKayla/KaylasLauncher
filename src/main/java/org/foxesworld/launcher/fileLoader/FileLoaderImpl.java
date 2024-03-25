package org.foxesworld.launcher.fileLoader;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.fileLoader.FileAttributes;
import org.foxesworld.engine.fileLoader.FileLoaderListener;
import org.foxesworld.engine.fileLoader.fileGuard.FileGuard;
import org.foxesworld.engine.utils.helper.JVMHelper;
import org.foxesworld.launcher.Core;
import org.foxesworld.launcher.fileLoader.fileGuard.FileGuardImpl;
import org.foxesworld.launcher.game.GameLauncher;

import java.io.File;

public class FileLoaderImpl implements FileLoaderListener {

    private final Core core;
    public FileLoaderImpl(Core core){
        this.core = core;
    }

    @Override
    public void onFilesRead() {
        Engine.getLOGGER().debug("--==|Files are read|==--");
       core.setGameLauncher(new GameLauncher(core.getActionHandler()));
        core.getGameLauncher().setGameListener(core);

        if (JVMHelper.getJavaVersion(core.getGameLauncher().getJreBin()) == null) {
            //If we failed java -version command -> download it
            core.getFileLoader().setReplaceMask("/uploads/files/");
            core.getFileLoader().addFileToDownload(core.getFileLoader().addJreToLoad(core.getGameLauncher().getCurrentJre()));
        }
        core.getFileLoader().downloadFiles();
    }
    @Override
    public void onFilesLoaded() {
        Engine.getLOGGER().debug("--==|Files loaded|==--");
        core.setFileGuard(new FileGuard(core.getGameLauncher()));
        core.getFileGuard().setFileGuardListener(new FileGuardImpl(core));
        core.getFileGuard().addIgnoreDirs(core.getActionHandler().getCurrentServer().getIgnoreDirs());
        core.getFileGuard().scanAndDeleteFilesInSubdirectories(core.getFileLoader().getFilesToKeep());
        core.getFileGuard().recursiveDelete(new File(core.getGameLauncher().buildGameDir() + "/assets/skins"));
    }

    @Override
    public void onNewFileFound(FileAttributes file, String localPath, final long totalSizeFinal) {
        String fullPath = core.getFileLoader().getHomeDir() + localPath;
        core.getActionHandler().getEngine().getGuiBuilder().setLabelText("downloadFile", new File(localPath).getName());
        core.getActionHandler().getEngine().getGuiBuilder().setLabelText("downloadDirectory", String.valueOf(new File(localPath).getParentFile()));

        if (core.getFileLoader().isInvalidFile(new File(fullPath), file.getHash(), file.getSize())) {
            core.getFileLoader().getDownloadUtils().downloader(file.getFilename().replace(" ", "%20"), fullPath, totalSizeFinal);
        }

        if (fullPath.contains("runtime") && fullPath.contains("zip")) {
            core.getFileLoader().getDownloadUtils().unpack(fullPath, new File(fullPath).getParentFile());
        }
    }
}
