package org.takesome.launcher.backend;

import java.util.LinkedHashMap;
import java.util.Map;

public class LauncherGameVersion {
    private String id;
    private String name;
    private String client;
    private String version;
    private String jreVersion;
    private String type;
    private String releaseTime;
    private String url;
    private String sha1;
    private boolean enabled = true;
    private Map<String, Object> metadata = new LinkedHashMap<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getClient() { return client; }
    public void setClient(String client) { this.client = client; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getJreVersion() { return jreVersion; }
    public void setJreVersion(String jreVersion) { this.jreVersion = jreVersion; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getReleaseTime() { return releaseTime; }
    public void setReleaseTime(String releaseTime) { this.releaseTime = releaseTime; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSha1() { return sha1; }
    public void setSha1(String sha1) { this.sha1 = sha1; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata == null ? new LinkedHashMap<>() : metadata; }
}
