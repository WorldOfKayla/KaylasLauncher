package org.foxesworld.engine.utils.Download;

import org.foxesworld.engine.Engine;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DownloadUtils {
    private final Engine engine;
    private final JLabel progressLabel;
    private final JProgressBar progressBar;
    private int percent;
    private long downloaded = 0;
    public DownloadUtils(Engine engine) {
        this.engine = engine;
        this.progressBar = (JProgressBar) engine.getGuiBuilder().getComponentById("progressBar");
        this.progressLabel = (JLabel) engine.getGuiBuilder().getComponentById("progressLabel");
    }

    public void downloader(String downloadFile, String savePath, long totalSize) {
        this.progressBar.add(this.progressLabel);
        String Durl = engine.getEngineData().getBindUrl() + downloadFile;

        File parentDir = new File(savePath).getParentFile();
        if (!parentDir.isDirectory()) {
            parentDir.mkdirs();
        }

        try {
            //TODO use our HTTP class (Partly done)
            URL url = new URL(Durl);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setDoOutput(false);
            httpConnection.setRequestMethod("GET");
            engine.getGETrequest().setRequestProperties(httpConnection, engine.getEngineData().getRequestProperties());
            long fileSize = httpConnection.getContentLength();
            long chunkSize = fileSize / 100;

            FileOutputStream fileOutputStream = new FileOutputStream(savePath);
            byte[] buffer = new byte[65536];

            try (InputStream in = new BufferedInputStream(httpConnection.getInputStream())) {

                int read;
                while ((read = in.read(buffer, 0, buffer.length)) != -1) {
                    fileOutputStream.write(buffer, 0, read);
                    downloaded += read;
                    percent = (int) (downloaded * 100 / totalSize);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(percent);
                        progressLabel.setText(getFileSize((int) downloaded) + "Mb /" + getFileSize(Math.toIntExact(totalSize)) + "Mb");
                    });
                }
            }
            fileOutputStream.close();
            httpConnection.disconnect();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void unpack(String path, File dir_to) {
        File fileZip = new File(path);
        try (ZipFile zip = new ZipFile(path, StandardCharsets.UTF_8)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            LinkedList<ZipEntry> zfiles = new LinkedList<>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    new File(dir_to + File.separator + entry.getName()).mkdirs(); // Use mkdirs() to create parent directories if they don't exist
                } else {
                    zfiles.add(entry);
                }
            }
            for (ZipEntry entry : zfiles) {
                File outFile = new File(dir_to, entry.getName());
                try (InputStream in = zip.getInputStream(entry);
                     OutputStream out = new FileOutputStream(outFile)) {
                    if (!outFile.getParentFile().exists()) {
                        outFile.getParentFile().mkdirs(); // Create parent directories if they don't exist
                    }
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) >= 0) {
                        out.write(buffer, 0, len);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fileZip.delete();
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