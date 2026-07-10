package org.takesome.launcher.backend;

public class LauncherServerStatus {
    private long serverId;
    private String serverName;
    private String host;
    private int port;
    private boolean online;
    private String status;
    private String message;
    private long latencyMs;
    private String versionName;
    private int protocolVersion = -1;
    private int playersOnline = -1;
    private int playersMax = -1;
    private String motd;
    private String checkedAt;
    private String error;

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }
    public int getProtocolVersion() { return protocolVersion; }
    public void setProtocolVersion(int protocolVersion) { this.protocolVersion = protocolVersion; }
    public int getPlayersOnline() { return playersOnline; }
    public void setPlayersOnline(int playersOnline) { this.playersOnline = playersOnline; }
    public int getPlayersMax() { return playersMax; }
    public void setPlayersMax(int playersMax) { this.playersMax = playersMax; }
    public String getMotd() { return motd; }
    public void setMotd(String motd) { this.motd = motd; }
    public String getCheckedAt() { return checkedAt; }
    public void setCheckedAt(String checkedAt) { this.checkedAt = checkedAt; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
