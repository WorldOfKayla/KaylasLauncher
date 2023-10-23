package org.foxesworld.engine.utils;

import org.foxesworld.engine.Engine;
import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadUtils {
    private Engine engine;
    private int totalFilesToDownload;
    private int downloaded = 0;

    public DownloadUtils(Engine engine) {
        this.engine = engine;
    }

    public void download(String Durl, String PATH, int totalFiles) {
        this.totalFilesToDownload = totalFiles;
        Runnable downloadTask = () -> downloader(Durl, PATH);
        new Thread(downloadTask).start();
    }


    private void downloader(String downloadFile, String PATH) {
        String Durl = engine.getEngineData().bindUrl + downloadFile;
        this.engine.displayPanel("loggedForm->false|newsForm->false|download->true");

        try {
            this.engine.getLOGGER().info(Durl + " size is - " + getFileSize(Durl) + "Mb");
            System.out.println("Downloading file: " + Durl);
            System.out.println("Saving to path: " + PATH);

            File parentDir = new File(PATH).getParentFile();
            if(!parentDir.isDirectory()) {
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
                        updateProgressBar(progress, loadProgress);
                    });
                }
            }
            out.close();
            byte[] response = out.toByteArray();

            try (FileOutputStream fos = new FileOutputStream(PATH)) {
                fos.write(response);
            } catch (FileNotFoundException exc) {
            } catch (IOException exc) {
            }
            downloaded++;
            totalFilesToDownload--;

            synchronized (this) {
                if (totalFilesToDownload == 0) {
                    SwingUtilities.invokeLater(() -> {
                        this.engine.displayPanel("download->false");
                    });
                }
            }
            //System.out.println("Files reaming " + (totalFilesToDownload - downloaded));
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

    private void updateProgressBar(int progress, String loadProgress) {
        // Здесь можно обновить ваш ProgressBar
    }

    public void setTotalFilesToDownload(int totalFilesToDownload) {
        this.totalFilesToDownload = totalFilesToDownload;
    }

}
