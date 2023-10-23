package org.foxesworld.engine.utils.Download;

import org.foxesworld.engine.Engine;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadUtils {
    private Engine engine;
    private JLabel progressLabel;
    private JProgressBar progressBar;
    private int procents;
    private long downloaded = 0;
    private DownloadListener downloadListener;

    public DownloadUtils(Engine engine) {
        this.engine = engine;
        this.progressBar = (JProgressBar) engine.getGuiBuilder().getComponentById("progressBar");
        this.progressLabel = (JLabel) engine.getGuiBuilder().getComponentById("progressLabel");
    }

    public void downloader(String downloadFile, String savePath, long totalSize) {
        String Durl = engine.getEngineData().bindUrl + downloadFile;

        File parentDir = new File(savePath).getParentFile();
        if (!parentDir.isDirectory()) {
            parentDir.mkdirs();
        }

        try {
            URL url = new URL(Durl);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setDoOutput(false);
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("User-Agent", "FoxesCraft/64.0");
            httpConnection.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4");
            httpConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            httpConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=jkghjgyutfvbhgvt78ty78yghb7y8");
            long fileSize = httpConnection.getContentLength();

            long chunkSize = fileSize / 100;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] data = new byte[1024];

            try (InputStream in = new BufferedInputStream(httpConnection.getInputStream())) {
                int read;
                while ((read = in.read(data, 0, data.length)) != -1) {
                    out.write(data, 0, read);
                    downloaded += read;
                    procents = (int) (downloaded * 100 / totalSize);
                    SwingUtilities.invokeLater(() -> {
                        downloadListener.onDownloadProgress(procents);
                        progressBar.setValue(procents);
                        progressLabel.setText(downloaded / (1024 * 1024) + "Mb /" + fileSize / (1024 * 1024) + "Mb");
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
            if (procents >= 99) {
                downloadListener.onDownloadComplete();
            }
            httpConnection.disconnect();

        } catch (IOException e) {
            downloadListener.onDownloadError(e);
            throw new RuntimeException(e);
        }
    }

    public void setDownloadListener(DownloadListener listener) {
        this.downloadListener = listener;
    }

    private double getFileSize(String url) throws IOException {
        URL furl = new URL(url);
        double fileSizeBytes = furl.openConnection().getContentLength();
        return fileSizeBytes / (1024.0 * 1024.0);
    }
}