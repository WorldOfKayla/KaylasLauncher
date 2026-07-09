package org.takesome.launcher.fileLoader;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.fileLoader.FileAttributes;
import org.takesome.kaylasEngine.fileLoader.IFileFetcher;
import org.takesome.launcher.backend.LauncherBackendClient;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class BackendFileFetcher implements IFileFetcher {
    private final Launcher launcher;

    public BackendFileFetcher(Launcher launcher) {
        this.launcher = Objects.requireNonNull(launcher, "launcher");
    }

    @Override
    public CompletableFuture<FileAttributes[]> fetchDownloadList(String client, String version, int platformCode) {
        LauncherBackendClient backendClient = launcher.getBackendClient();
        if (backendClient == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Launcher backend client is not initialized."));
        }
        return backendClient.fetchVersionFiles(client, version, platformCode);
    }
}
