package org.foxesworld.newengine.utils;

import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.AppFrame;
import org.foxesworld.newengine.gui.components.SystemComponents;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DownloadUtils {

    private JLabel progressLabel;
    private JProgressBar progressBar;
    private AppFrame appFrame;

    public DownloadUtils(AppFrame appFrame) {
        this.appFrame = appFrame;
        this.progressBar = (JProgressBar) appFrame.getSystemComponents().getComponentsMap().get("progressBar");
        this.progressLabel = (JLabel) appFrame.getSystemComponents().getComponentsMap().get("progressLabel");
    }

    public void download(String Durl, String PATH) {
        this.progressBar.setStringPainted(true);
        this.progressBar.add(progressLabel);
        Thread downloadThread = new Thread(() -> downloader(Durl, PATH));
        downloadThread.start();
    }

    private void downloader(String Durl, String PATH) {
        this.appFrame.displayPanel("[{\"panel\": \"authForm\", \"display\": false},{\"panel\": \"newsForm\", \"display\": false},{\"panel\": \"download\", \"display\": true}]");
        try {
            APP.LOGGER.info(Durl + " size is - " + getFileSize(Durl) + "Mb");
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
                    this.progressBar.setString(progress + "%");
                    SwingUtilities.invokeLater(() -> {
                        this.progressBar.setValue(progress);
                        this.progressLabel.setText(loadProgress);
                    });
                }
            }
            out.close();
            byte[] response = out.toByteArray();

            try (FileOutputStream fos = new FileOutputStream(PATH)) {
                fos.write(response);
            }
        } catch (FileNotFoundException exc) {
        } catch (IOException exc) {
        }

        SwingUtilities.invokeLater(() -> {
            this.appFrame.displayPanel("[{\"panel\": \"authForm\", \"display\": true},{\"panel\": \"newsForm\", \"display\": true},{\"panel\": \"download\", \"display\": false}]");
        });
    }

    public static void unpack(String path, String dir_to) throws IOException {
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
        }
        fileZip.delete();
    }

    private static double getFileSize(String url) throws IOException {
        URL furl = new URL(url);
        URLConnection conn = furl.openConnection();
        double SFS = conn.getContentLength() / (1024 * 1024);
        conn.getInputStream().close();
        return SFS;
    }
}