package org.foxesworld.launcher.config;

import com.google.gson.GsonBuilder;
import org.foxesworld.cfgProvider.CfgProvider;
import org.foxesworld.engine.Engine;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("unused")
public class Config extends org.foxesworld.engine.config.Config {

    private int selectedServer, lang, loaderIndex;
    private double volume;
    private int width, height, ramAmount;
    private String login, password, homeDir;
    private boolean autoEnter, fullScreen, loadNews, enableSound, launchAC, backgroundMusic, discordRPC;

    public Config(Engine engine) {
        super();
        initConfig(engine);
    }

    private void initConfig(Engine engine) {
        setCfgExportDir("cache/config/");
        setDirPathIndex(3);
        setCfgFileExtension(".json");
        CfgProvider.setDefaultConfFilesDir("config/");
        CfgProvider.setLOGGER(Engine.LOGGER);
        addCfgFiles(engine.getConfigFiles());
        this.config = getCfgMaps().get("config");
        assignConfigValues();
    }

    @Override
    public void addToConfig(Map<String, Object> inputData, List<?> values) {
        inputData.forEach((key, value) -> {
            if (values.contains(key)) {
                this.getConfig().put(key, value);
            }
        });
    }

    @Override
    public void setConfigValue(String key, Object value) {
        if (config.containsKey(key)) {
            clearConfigData(Collections.singletonList(key), false);
        }
        config.put(key, value);
        assignConfigValues();
    }

    @Override
    public void clearConfigData(List<String> dataToClear, boolean write) {
        Engine.getLOGGER().debug("Wiping " + dataToClear);
        dataToClear.forEach(config::remove);
        if (write) {
            writeCurrentConfig();
        }
    }

    @Override
    public void clearConfigData(String dataToClear, boolean write) {
        Engine.getLOGGER().debug("Wiping " + dataToClear);
        config.remove(dataToClear);
        if (write) {
            writeCurrentConfig();
        }
    }

    @Override
    public void assignConfigValues() {
        for (Field field : Config.class.getDeclaredFields()) {
            String fieldName = field.getName();
            if (config.containsKey(fieldName)) {
                processConfigField(field, fieldName);
            } else {
                setDefaultValueForField(field);
            }
        }
        this.writeCurrentConfig();
    }

    @Override
    public void writeCurrentConfig() {
        Engine.getLOGGER().debug("Writing Config");
        try (FileWriter fileWriter = new FileWriter(getFullPath() + "cache/config/config.json")) {
            fileWriter.write(configToJSON());
        } catch (IOException e) {
            Engine.LOGGER.error("Error writing config file", e);
        }
    }

    public Map<String, Map<String, Object>> getCfgMaps() {
        return getAllCfgMaps();
    }

    public String configToJSON() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(config);
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public int getLang() {
        return lang;
    }

    public int getRamAmount() {
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

    public boolean isLaunchAC() {
        return launchAC;
    }

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

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getHomeDir() {
        return homeDir;
    }

    public boolean isDiscordRPC() {
        return discordRPC;
    }
}