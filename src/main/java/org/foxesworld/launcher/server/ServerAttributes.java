package org.foxesworld.launcher.server;

import com.google.gson.annotations.SerializedName;

public class ServerAttributes {

    @SerializedName("id")
    public int id;

    @SerializedName("serverName")
    public String serverName;

    @SerializedName("serverVersion")
    public String serverVersion;

    @SerializedName("mainClass")
    public String mainClass;

    @SerializedName("jreVersion")
    public String jreVersion;

    @SerializedName("forgeVersion")
    public String forgeVersion;

    @SerializedName("client")
    public String client;

    @SerializedName("host")
    public String host;

    @SerializedName("port")
    public int port;
}
