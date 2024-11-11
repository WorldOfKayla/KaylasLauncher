package org.foxesworld.launcher.fileLoader;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.EngineData;
import org.foxesworld.engine.fileLoader.FileAttributes;
import org.foxesworld.engine.fileLoader.FileLoader;
import org.foxesworld.engine.fileLoader.FileLoaderListener;
import org.foxesworld.engine.fileLoader.fileGuard.FileGuard;
import org.foxesworld.engine.game.argsReader.ArgsReader;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.button.Button;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.utils.Download.DownloadUtils;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.helper.JVMHelper;
import org.foxesworld.launcher.Core;
import org.foxesworld.launcher.fileLoader.fileGuard.FileGuardImpl;
import org.foxesworld.launcher.game.GameLauncher;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileLoaderImpl extends ComponentsAccessor implements FileLoaderListener {
    private final Core core;
    private final DownloadUtils downloadUtils;
    private final Map<String, String> replaceMasks = new HashMap<>();
    private final Map<String, String> varsToReplace = new HashMap<>();

    public FileLoaderImpl(Core core) {
        super(core.getLauncher().getGuiBuilder(), "download", List.of(JProgressBar.class, Label.class, Button.class));
        this.core = core;
        this.downloadUtils = core.getFileLoader().getDownloadUtils();
    }

    @Override
    public void onFilesRead() {
        Engine.getLOGGER().debug("--==|Files are read|==--");
        initializeDownloadComponents();
        setupGameLauncher();
        if (JVMHelper.getJavaVersion(core.getGameLauncher().getJreBin()) == null) {
            addJreToLoadAsync(core.getGameLauncher().getCurrentJre());
        }
        core.getFileLoader().downloadFiles();
    }

    private void initializeDownloadComponents() {
        downloadUtils.setProgressBar((JProgressBar) this.getComponent("progressBar"));
        downloadUtils.setProgressLabel((JLabel) this.getComponent("progressLabel"));
        downloadUtils.setCancelButton((Button) this.getComponent("cancelDownload-small"));
    }

    private void setupGameLauncher() {
        core.setGameLauncher(new GameLauncher(core.getActionHandler()));
        core.getGameLauncher().setGameListener(core);
    }

    public void addJreToLoadAsync(String jreVersion) {
        Map<String, Object> request = new HashMap<>();
        request.put("sysRequest", "getJre");
        request.put("jreVersion", jreVersion);

        CompletableFuture<FileAttributes> future = new CompletableFuture<>();

        HTTPrequest httpRequest = new HTTPrequest(core.getLauncher(), "POST");
        httpRequest.sendAsync(request, response -> {
            try {
                FileAttributes jreFile = new Gson().fromJson((String) response, FileAttributes.class);
                jreFile.setReplaceMask("/uploads/files/");
                future.complete(jreFile);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, future::completeExceptionally);
        core.getFileLoader().addFileToDownload(future.join());
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
        String fullPath = constructFullPath(fileLoader.getHomeDir(), currentFile);
        if (fileLoader.shouldDownloadFile(currentFile)) {
            startDownload(fileLoader, currentFile, fullPath);
        }
        unpackRuntimeZipIfNeeded(fullPath);
    }

    private String constructFullPath(String homeDir, FileAttributes currentFile) {
        return homeDir + currentFile.getFilename().replace(currentFile.getReplaceMask(), "");
    }

    private void startDownload(FileLoader fileLoader, FileAttributes currentFile, String fullPath) {
        String encodedFilename = currentFile.getFilename().replace(" ", "%20");
        fileLoader.getDownloadUtils().downloader(encodedFilename, fullPath, fileLoader.getTotalSize());
    }
    private void updateDownloadInfoComponents(FileAttributes currentFile) {
        ComponentsAccessor downloadInfoAccessor = new ComponentsAccessor(core.getLauncher().getGuiBuilder(), "downloadInfo", Arrays.asList(Label.class, JProgressBar.class));
        String localPath = currentFile.getFilename().replace(currentFile.getReplaceMask(), "");

        Label downloadFile = (Label) downloadInfoAccessor.getComponentMap().get("downloadFile");
        Label downloadDirectory = (Label) downloadInfoAccessor.getComponentMap().get("downloadDirectory");

        downloadFile.setText(new File(localPath).getName());
        downloadDirectory.setText(String.valueOf(new File(localPath).getParentFile()));
        downloadFile.setIcon(core.getLauncher().getIconUtils().getVectorIcon("assets/ui/icons/files/" + this.core.getFileLoader().getFileExtension(currentFile.getFilename()) + ".svg", 36f, 38f));

        this.getPanel().updateUI();
        this.getPanel().repaint();
        this.getPanel().revalidate();
    }


    private void unpackRuntimeZipIfNeeded(String fullPath) {
        if (fullPath.contains("runtime") && fullPath.contains("zip")) {
            core.getFileLoader().getDownloadUtils().unpack(fullPath, new File(fullPath).getParentFile());
        }
    }

    @Override
    public void filesProcessed() {
    }

    @Override
    public void onCancel() {
        Engine.getLOGGER().info("--==|Download canceled|==--");
        core.getLauncher().getPanelVisibility().displayPanel("download->false|loggedForm->true|newsForm->true");
        //((JProgressBar)this.getComponent("progressBar")).setValue(0);
    }

    public void setReplaceMasks(List<EngineData.ReplaceMask> replaceTemplate) {
        addReplaceVars("version", core.getActionHandler().getCurrentServer().getServerVersion());
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

    private void addReplaceVars(String replace, String replacer) {
        varsToReplace.put(replace, replacer);
    }
}
