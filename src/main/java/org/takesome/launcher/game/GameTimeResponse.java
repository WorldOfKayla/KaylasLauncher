package org.takesome.launcher.game;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * GameTimeResponse represents the server response regarding the game session.
 */
public class GameTimeResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private GameTimeData data;

    public GameTimeResponse() {
        // Default constructor
    }

    /**
     * Constructs a GameTimeResponse with the specified elapsed time.
     *
     * @param elapsedSeconds the elapsed time in seconds
     */
    public GameTimeResponse(long elapsedSeconds) {
        this.status = "success";
        this.message = "Game time update";
        this.data = new GameTimeData(elapsedSeconds);
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public GameTimeData getData() {
        return data;
    }

    /**
     * GameTimeData holds detailed information about the current game session.
     */
    public static class GameTimeData {

        @SerializedName("elapsedTime")
        private long elapsedTime;

        @SerializedName("isPlaying")
        private boolean isPlaying;

        @SerializedName("playingOn")
        private String playingOn;

        @SerializedName("servers")
        private Map<String, ServerInfo> servers;

        public GameTimeData() {
            // Default constructor
        }

        /**
         * Constructs a GameTimeData with the specified elapsed time.
         *
         * @param elapsedTime the elapsed time in seconds
         */
        public GameTimeData(long elapsedTime) {
            this.elapsedTime = elapsedTime;
        }

        public long getElapsedTime() {
            return elapsedTime;
        }

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

    /**
     * ServerInfo represents the detailed information for a particular server.
     */
    public static class ServerInfo {

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

        public ServerInfo() {
            // Default constructor
        }

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
}
