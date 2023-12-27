package org.foxesworld.launcher.FileLoader;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;
import org.foxesworld.launcher.action.ActionHandler;
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

    private List<FilesAttributes> filesAttributes;
    private final Set<String> filesToKeep = new HashSet<>();
    private final String homeDir, client, version;
    private final DownloadUtils downloadUtils;
    private final ExecutorService executorService;
    private FileLoaderListener fileLoaderListener;
    private final AtomicInteger filesDownloaded = new AtomicInteger(0);
    private int totalFiles;

    public FileLoader(ActionHandler actionHandler) {
        this.engine = actionHandler.getEngine();
        this.client = actionHandler.getCurrentServer().getServerName();
        this.version = actionHandler.getCurrentServer().getServerVersion();
        this.POSTrequest = engine.getPOSTrequest();
        this.homeDir = engine.getCONFIG().getFullPath();
        this.downloadUtils = new DownloadUtils(engine);
        this.executorService = Executors.newFixedThreadPool(this.engine.getEngineData().getDownloadThreads());
    }

    public void getFilesToDownload() {
        this.engine.getLoadingManager().startLoading();
        Map<String, String> request = new HashMap<>();
        String fileWuthoutMask;
        request.put("sysRequest", "loadFiles");
        request.put("version", version);
        request.put("client", client);
        request.put("platform", String.valueOf(this.getPlatformNumber()));
        FilesAttributes[] filesAttributes = new Gson().fromJson(POSTrequest.send(engine.getEngineData().getBindUrl(), request), FilesAttributes[].class);
        for(FilesAttributes file: filesAttributes) {
            file.setReplaceMask("/uploads/files/clients/");
            fileWuthoutMask = file.getFilename().replace(file.getReplaceMask(), "");
            addFileToKeep(fileWuthoutMask);
            this.engine.getLOGGER().debug("Adding to keep "+fileWuthoutMask);
            this.engine.getLoadingManager().setLoadingText(engine.getLANG().getString("file.received")+fileWuthoutMask, "file.getting");
        }
        this.engine.getLOGGER().info("Keeping " + this.filesToKeep.size() +" files");
        this.engine.getLoadingManager().setLoadingText(String.valueOf(filesToKeep.size()), "file.amount");
        this.filesAttributes = Stream.of(filesAttributes).filter(this::shouldDownloadFile).collect(Collectors.toList());
        fileLoaderListener.onFilesRead();
    }
    private boolean shouldDownloadFile(FilesAttributes fileSection) {
        String localPath = fileSection.getFilename().replace(fileSection.getReplaceMask(), "");
        File localFile = new File(homeDir, localPath);
        return isInvalidFile(localFile, fileSection.getHash(), fileSection.getSize());
    }

    public void downloadFiles() {
        totalFiles = filesAttributes.size();
        this.engine.getLOGGER().debug("~-=== Downloading " + totalFiles + " files ===-~");
        if(totalFiles == 0) {this.fileLoaderListener.onFilesLoaded();}

        engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->false|download->true");
        final long totalSizeFinal = filesAttributes.stream().mapToLong(FilesAttributes::getSize).sum();
        filesAttributes.forEach(file -> executorService.execute(() -> {
            String localPath = file.getFilename().replace(file.getReplaceMask(), "");
            fileLoaderListener.onNewFileFound(file, localPath, totalSizeFinal);

            // Incrementing a counter
            filesDownloaded.incrementAndGet();

            // Checking if all files are loaded
            if (filesDownloaded.get() == totalFiles) {
                this.fileLoaderListener.onFilesLoaded();
            }
        }));
    }

    public boolean isInvalidFile(File file, String expectedHash, long expectedSize) {
        if (!file.exists() || file.length() != expectedSize) {
            return true;
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

            return !hexString.toString().equals(expectedHash);
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public int getPlatformNumber() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return 1; // Windows
        } else if (osName.contains("mac")) {
            return 2; // macOS
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("uni")) {
            return 3; // Unix / Linux
        } else if (osName.contains("sunos")) {
            return 4; // Solaris
        } else {
            return 0; // Other or Unknown
        }
    }

    public FilesAttributes addJreToLoad(String jreVersion){
        Map<String, String> request = new HashMap<>();
        request.put("sysRequest", "getJre");
        request.put("jreVersion", jreVersion);
        FilesAttributes jreFile = new Gson().fromJson(POSTrequest.send(engine.getEngineData().getBindUrl(), request), FilesAttributes.class);
        jreFile.setReplaceMask("/uploads/files/");
        return  jreFile;
    }

    public void setLoaderListener(FileLoaderListener fileLoaderListener) {
        this.fileLoaderListener = fileLoaderListener;
    }

    public DownloadUtils getDownloadUtils() {
        return downloadUtils;
    }

    public Set<String> getFilesToKeep() {
        return filesToKeep;
    }

    public void addFileToKeep(String filesToKeep) {
        this.filesToKeep.add(filesToKeep);
    }

    public void addFileToDownload(FilesAttributes filesAttributes) {
        this.filesAttributes.add(filesAttributes);
    }

    public String getHomeDir() {
        return homeDir;
    }
}