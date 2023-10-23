package org.foxesworld.launcher.FileLoader;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.action.ActionHandler;
import org.foxesworld.engine.utils.DownloadUtils;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class DownloadListBuilder {
    private Engine engine;
    private final ActionHandler actionHandler;
    private int totalFiles = 0;
    private final HTTPrequest POSTrequest;
    private final String downloadMask = "/uploads/files/clients/";
    private Map<String, String> loadCredentials = new HashMap<>();
    private final String homeDir;
    private final DownloadUtils downloadUtils;

    public DownloadListBuilder(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
        this.engine = actionHandler.getEngine();
        this.POSTrequest = engine.getPOSTrequest();
        this.homeDir = engine.getCONFIG().getFullPath();
        this.loadCredentials.put("sysRequest", "loadFiles");
        this.downloadUtils = new DownloadUtils(engine);
    }

    public List<FilesArray> getFilesToDownload(String version, String client) {
        this.loadCredentials.put("version", version);
        this.loadCredentials.put("client", client);
        FilesArray[] filesArray = new Gson().fromJson(this.POSTrequest.send(loadCredentials), FilesArray[].class);

        List<FilesArray> filesToLoad = Arrays.stream(filesArray).map(fileSection -> {
            String localPath = fileSection.filename.replace(downloadMask, "");
            File localFile = new File(homeDir, localPath);
            if (!localFile.exists() || !checkFile(localFile, fileSection.hash, fileSection.size)) {
                return fileSection;
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        engine.getLOGGER().debug("Total files to download: " + filesToLoad.size());

        return  filesToLoad;
    }

    private boolean checkFile(File file, String expectedHash, long expectedSize) {
        if (!file.exists() || file.length() != expectedSize) {
            return false;
        }

        if(file.length() == expectedSize) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] dataBytes = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(dataBytes)) != -1) {
                        md.update(dataBytes, 0, bytesRead);
                    }
                }

                byte[] digestBytes = md.digest();
                StringBuilder hexString = new StringBuilder();
                for (byte digestByte : digestBytes) {
                    hexString.append(Integer.toString((digestByte & 0xff) + 0x100, 16).substring(1));
                }

                return hexString.toString().equals(expectedHash);
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public Engine getEngine() {
        return engine;
    }
}
