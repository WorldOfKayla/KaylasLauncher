package org.foxesworld.launcher.fileLoader;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.EngineData;
import org.foxesworld.engine.fileLoader.FileAttributes;
import org.foxesworld.engine.fileLoader.FileLoader;
import org.foxesworld.engine.fileLoader.FileLoaderListener;
import org.foxesworld.engine.fileLoader.fileGuard.FileGuard;
import org.foxesworld.engine.game.argsReader.ArgsReader;
import org.foxesworld.engine.gui.componentAccessor.Component;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.button.Button;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.gui.components.textArea.TextArea;
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
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileLoaderImpl extends ComponentsAccessor implements FileLoaderListener {
    private final Core core;
    private final DownloadUtils downloadUtils;
    private final Map<String, String> replaceMasks = new HashMap<>();
    private final Map<String, String> varsToReplace = new HashMap<>();
    @SuppressWarnings("unused")
    @Component
    private Label progressLabel, downloadFile, downloadDirectory;
    @SuppressWarnings("unused")
    @Component
    private JProgressBar progressBar;

    @SuppressWarnings("unused")
    @Component
    private TextArea serverDescArea;

    @SuppressWarnings("unused")
    @Component("cancelDownload-small")
    private Button cancelDownload;

    public FileLoaderImpl(Core core) {
        super(core.getLauncher().getGuiBuilder(), "download", List.of(JProgressBar.class, Label.class, TextArea.class, Button.class));
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
        downloadUtils.setProgressBar(this.progressBar);
        downloadUtils.setProgressLabel(this.progressLabel);
        downloadUtils.setCancelButton(this.cancelDownload);
    }

    private void setupGameLauncher() {
        core.setGameLauncher(new GameLauncher(core.getActionHandler()));
        core.getGameLauncher().setGameListener(core);
    }

    public void addJreToLoadAsync(String jreVersion) {
        JreRequest jreRequest = new JreRequest(this.core.getLauncher(), jreVersion);
        CompletableFuture<FileAttributes> future = new CompletableFuture<>();
        jreRequest.sendAsync(Map.of(), response -> {
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
        core.getLauncher().getLoadingManager().toggleVisibility();
       // DownloadProcessor downloadProcessor = new DownloadProcessor(this.core.getLauncher().getGuiBuilder(), "download", List.of(TextArea.class));
        this.serverDescArea.setText(this.core.getActionHandler().getCurrentServer().getServerDescription());
    }

    @Override
    public void onFilesLoaded() {
        Engine.getLOGGER().debug("--==|Files loaded|==--");
        initializeArgsReader();
        setupFileGuard();
    }

    private void initializeArgsReader() {
        core.getGameLauncher().setArgsReader(new ArgsReader(core.getGameLauncher(), this.core.getActionHandler().getCurrentServer().isCheckLib()));
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
                core.getGameLauncher().getPathBuilders().buildClientDir().toAbsolutePath().toString(),
                core.getGameLauncher().getPathBuilders().buildVersionDir().toAbsolutePath().toString(),
                core.getGameLauncher().getPathBuilders().buildNativesPath().toAbsolutePath().toString(),
                core.getGameLauncher().getPathBuilders().buildAssetsPath().toAbsolutePath().toString()
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
        int bestMatchLength = 0;

        for (Map.Entry<String, String> entry : replaceMasks.entrySet()) {
            String maskKey = entry.getKey();
            String maskValue = entry.getValue();

            if (filename.startsWith(maskKey)) {
                int maskKeyLength = maskKey.length();
                if (maskKeyLength > bestMatchLength) {
                    bestMatch = maskValue;
                    bestMatchLength = maskKeyLength;
                }

                // Early exit for exact match
                if (maskKeyLength == filename.length()) {
                    break;
                }
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
        String localPath = currentFile.getFilename().replace(currentFile.getReplaceMask(), "");

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
        this.progressBar.setValue(100);
    }

    @Override
    public void onCancel() {
        Engine.getLOGGER().info("--==|Download canceled|==--");
        core.getLauncher().getPanelVisibility().displayPanel("download->false|loggedForm->true|newsForm->true");
        core.getLauncher().getExecutorServiceProvider().shutdown();
        this.progressBar.setValue(0);
    }

    public void setReplaceMasks(List<EngineData.ReplaceMask> replaceTemplate) {
        String serverVersion = core.getActionHandler().getCurrentServer().getServerVersion();
        String serverName = core.getActionHandler().getCurrentServer().getServerName();
        String port = String.valueOf(core.getActionHandler().getCurrentServer().getPort());

        addReplaceVars("version", serverVersion);
        addReplaceVars("serverName", serverName);
        addReplaceVars("port", port);

        Map<String, Pattern> variablePatterns = varsToReplace.keySet().stream()
                .collect(Collectors.toMap(var -> var, var -> Pattern.compile("\\$\\{" + var + "}")));

        for (EngineData.ReplaceMask mask : replaceTemplate) {
            String maskKey = mask.getMask();
            String maskValue = mask.getReplace();

            for (Map.Entry<String, String> entry : varsToReplace.entrySet()) {
                String variable = entry.getKey();
                String replacementValue = entry.getValue();
                Pattern pattern = variablePatterns.get(variable);
                maskKey = replaceVariableValue(pattern, maskKey, replacementValue);
                maskValue = replaceVariableValue(pattern, maskValue, replacementValue);
            }

            replaceMasks.put(maskKey, maskValue);
        }
    }

    private String replaceVariableValue(Pattern pattern, String originalValue, String replacementValue) {
        Matcher matcher = pattern.matcher(originalValue);
        return matcher.replaceAll(replacementValue);
    }

    private void addReplaceVars(String replace, String replacer) {
        varsToReplace.put(replace, replacer);
    }
}
