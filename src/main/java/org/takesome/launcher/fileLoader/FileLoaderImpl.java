package org.takesome.launcher.fileLoader;

import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.EngineData;
import org.takesome.kaylasEngine.fileLoader.AbstractFileLoader;
import org.takesome.kaylasEngine.fileLoader.FileAttributes;
import org.takesome.kaylasEngine.fileLoader.IFileLoaderListener;
import org.takesome.kaylasEngine.fileLoader.fileGuard.FileGuard;
import org.takesome.kaylasEngine.game.argsReader.ArgsReader;
import org.takesome.kaylasEngine.gui.components.button.Button;
import org.takesome.kaylasEngine.gui.components.label.Label;
import org.takesome.kaylasEngine.gui.components.textArea.TextArea;
import org.takesome.kaylasEngine.utils.Download.DownloadUtils;
import org.takesome.kaylasEngine.utils.helper.JVMHelper;
import org.takesome.launcher.Core;
import org.takesome.launcher.backend.LauncherBackendClient;
import org.takesome.launcher.fileLoader.fileGuard.FileGuardImpl;
import org.takesome.launcher.game.GameLauncher;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileLoaderImpl implements IFileLoaderListener {
    private static final String RUNTIME_CLIENT = "Runtime";
    private static final long FILE_UI_UPDATE_INTERVAL_MS = 150L;

    private final Core core;
    private FileLoaderUI fileLoaderUI;
    private final DownloadUtils downloadUtils;
    private final Map<String, String> replaceMasks = new HashMap<>();
    private final Map<String, String> varsToReplace = new HashMap<>();
    private final AtomicLong lastFileUiUpdateMillis = new AtomicLong(0L);

    public FileLoaderImpl(Core core) {

        this.core = core;
        this.downloadUtils = core.getFileLoader().getLauncherDownloadUtils();
    }

    @Override
    public void onFilesRead() {
        Engine.getLOGGER().debug("--==|Files are read|==--");
        this.fileLoaderUI = new FileLoaderUI(this, "download", List.of(JProgressBar.class, Label.class, TextArea.class, Button.class));
        setupGameLauncher();
        if (JVMHelper.getJavaVersion(core.getGameLauncher().getJreBin()) == null) {
            addJreToLoadAsync(core.getGameLauncher().getCurrentJre());
        }
        core.getFileLoader().downloadFiles();
    }

    private void setupGameLauncher() {
        core.setGameLauncher(new GameLauncher(core.getActionHandler()));
        core.getGameLauncher().setGameListener(core);
    }

    public void addJreToLoadAsync(String jreVersion) {
        if (jreVersion == null || jreVersion.isBlank()) {
            Engine.getLOGGER().warn("Runtime is missing locally, but server has no jreVersion. Continuing without runtime download.");
            return;
        }
        if (hasQueuedRuntime(jreVersion)) {
            Engine.getLOGGER().info("Runtime {} is already present in backend version file list.", jreVersion);
            return;
        }

        LauncherBackendClient backendClient = core.getLauncher().getBackendClient();
        if (backendClient == null) {
            Engine.getLOGGER().warn("Runtime {} is missing locally, but backend client is not initialized. Continuing with version files only.", jreVersion);
            return;
        }

        try {
            FileAttributes[] runtimeFiles = backendClient
                    .fetchVersionFiles(RUNTIME_CLIENT, jreVersion, core.getFileLoader().resolvePlatformCode())
                    .join();
            if (runtimeFiles == null || runtimeFiles.length == 0) {
                Engine.getLOGGER().warn("Runtime {} is missing locally and backend returned no runtime files under client={}. Continuing with version files only.",
                        jreVersion, RUNTIME_CLIENT);
                return;
            }

            int added = 0;
            for (FileAttributes runtimeFile : runtimeFiles) {
                if (runtimeFile == null) {
                    continue;
                }
                if (runtimeFile.getReplaceMask() != null && !runtimeFile.getReplaceMask().isBlank()) {
                    processFileAttributes(runtimeFile, runtimeFile.getReplaceMask());
                }
                core.getFileLoader().addFileToDownload(runtimeFile);
                added++;
            }
            Engine.getLOGGER().info("Queued {} backend runtime file(s) for JRE {}.", added, jreVersion);
        } catch (RuntimeException error) {
            Throwable root = rootCause(error);
            Engine.getLOGGER().warn("Unable to request runtime {} from backend: {}. Continuing with version files only.",
                    jreVersion, root == null ? error.getMessage() : root.getMessage());
        }
    }

    private boolean hasQueuedRuntime(String jreVersion) {
        return core.getFileLoader().getFileAttributes().stream()
                .map(FileAttributes::getFilename)
                .filter(filename -> filename != null)
                .map(String::toLowerCase)
                .anyMatch(filename -> filename.contains("runtime") && filename.contains(jreVersion.toLowerCase()));
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == null ? throwable : current;
    }

    @Override
    public void onDownloadStart() {
        SwingUtilities.invokeLater(() -> {
            core.getLauncher().getPanelVisibility().displayPanel("loggedForm->false|newsForm->false|download->true");
            core.getLauncher().getLoadingManager().toggleVisibility();
            this.fileLoaderUI.getServerDescArea().setText(this.core.getActionHandler().getCurrentServer().getServerDescription());
        });
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
            return;
        }

        String backendReplaceMask = fileAttributes.getReplaceMask();
        if (backendReplaceMask != null && !backendReplaceMask.isBlank()) {
            processFileAttributes(fileAttributes, backendReplaceMask);
        } else {
            Engine.getLOGGER().warn("File attribute has no matching replace mask: {}", fileAttributes.getFilename());
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
        String localPath = core.getFileLoader().getLocalPath(fileAttributes);
        String fullPath = core.getFileLoader().getHomeDir() + localPath;
        core.getFileLoader().addFileToKeep(localPath);
        Engine.getLOGGER().debug("Adding to keep " + fullPath);
    }

    @Override
    public void onNewFileFound(AbstractFileLoader source) {
        FileAttributes currentFile = source.getCurrentFile();
        if (currentFile != null) {
            onNewFileFound(source, currentFile);
        }
    }

    @Override
    public void onNewFileFound(AbstractFileLoader source, FileAttributes currentFile) {
        if (currentFile == null) {
            return;
        }
        LauncherFileLoader fileLoader = core.getFileLoader();
        updateDownloadInfoComponents(currentFile);
        String fullPath = constructFullPath(fileLoader.getHomeDir(), currentFile);
        if (fileLoader.shouldDownloadFile(currentFile)) {
            startDownload(fileLoader, currentFile, fullPath);
        }
        unpackRuntimeZipIfNeeded(fullPath);
    }

    private String constructFullPath(String homeDir, FileAttributes currentFile) {
        return homeDir + core.getFileLoader().getLocalPath(currentFile);
    }

    private void startDownload(LauncherFileLoader fileLoader, FileAttributes currentFile, String fullPath) {
        String encodedFilename = currentFile.getFilename().replace(" ", "%20");
        fileLoader.getLauncherDownloadUtils().downloader(encodedFilename, fullPath);
    }

    private void updateDownloadInfoComponents(FileAttributes currentFile) {
        if (!shouldUpdateFileUi()) {
            return;
        }
        String localPath = core.getFileLoader().getLocalPath(currentFile);
        String fileName = new File(localPath).getName();
        String directory = String.valueOf(new File(localPath).getParentFile());
        String extension = this.core.getFileLoader().resolveFileExtension(currentFile.getFilename());
        String speedText = this.core.getFileLoader().getLauncherDownloadUtils().getCurrentSpeedText();
        SwingUtilities.invokeLater(() -> {
            if (this.fileLoaderUI == null) {
                return;
            }
            this.fileLoaderUI.getDownloadFile().setText(fileName);
            this.fileLoaderUI.getDownloadDirectory().setText(directory);
            this.fileLoaderUI.getDownloadFile().setIcon(core.getLauncher().getIconUtils().getVectorIcon("assets/ui/icons/files/" + extension + ".svg", 36f, 38f));
            this.fileLoaderUI.getDownloadSpeed().setText(speedText);
            this.fileLoaderUI.getPanel().repaint();
        });
    }

    private boolean shouldUpdateFileUi() {
        long now = System.currentTimeMillis();
        long last = lastFileUiUpdateMillis.get();
        return now - last >= FILE_UI_UPDATE_INTERVAL_MS && lastFileUiUpdateMillis.compareAndSet(last, now);
    }

    private void unpackRuntimeZipIfNeeded(String fullPath) {
        if (fullPath.contains("runtime") && fullPath.contains(Core.getOSPrefix()) && fullPath.contains("zip")) {
            core.getFileLoader().getLauncherDownloadUtils().unpack(fullPath, new File(fullPath).getParentFile());
        }
    }

    @Override
    public void filesProcessed() {
        if (this.fileLoaderUI != null) {
            this.fileLoaderUI.getProgressBar().setValue(100);
        }
    }

    @Override
    public void onCancel() {
        Engine.getLOGGER().info("--==|Download canceled|==--");
        core.getLauncher().getPanelVisibility().displayPanel("download->false|loggedForm->true|newsForm->true");
        core.getLauncher().getExecutorServiceProvider().shutdown();
        this.fileLoaderUI.getProgressBar().setValue(0);
    }

    public void setReplaceMasks(List<EngineData.ReplaceMask> replaceTemplate) {
        String serverVersion = core.getActionHandler().getCurrentServer().getServerVersion();
        String serverName = core.getActionHandler().getCurrentServer().getServerName();
        String port = String.valueOf(core.getActionHandler().getCurrentServer().getPort());

        addReplaceVars("version", serverVersion);
        addReplaceVars("serverName", serverName);
        addReplaceVars("port", port);

        Map<String, Pattern> variablePatterns = varsToReplace.keySet().stream().collect(Collectors.toMap(var -> var, var -> Pattern.compile("\\$\\{" + var + "}")));

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

    public Core getCore() {
        return core;
    }

    public DownloadUtils getDownloadUtils() {
        return downloadUtils;
    }

    private String replaceVariableValue(Pattern pattern, String originalValue, String replacementValue) {
        Matcher matcher = pattern.matcher(originalValue);
        return matcher.replaceAll(replacementValue);
    }

    private void addReplaceVars(String replace, String replacer) {
        varsToReplace.put(replace, replacer);
    }
}
