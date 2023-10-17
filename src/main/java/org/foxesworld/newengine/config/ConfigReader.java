package org.foxesworld.newengine.config;

import com.foxesworld.cfgProvider.cfgProvider;
import org.foxesworld.newengine.AppFrame;

import java.util.List;
import java.util.Map;

public class ConfigReader extends ConfigAbstract {
    private  AppFrame appFrame;
    private ConfigWriter configWriter;
    public ConfigReader(AppFrame appFrame) {
        this.appFrame = appFrame;
        this.configWriter = new ConfigWriter(this);
        setCfgExportDir("config");
        setDebug(true);
        setDirPathIndex(0);
        setCfgFileExtension(".json");
        addCfgFiles(appFrame.getConfigFiles());
    }

    public void addToConfig(Map<String, String> inputData, List values){
        for(Map.Entry<String, String> configEntry: inputData.entrySet()){
            if(values.contains(configEntry.getKey())){
                this.appFrame.getCONFIG().put(configEntry.getKey(), configEntry.getValue());
            }
        }
    }
    public Map<String, Map> getCfgMaps() {
        return getAllCfgMaps();
    }

    public ConfigWriter getConfigWriter() {
        return configWriter;
    }

    public AppFrame getAppFrame() {
        return appFrame;
    }

    @Override
    public String getFullPath() {
        return cfgProvider.getGameFullPath();
    }


}
