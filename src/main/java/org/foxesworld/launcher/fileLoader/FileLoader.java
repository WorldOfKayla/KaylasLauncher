package org.foxesworld.launcher.fileLoader;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.fileLoader.FileAttributes;
import org.foxesworld.engine.fileLoader.FileLoaderListener;
import org.foxesworld.engine.fileLoader.fileGuard.FileGuard;
import org.foxesworld.launcher.Core;
import org.foxesworld.launcher.game.GameLauncher;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileLoader implements FileLoaderListener {

    private final Core core;
    public FileLoader(Core core){
        this.core = core;
    }

    @Override
    public void onFilesRead() {
        Engine.getLOGGER().debug("--==|Files are read|==--");
       core.setGameLauncher(new GameLauncher(core.getActionHandler()));
        core.getGameLauncher().setGameListener(core);
        if (this.getJavaVersion(core.getGameLauncher().getCurrentJre()) == null) {
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
        core.getFileGuard().setFileGuardListener(new org.foxesworld.launcher.fileLoader.fileGuard.FileGuard(core));
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

    public String getJavaVersion(String javaVersion) {
        String javaExecutablePath = this.core.getGameLauncher().buildRuntimeDir() + File.separator +javaVersion + File.separator + "bin" + File.separator + "java";
        String[] command = {javaExecutablePath, "-version"};
        String out = "";
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("java version")) {
                    out = line.split(" ")[2].replace("\"", "");
                }
            }
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out;
    }
}
