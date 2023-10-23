package org.foxesworld.launcher.FileLoader;

import com.google.gson.annotations.SerializedName;

public class FilesArray {
    @SerializedName("filename")
    public String filename;

    @SerializedName("hash")
    public String hash;

    @SerializedName("size")
    public int size;
}