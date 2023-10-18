package org.foxesworld.newengine.config;

import com.google.gson.Gson;

public class ConfigWriter {

    private Config config;
    public ConfigWriter(Config config) {
        this.config = config;
    }

    public String configToJSON(){
        return new Gson().toJson(this.config.getAppFrame().getCONFIG());
    }
}
