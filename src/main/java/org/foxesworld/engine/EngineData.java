package org.foxesworld.engine;

import com.google.gson.annotations.SerializedName;
import org.foxesworld.engine.game.TweakClasses;
import org.foxesworld.engine.utils.HTTP.RequestProperty;

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
