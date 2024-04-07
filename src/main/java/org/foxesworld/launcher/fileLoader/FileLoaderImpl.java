package org.foxesworld.launcher.fileLoader;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.EngineData;
import org.foxesworld.engine.fileLoader.FileAttributes;
import org.foxesworld.engine.fileLoader.FileLoaderListener;
import org.foxesworld.engine.fileLoader.fileGuard.FileGuard;
import org.foxesworld.engine.game.argsReader.ArgsReader;
import org.foxesworld.engine.utils.helper.JVMHelper;
import org.foxesworld.launcher.Core;
import org.foxesworld.launcher.fileLoader.fileGuard.FileGuardImpl;
import org.foxesworld.launcher.game.GameLauncher;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileLoaderImpl implements FileLoaderListener {
    private final Core core;

    public FileLoaderImpl(Core core) {
        this.core = core;
    }

    private final Map<String, String> replaceMasks = new HashMap<>();
    private final Map<String, String> varsToReplace = new HashMap<>();

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

    //Will merge to original
    public void setReplaceMasks(List<EngineData.ReplaceMask> replaceTemplate) {
        addReplaceVars();
        for (EngineData.ReplaceMask mask : replaceTemplate) {
            for (Map.Entry<String, String> vars : this.varsToReplace.entrySet()) {
                String key = replaceVariableValue(vars.getKey(), mask.getMask(), vars.getValue());
                String val = replaceVariableValue(vars.getKey(), mask.getReplace(), vars.getValue());
                replaceMasks.put(key, val);
            }
        }
    }

    //Will merge to original
    private String replaceVariableValue(String variableName, String originalValue, String newValue) {
        Pattern pattern = Pattern.compile("\\$\\{" + variableName + "\\}");
        Matcher matcher = pattern.matcher(originalValue);
        return matcher.replaceAll(newValue);
    }
    //Will merge to original
    private void addReplaceVars() {
        this.varsToReplace.put("version", core.getActionHandler().getCurrentServer().getServerVersion());
    }

    @Override
    public void onFilesLoaded() {
        Engine.getLOGGER().debug("--==|Files loaded|==--");
        this.core.getGameLauncher().setArgsReader(new ArgsReader(core.getGameLauncher()));
        List<String> checkList = Arrays.asList(
                core.getGameLauncher().getPathBuilders().buildClientDir(),
                core.getGameLauncher().getPathBuilders().buildVersionDir(),
                core.getGameLauncher().getPathBuilders().buildLibrariesPath(),
                core.getGameLauncher().getPathBuilders().buildNativesPath(),
                core.getGameLauncher().getPathBuilders().buildAssetsPath()
        );
        core.setFileGuard(new FileGuard(core.getGameLauncher(), checkList));
        core.getFileGuard().setFileGuardListener(new FileGuardImpl(core));
        core.getFileGuard().addIgnoreDirs(core.getActionHandler().getCurrentServer().getIgnoreDirs());
        core.getFileGuard().scanAndDeleteFilesInSubdirectories(core.getFileLoader().getFilesToKeep());
        core.getFileGuard().recursiveDelete(new File(core.getGameLauncher().getPathBuilders().buildGameDir() + "/assets/skins"));
    }

    @Override
    public void onFileAdd(FileAttributes fileAttributes) {
        String bestMatch = null;
        for (Map.Entry<String, String> masksWeReplace : this.replaceMasks.entrySet()) {
            String maskKey = masksWeReplace.getKey();
            if (fileAttributes.getFilename().startsWith(maskKey)) {
                if (bestMatch == null || maskKey.length() > bestMatch.length()) {
                    bestMatch = masksWeReplace.getValue();
                }
            }
        }
        if (bestMatch != null) {
            fileAttributes.setReplaceMask(bestMatch);
            String fileWithoutMask = fileAttributes.getFilename().replace(bestMatch, "");
            String fullPath = core.getFileLoader().getHomeDir() + fileWithoutMask;
            core.getFileLoader().addFileToKeep(fileWithoutMask);
            Engine.getLOGGER().debug("Adding to keep " + fullPath);
        }
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
