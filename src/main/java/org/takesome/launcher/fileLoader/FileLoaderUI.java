package org.takesome.launcher.fileLoader;

import org.takesome.kaylasEngine.gui.componentAccessor.Component;
import org.takesome.kaylasEngine.gui.componentAccessor.ComponentsAccessor;
import org.takesome.kaylasEngine.gui.components.button.Button;
import org.takesome.kaylasEngine.gui.components.label.Label;
import org.takesome.kaylasEngine.gui.components.textArea.TextArea;
import org.takesome.kaylasEngine.utils.Download.DownloadUtils;

import javax.swing.*;
import java.util.List;

@SuppressWarnings("unused")
public class FileLoaderUI extends ComponentsAccessor {

    @Component
    private Label progressLabel, downloadFile, downloadDirectory, downloadSpeed;
    @Component
    private JProgressBar progressBar;
    @Component
    private TextArea serverDescArea;
    @Component("cancelDownload-small")
    private Button cancelDownload;

    public FileLoaderUI(FileLoaderImpl fileLoader, String panelId, List<Class<?>> componentTypes) {
        super(fileLoader.getCore().getLauncher().getGuiBuilder(), panelId, componentTypes);
        this.initializeDownloadComponents(fileLoader.getDownloadUtils());
    }

    private void initializeDownloadComponents(DownloadUtils downloadUtils) {
        downloadUtils.setProgressBar(this.progressBar);
        downloadUtils.setProgressLabel(this.progressLabel);
        downloadUtils.setCancelButton(this.cancelDownload);
    }

    public Label getProgressLabel() {
        return progressLabel;
    }

    public Label getDownloadFile() {
        return downloadFile;
    }

    public Label getDownloadDirectory() {
        return downloadDirectory;
    }

    public Label getDownloadSpeed() {
        return downloadSpeed;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public TextArea getServerDescArea() {
        return serverDescArea;
    }

    public Button getCancelDownload() {
        return cancelDownload;
    }
}
