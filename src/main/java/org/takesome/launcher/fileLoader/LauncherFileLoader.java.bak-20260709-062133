package org.takesome.launcher.fileLoader;

import org.takesome.kaylasEngine.fileLoader.AbstractFileLoader;
import org.takesome.kaylasEngine.fileLoader.FileFetcher;
import org.takesome.kaylasEngine.fileLoader.FileValidator;
import org.takesome.kaylasEngine.fileLoader.ILoadingManager;
import org.takesome.kaylasEngine.server.ServerAttributes;
import org.takesome.kaylasEngine.utils.Download.DownloadUtils;
import org.takesome.launcher.gui.ActionHandler;

import java.util.Objects;

/**
 * Launcher-side compatibility adapter for the new KaylasUIEngine file loading layer.
 * <p>
 * The old engine exposed a concrete {@code FileLoader}; the new engine exposes
 * {@link AbstractFileLoader} plus small strategy interfaces. This class keeps the
 * launcher's call-sites compact while binding them to the new implementation model.
 */
public final class LauncherFileLoader extends AbstractFileLoader {

    private final DownloadUtils launcherDownloadUtils;

    public LauncherFileLoader(ActionHandler actionHandler, String homeDir) {
        this(
                Objects.requireNonNull(actionHandler, "actionHandler"),
                homeDir,
                new DownloadUtils(actionHandler.getEngine())
        );
    }

    private LauncherFileLoader(ActionHandler actionHandler, String homeDir, DownloadUtils downloadUtils) {
        super(
                actionHandler.getEngine(),
                loadingManagerAdapter(actionHandler),
                new FileFetcher(actionHandler.getEngine())::fetchDownloadList,
                new FileValidator(),
                downloadUtils::setTotalSize,
                homeDir,
                resolveClient(actionHandler.getCurrentServer()),
                resolveVersion(actionHandler.getCurrentServer())
        );
        this.launcherDownloadUtils = downloadUtils;
    }

    private static ILoadingManager loadingManagerAdapter(ActionHandler actionHandler) {
        return new ILoadingManager() {
            @Override
            public void toggleVisibility() {
                actionHandler.getLauncher().getLoadingManager().toggleVisibility();
            }

            @Override
            public void setLoadingText(String descriptionKey, String titleKey) {
                actionHandler.getLauncher().getLoadingManager().setLoadingText(descriptionKey, titleKey);
            }
        };
    }

    private static String resolveClient(ServerAttributes server) {
        if (server == null) {
            return "";
        }
        String client = server.getClient();
        if (client != null && !client.isBlank()) {
            return client;
        }
        String serverName = server.getServerName();
        return serverName == null ? "" : serverName;
    }

    private static String resolveVersion(ServerAttributes server) {
        if (server == null || server.getServerVersion() == null) {
            return "";
        }
        return server.getServerVersion();
    }

    public DownloadUtils getLauncherDownloadUtils() {
        return launcherDownloadUtils;
    }

    public String resolveFileExtension(String fileName) {
        return getFileExtension(fileName);
    }
}
