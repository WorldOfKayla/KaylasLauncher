package org.foxesworld.engine.config;

import com.foxesworld.cfgProvider.cfgProvider;
import com.google.gson.GsonBuilder;
import org.foxesworld.engine.AppFrame;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Config extends ConfigAbstract {
    private AppFrame appFrame;
    private Map<String, Object> CONFIG;

    public Config(AppFrame appFrame) {
        this.appFrame = appFrame;
        setCfgExportDir("config/");
        setDebug(true);
        setDirPathIndex(3);
        setCfgFileExtension(".json");
        cfgProvider.setDefaultConfFilesDir("config/");
        addCfgFiles(appFrame.getConfigFiles());
        this.CONFIG = getCfgMaps().get("config");
        System.out.println(this.getFullPath());
    }

    public void addToConfig(Map<String, String> inputData, List values) {
        for (Map.Entry<String, String> configEntry : inputData.entrySet()) {
            if (values.contains(configEntry.getKey())) {
                this.appFrame.getCONFIG().put(configEntry.getKey(), configEntry.getValue());
            }
        }
    }

    public void setConfigValue(String key, Object value){
        if(CONFIG.get(key) != null) {
            clearConfigData(Arrays.asList(key), false);
        }
        CONFIG.put(key, value);
    }

    public void clearConfigData(List<String> dataToClear, boolean write) {
        this.appFrame.getLOGGER().debug("Wiping "+dataToClear);
        for (String keyToWipe : dataToClear) {
            this.CONFIG.remove(keyToWipe);
        }
        if (write) {
            this.writeCurrentConfig();
        }
    }

    public void writeCurrentConfig() {
        this.appFrame.getLOGGER().debug("Writing "+ configToJSON());
        try (FileWriter fileWriter = new FileWriter(this.getFullPath() + File.separator + "config/config.json")) {
            fileWriter.write(configToJSON());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Map> getCfgMaps() {
        return getAllCfgMaps();
    }

    public String configToJSON() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(CONFIG);
    }

    public Map<String, Object> getCONFIG() {
        return CONFIG;
    }

    public AppFrame getAppFrame() {
        return appFrame;
    }

    @Override
    public String getFullPath() {
        return cfgProvider.getGameFullPath();
    }
}
