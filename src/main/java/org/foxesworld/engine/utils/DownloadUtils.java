package org.foxesworld.engine.utils;

import org.foxesworld.engine.Engine;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadUtils {
    private Engine engine;
    private JLabel progressLabel;
    private JProgressBar progressBar;
    private JProgressBar progressBarMini;

    public DownloadUtils(Engine engine) {
        this.engine = engine;
        this.progressBar = (JProgressBar) engine.getGuiBuilder().getComponentById("progressBar");
        this.progressBarMini = (JProgressBar) engine.getGuiBuilder().getComponentById("progressMini");
        this.progressLabel = (JLabel) engine.getGuiBuilder().getComponentById("progressLabel");
    }

    public void download(String Durl, String PATH, int totalFiles, int downloadedCount) {
        Runnable downloadTask = () -> downloader(Durl, PATH, totalFiles, downloadedCount);
        new Thread(downloadTask).start();
    }

    private void downloader(String downloadFile, String savePath, int totalFiles, int downloadedCount) {
        String Durl = engine.getEngineData().bindUrl + downloadFile;

        try {
            engine.getLOGGER().info(Durl + " size is - " + getFileSize(Durl) + "Mb");

            File parentDir = new File(savePath).getParentFile();
            if (!parentDir.isDirectory()) {
                parentDir.mkdirs();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            URL url = new URL(Durl);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

            long fileSize = httpConnection.getContentLength();
            long chunkSize = fileSize / 100;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            long downloaded = 0;

            try (InputStream in = new BufferedInputStream(httpConnection.getInputStream())) {
                int read;
                while ((read = in.read(data)) != -1) {
                    out.write(data, 0, read);
                    downloaded += read;
                    int progress = (int) (downloaded / chunkSize);
                    String loadProgress = downloaded / (1024 * 1024) + "Mb /" + fileSize / (1024 * 1024) + "Mb";
                    int percent = (downloadedCount * 100) / totalFiles;

                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(percent);
                        progressBarMini.setValue(progress);
                        progressLabel.setText(loadProgress);
                    });
                }
            }

            out.close();
            byte[] response = out.toByteArray();

            try (FileOutputStream fos = new FileOutputStream(savePath)) {
                fos.write(response);
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private double getFileSize(String url) throws IOException {
        URL furl = new URL(url);
        double fileSizeBytes = furl.openConnection().getContentLength();
        return fileSizeBytes / (1024.0 * 1024.0);
    }
}