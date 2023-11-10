package org.foxesworld.launcher.server;

import com.google.gson.annotations.SerializedName;

public class ServerAttributes {
    @SerializedName("id")
    private int id;
    @SerializedName("serverName")
    private String serverName;
    @SerializedName("serverVersion")
    private String serverVersion;
    @SerializedName("mainClass")
    private String mainClass;
    @SerializedName("jreVersion")
    private String jreVersion;
    @SerializedName("forgeVersion")
    private String forgeVersion;
    @SerializedName("client")
    private String client;
    @SerializedName("host")
    private String host;
    @SerializedName("port")
    private int port;

    public int getId() {
        return id;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getJreVersion() {
        return jreVersion;
    }

    public String getForgeVersion() {
        return forgeVersion;
    }

    public String getClient() {
        return client;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
