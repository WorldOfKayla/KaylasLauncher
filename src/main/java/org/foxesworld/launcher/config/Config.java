package org.foxesworld.launcher.config;

import com.google.gson.GsonBuilder;
import org.foxesworld.cfgProvider.CfgProvider;
import org.foxesworld.engine.Engine;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class Config extends org.foxesworld.engine.config.Config {

    private int selectedServer, lang, loaderIndex;
    private double volume;
    private int width, height, ramAmount;
    private String login, password;
    private boolean autoEnter, fullScreen, loadNews, enableSound, launchAC, backgroundMusic, discordRPC;

    public Config(Engine engine) {
        setCfgExportDir("cache/config/");
        setDirPathIndex(3);
        setCfgFileExtension(".json");
        CfgProvider.setDefaultConfFilesDir("config/");
        CfgProvider.setLOGGER(Engine.LOGGER);
        addCfgFiles(engine.getConfigFiles());
        this.config = getCfgMaps().get("config");
        this.assignConfigValues();
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
        if (config.get(key) != null) {
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
        Iterator<Map.Entry<String, Object>> iterator = config.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> configEntry = iterator.next();
            String key = configEntry.getKey();
            Object value = configEntry.getValue();

            try {
                Field field = Config.class.getDeclaredField(key);
                field.setAccessible(true);
                Object castedValue = castValue(field.getType(), value);

                if (isValidValue(field.getType(), castedValue)) {
                    field.set(this, castedValue);
                } else {
                    handleInvalidValue(field, key, iterator);
                }
            } catch (NoSuchFieldException e) {
                Engine.LOGGER.warn("Removing '" + key + "' as it doesn't exist!");
                iterator.remove();
            } catch (IllegalAccessException e) {
                Engine.LOGGER.error("Failed to access field " + key + " in Config class!");
            } catch (IllegalArgumentException e) {
                Engine.LOGGER.error("Failed to assign value to field " + key + " in Config class! Type mismatch.");
            }
        }

        // Second pass: Ensure all fields in the Config class have a value in the config map
        for (Field field : Config.class.getDeclaredFields()) {
            String fieldName = field.getName();
            if (!config.containsKey(fieldName)) {
                try {
                    field.setAccessible(true);
                    Object defaultValue = field.get(this);
                    config.put(fieldName, defaultValue);
                    Engine.LOGGER.info("Adding default value for '" + fieldName + "' to config.");
                } catch (IllegalAccessException e) {
                    Engine.LOGGER.error("Failed to access field " + fieldName + " in Config class!");
                }
            }
        }

        this.writeCurrentConfig();
    }

    private void handleInvalidValue(Field field, String key, Iterator<Map.Entry<String, Object>> iterator) {
        try {
            Object defaultValue = field.get(this);
            config.put(key, defaultValue);
            Engine.LOGGER.warn("Invalid value for '" + key + "', setting default: " + defaultValue);
            field.set(this, defaultValue);
        } catch (IllegalAccessException e) {
            Engine.LOGGER.error("Failed to access field " + key + " in Config class!");
        }
    }


    private boolean isValidValue(Class<?> fieldType, Object value) {
        if (value == null) {
            return false;
        }

        if (fieldType == boolean.class || fieldType == Boolean.class) {
            return value instanceof Boolean;
        } else if (fieldType == int.class || fieldType == Integer.class) {
            return value instanceof Integer || (value instanceof String && isInteger((String) value));
        } else if (fieldType == double.class || fieldType == Double.class) {
            return value instanceof Double || (value instanceof String && isDouble((String) value));
        } else if (fieldType == float.class || fieldType == Float.class) {
            return value instanceof Float || (value instanceof String && isFloat((String) value));
        } else if (fieldType == long.class || fieldType == Long.class) {
            return value instanceof Long || (value instanceof String && isLong((String) value));
        } else if (fieldType == String.class) {
            return value instanceof String;
        }

        return false;
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isFloat(String value) {
        try {
            Float.parseFloat(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isLong(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Object castValue(Class<?> fieldType, Object value) {
        if (value == null) {
            return null;
        }

        if (fieldType == boolean.class || fieldType == Boolean.class) {
            return Boolean.parseBoolean(value.toString());
        } else if (fieldType == int.class || fieldType == Integer.class) {
            return Integer.parseInt(value.toString());
        } else if (fieldType == double.class || fieldType == Double.class) {
            return Double.parseDouble(value.toString());
        } else if (fieldType == float.class || fieldType == Float.class) {
            return Float.parseFloat(value.toString());
        } else if (fieldType == long.class || fieldType == Long.class) {
            return Long.parseLong(value.toString());
        } else if (fieldType == String.class) {
            return value.toString();
        }

        throw new IllegalArgumentException("Unsupported field type: " + fieldType);
    }

    @Override
    public void writeCurrentConfig() {
        Engine.getLOGGER().debug("Writing Config");
        try (FileWriter fileWriter = new FileWriter(getFullPath() + "cache/config/config.json")) {
            fileWriter.write(configToJSON());
        } catch (IOException e) {
            e.printStackTrace();
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

    public boolean isDiscordRPC() {
        return discordRPC;
    }
}