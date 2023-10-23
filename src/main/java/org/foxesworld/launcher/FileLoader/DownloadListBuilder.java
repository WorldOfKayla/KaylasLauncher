package org.foxesworld.launcher.FileLoader;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.DownloadUtils;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DownloadListBuilder {
    private final Engine engine;
    private final HTTPrequest POSTrequest;
    private final String downloadMask = "/uploads/files/clients/";
    private final String homeDir;
    private final DownloadUtils downloadUtils;
    private final ExecutorService executorService;

    public DownloadListBuilder(Engine engine) {
        this.engine = engine;
        this.POSTrequest = engine.getPOSTrequest();
        this.homeDir = engine.getCONFIG().getFullPath();
        this.downloadUtils = new DownloadUtils(engine);
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public List<FilesArray> getFilesToDownload(String version, String client) {
        Map<String, String> request = new HashMap<>();
       request.put("sysRequest", "loadFiles");
        request.put("version", version);
        request.put("client", client);

        FilesArray[] filesArray = new Gson().fromJson(POSTrequest.send(request), FilesArray[].class);

        return Stream.of(filesArray).filter(this::shouldDownloadFile).collect(Collectors.toList());
    }

    public void downloadFiles(List<FilesArray> filesToDownload) {
        AtomicInteger downloaded = new AtomicInteger();
        engine.displayPanel("loggedForm->false|newsForm->false|download->true");

        filesToDownload.forEach(file -> executorService.execute(() -> {
            String localPath = file.filename.replace(downloadMask, "");
            String localFilePath = homeDir + localPath;
            System.out.println("Downloading " + file.filename);
            downloadUtils.download(file.filename, localFilePath, filesToDownload.size(), downloaded.getAndIncrement());
        }));
    }

    private boolean shouldDownloadFile(FilesArray fileSection) {
        String localPath = fileSection.filename.replace(downloadMask, "");
        File localFile = new File(homeDir, localPath);
        return !localFile.exists() || !checkFile(localFile, fileSection.hash, fileSection.size);
    }

    private boolean checkFile(File file, String expectedHash, long expectedSize) {
        if (!file.exists() || file.length() != expectedSize) {
            return false;
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] dataBytes = new byte[1024];
            int bytesRead;

            try (FileInputStream fis = new FileInputStream(file)) {
                while ((bytesRead = fis.read(dataBytes)) != -1) {
                    md.update(dataBytes, 0, bytesRead);
                }
            }

            byte[] digestBytes = md.digest();
            StringBuilder hexString = new StringBuilder();

            for (byte digestByte : digestBytes) {
                hexString.append(String.format("%02x", digestByte));
            }

            return hexString.toString().equals(expectedHash);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
