package org.foxesworld.engine.config;

import com.foxesworld.cfgProvider.cfgProvider;
import com.google.gson.GsonBuilder;
import org.foxesworld.engine.Engine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Config extends ConfigAbstract {
    private Engine engine;
    private Map<String, Object> CONFIG;

    private int selectedServer, gpuIndex;
    private double volume;
    private  String login, password, lang, logLevel, ramAmount;
    private  boolean autoEnter, fullScreen, loadNews, enableSound;

    public Config(Engine engine) {
        this.engine = engine;
        setCfgExportDir("config/");
        setDebug(true);
        setDirPathIndex(3);
        setCfgFileExtension(".json");
        cfgProvider.setDefaultConfFilesDir("config/");
        addCfgFiles(engine.getConfigFiles());
        this.CONFIG = getCfgMaps().get("config");
        this.assignConfigValues();
    }
    public void addToConfig(Map<String, String> inputData, List values) {
        for (Map.Entry<String, String> configEntry : inputData.entrySet()) {
            if (values.contains(configEntry.getKey())) {
                this.getCONFIG().put(configEntry.getKey(), configEntry.getValue());
            }
        }
    }
    public void setConfigValue(String key, Object value){
        if(CONFIG.get(key) != null) {
            clearConfigData(Arrays.asList(key), false);
        }
        CONFIG.put(key, value);
        assignConfigValues();
    }

    public void clearConfigData(List<String> dataToClear, boolean write) {
        this.engine.getLOGGER().debug("Wiping "+dataToClear);
        for (String keyToWipe : dataToClear) {
            this.CONFIG.remove(keyToWipe);
        }
        if (write) {
            this.writeCurrentConfig();
        }
    }
    public void clearConfigData(String dataToClear, boolean write) {
        this.engine.getLOGGER().debug("Wiping "+dataToClear);
            this.CONFIG.remove(dataToClear);
        if (write) {
            this.writeCurrentConfig();
        }
    }
    private void assignConfigValues(){
        for(Map.Entry<String, Object> configMap : this.CONFIG.entrySet()){
            try {
                Field field = Config.class.getDeclaredField(configMap.getKey());
                if(field.hashCode()!= 0) {
                    field.set(this, configMap.getValue());
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void writeCurrentConfig() {
        this.engine.getLOGGER().debug("Writing "+ configToJSON());
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
    @Override
    public String getFullPath() {
        return cfgProvider.getGameFullPath();
    }
    public String getLogin() {
        return login;
    }
    public String getPassword() {
        return password;
    }
    public String getLang() {
        return lang;
    }
    public String getLogLevel() {
        return logLevel;
    }
    public String getRamAmount() {
        return ramAmount;
    }
    public double getVolume() {
        return volume;
    }
    public boolean isAutoEnter() {
        return autoEnter;
    }
    public boolean isFullScreen() {
        return fullScreen;
    }
    public boolean isLoadNews() {
        return loadNews;
    }
    public boolean isEnableSound() {
        return enableSound;
    }
    public void setVolume(double volume) {
        this.volume = volume;
    }
    public int getSelectedServer() {
        return selectedServer;
    }
}
