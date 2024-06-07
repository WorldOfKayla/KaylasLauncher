package org.foxesworld.launcher.config;

import com.google.gson.GsonBuilder;
import org.foxesworld.cfgProvider.CfgProvider;
import org.foxesworld.engine.Engine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Config extends org.foxesworld.engine.config.Config {
    @SuppressWarnings("unused")
    private int selectedServer, loaderIndex;
    private double volume;
    @SuppressWarnings("unused")
    private int ramAmount;
    @SuppressWarnings("unused")
    private Object width, height;
    @SuppressWarnings("unused")
    private String login, password, lang;
    @SuppressWarnings("unused")
    private boolean autoEnter, fullScreen, loadNews, enableSound, launchAC, backgroundMusic;

    public Config(Engine engine) {
        setCfgExportDir("cache/config/");
        setDirPathIndex(3);
        setCfgFileExtension(".json");
        CfgProvider.setDefaultConfFilesDir("config/");
        CfgProvider.setLOGGER(Engine.LOGGER);
        addCfgFiles(engine.getConfigFiles());
        this.CONFIG = getCfgMaps().get("config");
        this.assignConfigValues();
    }
    @Override
    public void addToConfig(Map<String, String> inputData, List values) {
        for (Map.Entry<String, String> configEntry : inputData.entrySet()) {
            if (values.contains(configEntry.getKey())) {
                this.getCONFIG().put(configEntry.getKey(), configEntry.getValue());
            }
        }
    }
    @Override
    public void setConfigValue(String key, Object value){
        if(CONFIG.get(key) != null) {
            clearConfigData(Collections.singletonList(key), false);
        }
        CONFIG.put(key, value);
        assignConfigValues();
    }

    @Override
    public void clearConfigData(List<String> dataToClear, boolean write) {
        Engine.getLOGGER().debug("Wiping "+dataToClear);
        for (String keyToWipe : dataToClear) {
            this.CONFIG.remove(keyToWipe);
        }
        if (write) {
            this.writeCurrentConfig();
        }
    }
    @Override
    public void clearConfigData(String dataToClear, boolean write) {
        Engine.getLOGGER().debug("Wiping "+dataToClear);
        this.CONFIG.remove(dataToClear);
        if (write) {
            this.writeCurrentConfig();
        }
    }
    @Override
    public void assignConfigValues() {
        Iterator<Map.Entry<String, Object>> iterator = CONFIG.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> configMap = iterator.next();
            try {
                Field field = Config.class.getDeclaredField(configMap.getKey());
                field.setAccessible(true);
                field.set(this, configMap.getValue());
            } catch (NoSuchFieldException e) {
                Engine.LOGGER.warn("Removing '" + configMap.getKey() + "' as it doesn't exist!");
                iterator.remove();
                this.writeCurrentConfig();
            } catch (IllegalAccessException e) {
                Engine.LOGGER.error("Failed to access field " + configMap.getKey() + " in Config class!");
            }
        }

        for (Field field : Config.class.getDeclaredFields()) {
            String fieldName = field.getName();
            if (!CONFIG.containsKey(fieldName)) {
                try {
                    Object defaultValue = field.get(this);
                    CONFIG.put(fieldName, defaultValue);
                    Engine.LOGGER.info("Adding default value for '" + fieldName + "' to config.");
                } catch (IllegalAccessException e) {
                    Engine.LOGGER.error("Failed to access field " + fieldName + " in Config class!");
                }
            }
        }

        this.writeCurrentConfig();
    }


    @Override
    public void writeCurrentConfig() {
        Engine.getLOGGER().debug("Writing Config");
        try (FileWriter fileWriter = new FileWriter(getFullPath() + File.separator + "cache/config/config.json")) {
            fileWriter.write(configToJSON());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public Map<String, Map<String, Object>> getCfgMaps() {
        return getAllCfgMaps();
    }
    public String configToJSON() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(CONFIG);
    }
    public Map<String, Object> getCONFIG() {
        return CONFIG;
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

    public int getRamAmount() {
        return ramAmount;
    }

    @SuppressWarnings("unused")
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

    public boolean isLaunchAC() {
        return launchAC;
    }

    @SuppressWarnings("unused")
    public boolean isEnableSound() {
        return enableSound;
    }

    public boolean isBackgroundMusic() {
        return backgroundMusic;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public int getSelectedServer() {
        return selectedServer;
    }

    public int getLoaderIndex() {
        return loaderIndex;
    }

    public Object getWidth() {
        return width;
    }

    public Object getHeight() {
        return height;
    }

}