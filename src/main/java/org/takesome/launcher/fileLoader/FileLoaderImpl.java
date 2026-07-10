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
import org.takesome.kaylasEngine.gui.components.progressBar.ProgressBar;
import org.takesome.kaylasEngine.gui.components.textArea.TextArea;
import org.takesome.kaylasEngine.utils.Download.DownloadUtils;
import org.takesome.kaylasEngine.utils.LongestPrefixMatcher;
import org.takesome.kaylasEngine.utils.StringTemplateResolver;
import org.takesome.launcher.Core;
import org.takesome.launcher.backend.LauncherBackendClient;
import org.takesome.launcher.fileLoader.fileGuard.FileGuardImpl;
import org.takesome.launcher.game.GameLauncher;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class FileLoaderImpl implements IFileLoaderListener {
    private static final String RUNTIME_CLIENT = "Runtime";
    private static final long FILE_UI_UPDATE_INTERVAL_MS = 150L;

    private final Core core;
    private FileLoaderUI fileLoaderUI;
    private final DownloadUtils downloadUtils;
    private final LongestPrefixMatcher<String> replaceMaskMatcher = new LongestPrefixMatcher<>();
    private final AtomicLong lastFileUiUpdateMillis = new AtomicLong(0L);

    public FileLoaderImpl(Core core) {
        this.core = core;
        this.downloadUtils = core.getFileLoader().getLauncherDownloadUtils();
    }

    @Override
    public void onFilesRead() {
        Engine.getLOGGER().debug("--==|Files are read|==--");
        this.fileLoaderUI = new FileLoaderUI(this, "download", List.of(ProgressBar.class, Label.class, TextArea.class, Button.class));
        setupGameLauncher();
        if (!Files.isRegularFile(core.getGameLauncher().getJavaExecutablePath())) {
            addRequiredRuntimeToDownload(core.getGameLauncher().getCurrentJre());
        }
        core.getFileLoader().downloadFiles();
    }

    private void setupGameLauncher() {
        core.setGameLauncher(new GameLauncher(core.getActionHandler()));
        core.getGameLauncher().setGameListener(core);
    }

    public void addRequiredRuntimeToDownload(String jreVersion) {
        if (jreVersion == null || jreVersion.isBlank()) {
            throw new IllegalStateException("Required runtime is missing locally and server has no jreVersion.");
        }

        LauncherBackendClient backendClient = core.getLauncher().getBackendClient();
        if (backendClient == null) {
            throw new IllegalStateException("Required runtime " + jreVersion + " is missing locally, but backend client is not initialized.");
        }

        try {
            Engine.getLOGGER().info("Runtime {} is missing locally. Requesting runtime files from backend.", jreVersion);
            FileAttributes[] runtimeFiles = backendClient
                    .fetchVersionFiles(RUNTIME_CLIENT, jreVersion, core.getFileLoader().resolvePlatformCode())
                    .join();
            if (runtimeFiles == null || runtimeFiles.length == 0) {
                throw new IllegalStateException("Backend returned no runtime files for client=" + RUNTIME_CLIENT + " version=" + jreVersion);
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
            if (added == 0) {
                throw new IllegalStateException("Backend runtime response contained no usable files for version " + jreVersion);
            }
            Engine.getLOGGER().info("Queued {} required backend runtime file(s) for JRE {}.", added, jreVersion);
        } catch (RuntimeException error) {
            Throwable root = rootCause(error);
            throw new IllegalStateException("Unable to request required runtime " + jreVersion + " from backend: "
                    + (root == null ? error.getMessage() : root.getMessage()), error);
        }
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
        if (!Files.isRegularFile(core.getGameLauncher().getJavaExecutablePath())) {
            throw new IllegalStateException("Required runtime was downloaded but no Java executable was found: "
                    + core.getGameLauncher().getJavaExecutablePath());
        }
        initializeArgsReader();
        setupFileGuard();
    }

    private void initializeArgsReader() {
        boolean checkLib = this.core.getActionHandler().getCurrentServer().isCheckLib();
        if (!checkLib) {
            Engine.getLOGGER().warn("LIBRARY HASH IS IGNORED!!! That may be insecure!!!");
        }
        core.getGameLauncher().setArgsReader(new ArgsReader(core.getGameLauncher(), checkLib));
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
        return replaceMaskMatcher.match(filename).orElse(null);
    }

    private void processFileAttributes(FileAttributes fileAttributes, String bestMatch) {
        fileAttributes.setReplaceMask(bestMatch);
        String localPath = core.getFileLoader().getLocalPath(fileAttributes);
        String fullPath = core.getFileLoader().getHomeDir() + localPath;
        core.getFileLoader().addFileToKeep(localPath);
        Engine.getLOGGER().trace("Adding to keep {}", fullPath);
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
        if (fullPath.contains("runtime") && fullPath.contains(Core.getOSPrefix()) && fullPath.endsWith(".zip")) {
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
        core.getLauncher().shutdownExecutorService();
        this.fileLoaderUI.getProgressBar().setValue(0);
    }

    public void setReplaceMasks(List<EngineData.ReplaceMask> replaceTemplate) {
        Map<String, Object> variables = Map.of(
                "version", core.getActionHandler().getCurrentServer().getServerVersion(),
                "serverName", core.getActionHandler().getCurrentServer().getServerName(),
                "port", core.getActionHandler().getCurrentServer().getPort()
        );
        Map<String, String> resolvedMasks = new LinkedHashMap<>();
        if (replaceTemplate != null) {
            for (EngineData.ReplaceMask mask : replaceTemplate) {
                if (mask == null || mask.getMask() == null || mask.getMask().isEmpty()) {
                    continue;
                }
                resolvedMasks.put(
                        StringTemplateResolver.resolve(mask.getMask(), variables),
                        StringTemplateResolver.resolve(mask.getReplace(), variables)
                );
            }
        }
        replaceMaskMatcher.replaceAll(resolvedMasks);
        Engine.LOGGER.debug("Prepared {} file replace-mask prefix rule(s).", replaceMaskMatcher.size());
    }

    public Core getCore() {
        return core;
    }

    public DownloadUtils getDownloadUtils() {
        return downloadUtils;
    }


}
