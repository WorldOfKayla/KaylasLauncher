package org.foxesworld.launcher.fileLoader;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.EngineData;
import org.foxesworld.engine.fileLoader.FileAttributes;
import org.foxesworld.engine.fileLoader.FileLoader;
import org.foxesworld.engine.fileLoader.FileLoaderListener;
import org.foxesworld.engine.fileLoader.fileGuard.FileGuard;
import org.foxesworld.engine.game.argsReader.ArgsReader;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.utils.Download.DownloadUtils;
import org.foxesworld.engine.utils.helper.JVMHelper;
import org.foxesworld.launcher.Core;
import org.foxesworld.launcher.fileLoader.fileGuard.FileGuardImpl;
import org.foxesworld.launcher.game.GameLauncher;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileLoaderImpl implements FileLoaderListener {
    private final Core core;
    private final ComponentsAccessor componentsAccessor;
    private final DownloadUtils downloadUtils;

    private final Map<String, String> replaceMasks = new HashMap<>();
    private final Map<String, String> varsToReplace = new HashMap<>();

    public FileLoaderImpl(Core core) {
        this.core = core;
        this.downloadUtils = core.getFileLoader().getDownloadUtils();
        this.componentsAccessor = new ComponentsAccessor(core.getLauncher().getGuiBuilder(), "download", List.of(Label.class, JProgressBar.class));
    }

    @Override
    public void onFilesRead() {
        Engine.getLOGGER().debug("--==|Files are read|==--");
        initializeDownloadComponents();
        setupGameLauncher();
        checkAndDownloadJre();
        core.getFileLoader().downloadFiles();
    }

    private void initializeDownloadComponents() {
        downloadUtils.setProgressBar((JProgressBar) componentsAccessor.getComponent("progressBar"));
        downloadUtils.setProgressLabel((JLabel) componentsAccessor.getComponent("progressLabel"));
    }

    private void setupGameLauncher() {
        core.setGameLauncher(new GameLauncher(core.getActionHandler()));
        core.getGameLauncher().setGameListener(core);
    }

    private void checkAndDownloadJre() {
        if (JVMHelper.getJavaVersion(core.getGameLauncher().getJreBin()) == null) {
            core.getFileLoader().setReplaceMask("/uploads/files/");
            core.getFileLoader().addFileToDownload(core.getFileLoader().addJreToLoad(core.getGameLauncher().getCurrentJre()));
        }
    }

    @Override
    public void onDownloadStart() {
        core.getLauncher().getPanelVisibility().displayPanel("loggedForm->false|newsForm->false|download->true");
        core.getLauncher().getLoadingManager().toggleLoader();
    }

    @Override
    public void onFilesLoaded() {
        Engine.getLOGGER().debug("--==|Files loaded|==--");
        initializeArgsReader();
        setupFileGuard();
    }

    private void initializeArgsReader() {
        core.getGameLauncher().setArgsReader(new ArgsReader(core.getGameLauncher()));
    }

    private void setupFileGuard() {
        List<String> checkList = getFileGuardCheckList();
        core.setFileGuard(new FileGuard(core.getGameLauncher(), checkList));
        core.getFileGuard().setFileGuardListener(new FileGuardImpl(core));
        core.getFileGuard().addIgnoreDirs(core.getActionHandler().getCurrentServer().getIgnoreDirs());
        core.getFileGuard().scanAndDeleteFilesInSubdirectories(core.getFileLoader().getFilesToKeep());
        core.getFileGuard().recursiveDelete(new File(core.getGameLauncher().getPathBuilders().buildGameDir() + "/assets/skins"));
    }

    private List<String> getFileGuardCheckList() {
        return Arrays.asList(
                core.getGameLauncher().getPathBuilders().buildClientDir(),
                core.getGameLauncher().getPathBuilders().buildVersionDir(),
                core.getGameLauncher().getPathBuilders().buildNativesPath(),
                core.getGameLauncher().getPathBuilders().buildAssetsPath()
        );
    }

    @Override
    public void onFileAdd(FileAttributes fileAttributes) {
        String bestMatch = findBestMatch(fileAttributes.getFilename());
        if (bestMatch != null) {
            processFileAttributes(fileAttributes, bestMatch);
        }
    }

    private String findBestMatch(String filename) {
        String bestMatch = null;
        for (Map.Entry<String, String> entry : replaceMasks.entrySet()) {
            String maskKey = entry.getKey();
            if (filename.startsWith(maskKey) && (bestMatch == null || maskKey.length() > bestMatch.length())) {
                bestMatch = entry.getValue();
            }
        }
        return bestMatch;
    }

    private void processFileAttributes(FileAttributes fileAttributes, String bestMatch) {
        fileAttributes.setReplaceMask(bestMatch);
        String fileWithoutMask = fileAttributes.getFilename().replace(bestMatch, "");
        String fullPath = core.getFileLoader().getHomeDir() + fileWithoutMask;
        core.getFileLoader().addFileToKeep(fileWithoutMask);
        Engine.getLOGGER().debug("Adding to keep " + fullPath);
    }

    @Override
    public void onNewFileFound(FileLoader fileLoader) {
        FileAttributes currentFile = fileLoader.getCurrentFile();
        updateDownloadInfoComponents(currentFile);
        String fullPath = core.getFileLoader().getHomeDir() + currentFile.getFilename().replace(currentFile.getReplaceMask(), "");
        if (core.getFileLoader().isInvalidFile(new File(fullPath), currentFile.getHash(), currentFile.getSize())) {
            core.getFileLoader().getDownloadUtils().downloader(currentFile.getFilename().replace(" ", "%20"), fullPath, fileLoader.getTotalSize());
        }
        unpackRuntimeZipIfNeeded(fullPath);
    }

    private void updateDownloadInfoComponents(FileAttributes currentFile) {
        ComponentsAccessor downloadInfoAccessor = new ComponentsAccessor(core.getLauncher().getGuiBuilder(), "downloadInfo", Arrays.asList(Label.class, JProgressBar.class));
        String localPath = currentFile.getFilename().replace(currentFile.getReplaceMask(), "");
        //String fullPath = core.getFileLoader().getHomeDir() + localPath;

        Label downloadFile = (Label) downloadInfoAccessor.getComponentMap().get("downloadFile");
        Label downloadDirectory = (Label) downloadInfoAccessor.getComponentMap().get("downloadDirectory");
        Label filesAmount = (Label) downloadInfoAccessor.getComponentMap().get("filesAmount");

        downloadFile.setText(new File(localPath).getName());
        downloadDirectory.setText(String.valueOf(new File(localPath).getParentFile()));
        //filesAmount.setText();
        downloadFile.setIcon(new ImageIcon(
                this.core.getLauncher().getImageUtils().getScaledImage(
                        this.core.getLauncher().getImageUtils().getLocalImage("assets/ui/icons/files/" + core.getFileLoader().getFileType() + ".png"), 36, 38)
        ));
    }

    private void unpackRuntimeZipIfNeeded(String fullPath) {
        if (fullPath.contains("runtime") && fullPath.contains("zip")) {
            core.getFileLoader().getDownloadUtils().unpack(fullPath, new File(fullPath).getParentFile());
        }
    }

    public void setReplaceMasks(List<EngineData.ReplaceMask> replaceTemplate) {
        addReplaceVars();
        replaceTemplate.forEach(mask -> varsToReplace.forEach((key, value) -> {
            String maskKey = replaceVariableValue(key, mask.getMask(), value);
            String maskValue = replaceVariableValue(key, mask.getReplace(), value);
            replaceMasks.put(maskKey, maskValue);
        }));
    }

    private String replaceVariableValue(String variableName, String originalValue, String newValue) {
        Pattern pattern = Pattern.compile("\\$\\{" + variableName + "}");
        Matcher matcher = pattern.matcher(originalValue);
        return matcher.replaceAll(newValue);
    }

    private void addReplaceVars() {
        this.varsToReplace.put("version", core.getActionHandler().getCurrentServer().getServerVersion());
    }
}