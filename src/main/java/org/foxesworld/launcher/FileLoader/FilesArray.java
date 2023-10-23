package org.foxesworld.launcher.FileLoader;

import com.google.gson.annotations.SerializedName;

class FilesArray {
    @SerializedName("filename")
    String filename;

    @SerializedName("hash")
    String hash;

    @SerializedName("size")
    int size;
}