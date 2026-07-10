package org.takesome.launcher.fileLoader;

import org.takesome.kaylasEngine.fileLoader.FileAttributes;
import org.takesome.kaylasEngine.fileLoader.IFileFetcher;
import org.takesome.kaylasEngine.server.ServerAttributes;
import org.takesome.kaylasEngine.server.ServerIdentity;
import org.takesome.launcher.backend.LauncherBackendClient;
import org.takesome.launcher.gui.ActionHandler;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class BackendFileFetcher implements IFileFetcher {
    private final ActionHandler actionHandler;

    public BackendFileFetcher(ActionHandler actionHandler) {
        this.actionHandler = Objects.requireNonNull(actionHandler, "actionHandler");
    }

    @Override
    public CompletableFuture<FileAttributes[]> fetchDownloadList(String client, String version, int platformCode) {
        LauncherBackendClient backendClient = actionHandler.getLauncher().getBackendClient();
        if (backendClient == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Launcher backend client is not initialized."));
        }

        ServerAttributes server = actionHandler.getCurrentServer();
        return backendClient.fetchVersionFiles(
                ServerIdentity.coreType(server),
                ServerIdentity.overlayClient(server, client),
                version,
                platformCode
        );
    }
}
