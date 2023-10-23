package org.foxesworld.engine.utils;

import org.foxesworld.engine.Engine;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadUtils {
    private Engine engine;
    private int percentage;
    private int downloaded = 0;
    private JLabel progressLabel;
    private JProgressBar progressBar;

    public DownloadUtils(Engine engine) {
        this.engine = engine;
        this.progressBar = (JProgressBar) engine.getGuiBuilder().getComponentById("progressBar");
        this.progressLabel = (JLabel) engine.getGuiBuilder().getComponentById("progressLabel");
    }

    public void download(String Durl, String PATH, int totalFiles, int downloadedCount) {
        Runnable downloadTask = () -> downloader(Durl, PATH, totalFiles, downloadedCount);
        new Thread(downloadTask).start();
    }


    private void downloader(String downloadFile, String savePath, int totalFiles, int downloadedCount) {
        String Durl = engine.getEngineData().bindUrl + downloadFile;

        try {
            this.engine.getLOGGER().info(Durl + " size is - " + getFileSize(Durl) + "Mb");
            //System.out.println("Downloading file: " + Durl);
            //System.out.println("Saving to path: " + savePath);

            File parentDir = new File(savePath).getParentFile();
            if (!parentDir.isDirectory()) {
                parentDir.mkdirs();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            URL url = new URL(Durl);
            HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());

            long fileSize = httpConnection.getContentLength();
            long chunkSize = fileSize / 100;

            ByteArrayOutputStream out;
            try (InputStream in = new BufferedInputStream(httpConnection.getInputStream())) {
                out = new ByteArrayOutputStream();
                byte[] data = new byte[1024];
                long downloaded = 0;
                int read = 0;
                while (-1 != (read = in.read(data))) {
                    out.write(data, 0, read);
                    downloaded += read;
                    final int progress = (int) (downloaded / chunkSize);
                    String loadProgress = downloaded / (1024 * 1024) + "Mb /" + fileSize / (1024 * 1024) + "Mb";
                    SwingUtilities.invokeLater(() -> {
                        int percent = (downloadedCount * 100) / totalFiles;
                        this.progressBar.setValue(progress);
                        this.progressLabel.setText(loadProgress);
                    });
                }
            }
            out.close();
            byte[] response = out.toByteArray();

            try (FileOutputStream fos = new FileOutputStream(savePath)) {
                fos.write(response);
            } catch (FileNotFoundException exc) {
            } catch (IOException exc) {
            }


            synchronized (this) {
                if (percentage == 0) {
                    SwingUtilities.invokeLater(() -> {
                        this.engine.displayPanel("download->false");
                    });
                }
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private double getFileSize(String url) throws IOException {
        URL furl = new URL(url);
        double fileSizeBytes = furl.openConnection().getContentLength();
        double fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);
        return fileSizeMB;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

}
