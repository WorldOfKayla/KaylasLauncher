package org.foxesworld.launcher.fileLoader;

import org.foxesworld.engine.gui.componentAccessor.Component;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.button.Button;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.gui.components.textArea.TextArea;
import org.foxesworld.engine.utils.Download.DownloadUtils;

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
