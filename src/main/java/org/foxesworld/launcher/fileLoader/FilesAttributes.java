package org.foxesworld.launcher.fileLoader;

import com.google.gson.annotations.SerializedName;

public class FilesAttributes {
    @SerializedName("filename")
    private String filename;

    @SerializedName("hash")
    private String hash;

    @SerializedName("size")
    private int size;
    private  String replaceMask;
    public int getSize() {
        return size;
    }

    public String getFilename() {
        return filename;
    }

    public String getHash() {
        return hash;
    }

    public void setReplaceMask(String replaceMask) {
        this.replaceMask = replaceMask;
    }
    public String getReplaceMask() {
        return replaceMask;
    }
}