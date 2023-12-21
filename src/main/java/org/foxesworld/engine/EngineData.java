package org.foxesworld.engine;

import org.foxesworld.engine.game.TweakClasses;
import org.foxesworld.engine.utils.HTTP.RequestProperty;

import java.util.List;

public class EngineData {
    private String bindUrl;
    private String launcherBrand;
    private String launcherVersion;
    private int socket;
    @Deprecated
    private String appId;
    @Deprecated
    private String accessToken;
    private String groupDomain;
    private String vkAPIversion;
    private int downloadThreads;
    private List<RequestProperty> requestProperties;
    private List<TweakClasses> tweakClasses;
    public String getBindUrl() {
        return bindUrl;
    }
    public String getLauncherBrand() {
        return launcherBrand;
    }
    public String getLauncherVersion() {
        return launcherVersion;
    }
    public String getAppId() {
        return appId;
    }
    public String getAccessToken() {
        return accessToken;
    }
    public String getGroupDomain() {
        return groupDomain;
    }
    public String getVkAPIversion() {
        return vkAPIversion;
    }
    public int getDownloadThreads() {
        return downloadThreads;
    }
    public List<RequestProperty> getRequestProperties() {
        return requestProperties;
    }
    public List<TweakClasses> getTweakClasses() {
        return tweakClasses;
    }
    public int getSocket() {
        return socket;
    }
}
