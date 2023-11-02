package org.foxesworld.engine.utils.Download;

import me.tongfei.progressbar.ProgressBar;
import org.foxesworld.engine.Engine;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DownloadUtils {
    private Engine engine;
    private JLabel progressLabel;
    private JProgressBar progressBar;
    private int percent;
    private long downloaded = 0;
    private DownloadListener downloadListener;
    private ProgressBar consolePb;

    public DownloadUtils(Engine engine) {
        this.engine = engine;
        this.progressBar = (JProgressBar) engine.getGuiBuilder().getComponentById("progressBar");
        this.progressLabel = (JLabel) engine.getGuiBuilder().getComponentById("progressLabel");
    }

    public void downloader(String downloadFile, String savePath, long totalSize) {
        this.progressBar.add(this.progressLabel);
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
                    percent = (int) (downloaded * 100 / totalSize);
                    SwingUtilities.invokeLater(() -> {
                        this.consolePb = new ProgressBar("Exp", 100);
                        downloadListener.onDownloadProgress(percent);
                        progressBar.setValue(percent);
                        this.consolePb.stepTo(percent);
                        progressLabel.setText(getFileSize((int) downloaded) + "Mb /" + getFileSize(Math.toIntExact(totalSize)) + "Mb");
                        System.out.println(downloaded+" - "+totalSize);
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
            //System.out.println(chunkSize);
            if (downloaded == totalSize) {
                downloadListener.onDownloadComplete();
            }
            httpConnection.disconnect();

        } catch (IOException e) {
            downloadListener.onDownloadError(e);
            throw new RuntimeException(e);
        }
    }

    public void unpack(String path, File dir_to) {
        File fileZip = new File(path);
        try (ZipFile zip = new ZipFile(path)) {
            Enumeration entries = zip.entries();
            LinkedList<ZipEntry> zfiles = new LinkedList<>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    new File(dir_to + "/" + entry.getName()).mkdir();
                } else {
                    zfiles.add(entry);
                }
            }
            for (ZipEntry entry : zfiles) {
                OutputStream out;
                try (InputStream in = zip.getInputStream(entry)) {
                    out = new FileOutputStream(dir_to + File.separator + entry.getName());
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) >= 0) {
                        out.write(buffer, 0, len);
                    }
                }
                out.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fileZip.delete();
    }

    public void setDownloadListener(DownloadListener listener) {
        this.downloadListener = listener;
    }

    private double getFileSize(String url) throws IOException {
        URL furl = new URL(url);
        double fileSizeBytes = furl.openConnection().getContentLength();
        return fileSizeBytes / (1024.0 * 1024.0);
    }

    private int getFileSize(int kbSize) {
        return (int) (kbSize / (1024.0 * 1024.0));
    }
}