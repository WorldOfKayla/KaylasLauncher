package org.foxesworld.engine;

import com.google.gson.annotations.SerializedName;
import org.foxesworld.engine.gui.components.game.TweakClasses;
import org.foxesworld.engine.utils.HTTP.RequestProperty;

import java.util.List;

public class EngineData {

    @SerializedName("bindUrl")
    public String bindUrl;

    @SerializedName("appId")
    public String appId;

    @SerializedName("requestProperties")
    public List<RequestProperty> requestProperties;

    @SerializedName("tweakClasses")
    public List<TweakClasses> tweakClasses;

}
