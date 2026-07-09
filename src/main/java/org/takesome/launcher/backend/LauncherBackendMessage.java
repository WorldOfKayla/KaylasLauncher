package org.takesome.launcher.backend;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class LauncherBackendMessage {
    private String type;
    private String requestId;
    private String timestamp = Instant.now().toString();
    private Map<String, Object> payload = new LinkedHashMap<>();

    public LauncherBackendMessage() {
    }

    public LauncherBackendMessage(String type, String requestId, Map<String, Object> payload) {
        this.type = type;
        this.requestId = requestId;
        this.payload = payload == null ? new LinkedHashMap<>() : payload;
    }

    public static LauncherBackendMessage of(String type, String requestId, Map<String, Object> payload) {
        return new LauncherBackendMessage(type, requestId, payload);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload == null ? new LinkedHashMap<>() : payload;
    }
}
