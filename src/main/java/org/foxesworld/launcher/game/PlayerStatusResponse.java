package org.foxesworld.launcher.game;

public class PlayerStatusResponse {
    private String message;
    private boolean isPlaying;
    private Long startTimestamp;

    public String getMessage() {
        return message;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public Long getStartTimestamp() {
        return startTimestamp;
    }
}
