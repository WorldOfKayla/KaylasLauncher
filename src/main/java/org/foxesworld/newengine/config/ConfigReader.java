package org.foxesworld.newengine.config;

import com.foxesworld.cfgProvider.cfgProvider;
import org.foxesworld.newengine.AppFrame;

import java.util.Map;

public class ConfigReader extends ConfigAbstract {
    public ConfigReader(AppFrame appFrame) {
        setCfgExportDir("config");
        setDebug(true);
        setDirPathIndex(0);
        setCfgFileExtension(".json");
        addCfgFiles(appFrame.getConfigFiles());
    }
    public Map<String, Map> getCfgMaps() {
        return getAllCfgMaps();
    }

    @Override
    public String getFullPath() {
        return cfgProvider.getGameFullPath();
    }


}
