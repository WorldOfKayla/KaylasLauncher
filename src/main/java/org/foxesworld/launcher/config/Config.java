package org.foxesworld.launcher.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.foxesworld.engine.Engine;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("unused")
public class Config extends org.foxesworld.engine.config.Config {

    private int selectedServer, lang, loaderIndex, width, height, ramAmount;
    private double volume;
    private String login, password, homeDir;
    private boolean autoEnter, fullScreen, loadNews, enableSound, launchAC, backgroundMusic, discordRPC;

    public Config(Engine engine) {
        super(engine.getConfigFiles(), Engine.LOGGER);
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
        assignConfigValues(this.getClass());
    }

    @Override
    public void clearConfigData(List<String> dataToClear, boolean write) {
        Engine.getLOGGER().debug("Clearing data: " + dataToClear);
        dataToClear.forEach(config::remove);
        if (write) {
            writeCurrentConfig();
        }
    }

    @Override
    public void clearConfigData(String dataToClear, boolean write) {
        Engine.getLOGGER().debug("Clearing data: " + dataToClear);
        config.remove(dataToClear);
        if (write) {
            writeCurrentConfig();
        }
    }

    @Override
    public void writeCurrentConfig() {
        Engine.getLOGGER().debug("Writing config to file.");
        try (FileWriter writer = new FileWriter(getFullPath() + "cache/config/config.json")) {
            writer.write(configToJSON());
        } catch (IOException e) {
            Engine.LOGGER.error("Error writing config file.", e);
        }
    }

    public Map<String, Map<String, Object>> getAllCfgMaps() {
        return super.cfgProvider.getAllCfgMaps();
    }

    @Override
    public String configToJSON() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(config);
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    // Getters
    public String getLogin() { return login; }
    public String getPassword() { return password; }
    public int getLang() { return lang; }
    public int getRamAmount() { return ramAmount; }
    public double getVolume() { return volume; }
    public boolean isAutoEnter() { return autoEnter; }
    public boolean isFullScreen() { return fullScreen; }
    public boolean isLoadNews() { return loadNews; }
    public boolean isEnableSound() { return enableSound; }
    public boolean isLaunchAC() { return launchAC; }
    public boolean isBackgroundMusic() { return backgroundMusic; }
    public boolean isDiscordRPC() { return discordRPC; }
    public int getSelectedServer() { return selectedServer; }
    public int getLoaderIndex() { return loaderIndex; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getHomeDir() { return homeDir; }

    // Setters
    public void setVolume(double volume) {
        this.volume = volume;
        setConfigValue("volume", volume);
    }
}