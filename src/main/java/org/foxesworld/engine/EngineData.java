package org.foxesworld.engine;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.foxesworld.engine.game.TweakClasses;
import org.foxesworld.engine.utils.HTTP.RequestProperty;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class EngineData {
    @SerializedName("bindUrl")
    private String bindUrl;
    @SerializedName("launcherBrand")
    private String launcherBrand;
    @SerializedName("launcherVersion")
    private String launcherVersion;
    @SerializedName("appId")
    private String appId;
    @SerializedName("accessToken")
    private String accessToken;
    @SerializedName("groupDomain")
    private String groupDomain;
    @SerializedName("vkAPIversion")
    private String vkAPIversion;
    @SerializedName("downloadThreads")
    private int downloadThreads;
    @SerializedName("requestProperties")
    private List<RequestProperty> requestProperties;
    @SerializedName("tweakClasses")
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
}
