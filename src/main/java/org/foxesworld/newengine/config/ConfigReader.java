package org.foxesworld.newengine.config;

import com.foxesworld.cfgProvider.cfgProvider;

import java.util.Map;

public class ConfigReader extends ConfigAbstract {
    public ConfigReader(String[] configFiles) {
        setCfgExportDir("config");
        setDebug(true);
        setDirPathIndex(0);
        setCfgFileExtension(".json");
        addCfgFiles(configFiles);
    }
    public Map<String, Map> getCfgMaps() {
        return getAllCfgMaps();
    }

    @Override
    public String getFullPath() {
        return cfgProvider.getGameFullPath();
    }


}
