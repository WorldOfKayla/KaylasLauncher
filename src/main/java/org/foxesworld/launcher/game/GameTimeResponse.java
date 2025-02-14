package org.foxesworld.launcher.game;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class GameTimeResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private GameTimeData data;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public GameTimeData getData() {
        return data;
    }
}

class GameTimeData {
    @SerializedName("isPlaying")
    private boolean isPlaying;

    @SerializedName("playingOn")
    private String playingOn;

    @SerializedName("servers")
    private Map<String, ServerInfo> servers;

    public boolean isPlaying() {
        return isPlaying;
    }

    public String getPlayingOn() {
        return playingOn;
    }

    public Map<String, ServerInfo> getServers() {
        return servers;
    }
}

class ServerInfo {
    @SerializedName("totalTime")
    private long totalTime;

    @SerializedName("startTimestamp")
    private long startTimestamp;

    @SerializedName("lastUpdated")
    private long lastUpdated;

    @SerializedName("lastSession")
    private long lastSession;

    @SerializedName("lastPlayed")
    private long lastPlayed;

    public long getTotalTime() {
        return totalTime;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public long getLastSession() {
        return lastSession;
    }

    public long getLastPlayed() {
        return lastPlayed;
    }
}
