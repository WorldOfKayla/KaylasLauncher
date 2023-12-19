package org.foxesworld.launcher.Server;

public class ServerAttributes {
    private int id;
    private String serverName;
    private String serverVersion;
    private String mainClass;
    private String jreVersion;
    private String forgeVersion;
    private String forgeGroup;
    private  String mcpVersion;
    private String client;
    private String host;
    private int port;
    private String ignoreDirs;
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
    public String getMcpVersion() {
        return mcpVersion;
    }
    public String getForgeGroup() {
        return forgeGroup;
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
    public String getIgnoreDirs() {
        return ignoreDirs;
    }
}
