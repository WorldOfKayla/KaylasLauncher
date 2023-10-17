package org.foxesworld.newengine.config;

import com.google.gson.Gson;

public class ConfigWriter {

    private ConfigReader configReader;
    public ConfigWriter(ConfigReader configReader) {
        this.configReader = configReader;
    }

    public String configToJSON(){
        return new Gson().toJson(this.configReader.getAppFrame().getCONFIG());
    }
}
