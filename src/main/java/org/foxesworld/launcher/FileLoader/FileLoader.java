package org.foxesworld.launcher.FileLoader;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.action.ActionHandler;
import org.foxesworld.engine.utils.Download.DownloadUtils;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileLoader {
    private final Engine engine;
    private final HTTPrequest POSTrequest;
    private final Set<String> filesToKeep = new HashSet<>();
    private final String homeDir;
    private final DownloadUtils downloadUtils;
    private final ExecutorService executorService;
    private FileLoaderListener fileLoaderListener;
    private final AtomicInteger filesDownloaded = new AtomicInteger(0);
    private int totalFiles;
    private String currentFile = "";

    public FileLoader(ActionHandler actionHandler) {
        this.engine = actionHandler.getEngine();
        this.POSTrequest = engine.getPOSTrequest();
        this.homeDir = engine.getCONFIG().getFullPath();
        this.downloadUtils = new DownloadUtils(engine);
        this.executorService = Executors.newFixedThreadPool(this.engine.getEngineData().downloadThreads);
    }

    public List<FilesArray> getFilesToDownload(String version, String client) {
        Map<String, String> request = new HashMap<>();
        request.put("sysRequest", "loadFiles");
        request.put("version", version);
        request.put("client", client);
        FilesArray[] filesArray = new Gson().fromJson(POSTrequest.send(engine.getEngineData().bindUrl, request), FilesArray[].class);
        for(FilesArray file: filesArray) {
            file.setReplaceMask("/uploads/files/clients/");
            addFileToKeep(file.filename.replace(file.getReplaceMask(), ""));
            System.out.println("Adding to keep "+file.filename.replace(file.getReplaceMask(), ""));
        }
        return Stream.of(filesArray).filter(this::shouldDownloadFile).collect(Collectors.toList());
    }

    public void downloadFiles(List<FilesArray> filesToDownload) {
        totalFiles = filesToDownload.size();
        this.engine.getLOGGER().debug("Downloading " + totalFiles + " files");
        engine.displayPanel("loggedForm->false|newsForm->false|download->true");
        final long totalSizeFinal = filesToDownload.stream().mapToLong(FilesArray::getSize).sum();
        filesToDownload.forEach(file -> executorService.execute(() -> {
            String localPath = file.filename.replace(file.getReplaceMask(), "");
            this.engine.getGuiBuilder().setLabelText("downloadFile", new File(localPath).getName());
            this.engine.getGuiBuilder().setLabelText("downloadDirectory", String.valueOf(new File(localPath).getParentFile()));
            fileLoaderListener.onNewFileFound(file, this.homeDir + localPath, totalSizeFinal);

            // Incrementing a counter
            filesDownloaded.incrementAndGet();

            // Checking if all files are loaded
            if (filesDownloaded.get() == totalFiles) {
                this.fileLoaderListener.onFilesLoaded();
            }
        }));
    }

    private boolean shouldDownloadFile(FilesArray fileSection) {
        String localPath = fileSection.filename.replace(fileSection.getReplaceMask(), "");
        File localFile = new File(homeDir, localPath);
        return !localFile.exists() || !checkFile(localFile, fileSection.hash, fileSection.size);
    }

    public boolean checkFile(File file, String expectedHash, long expectedSize) {
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

    public FilesArray addJreToLoad(String jreVersion){
        Map<String, String> request = new HashMap<>();
        request.put("sysRequest", "getJre");
        request.put("jreVersion", jreVersion);
        FilesArray jreFile = new Gson().fromJson(POSTrequest.send(engine.getEngineData().bindUrl, request), FilesArray.class);
        jreFile.setReplaceMask("/uploads/files/");
        return  jreFile;
    }

    public void setLoaderListener(FileLoaderListener fileLoaderListener) {
        this.fileLoaderListener = fileLoaderListener;
    }

    public DownloadUtils getDownloadUtils() {
        return downloadUtils;
    }

    public void setCurrentFile(String currentFile) {
        this.currentFile = currentFile;
    }

    public String getCurrentFile() {
        return currentFile;
    }

    public Set<String> getFilesToKeep() {
        return filesToKeep;
    }

    public void addFileToKeep(String filesToKeep) {
        this.filesToKeep.add(filesToKeep);
    }
}