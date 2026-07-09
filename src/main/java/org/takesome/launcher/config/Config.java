package org.takesome.launcher.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.foxesworld.cfgProvider.annotations.ConfigKey;
import org.takesome.kaylasEngine.Engine;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("unused")
public class Config extends org.takesome.kaylasEngine.config.Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> NON_CONFIG_FIELDS = Set.of("GSON", "NON_CONFIG_FIELDS");
    private static final String DEFAULT_HOME_DIR = "SysVal{AppDir}\\.KineticHorizons";
    private static final String LEGACY_HOME_SUFFIX = ".Foxes" + "World";

    private int selectedServer = 0;
    private int lang = 0;
    private int loaderIndex = 0;
    private int width = 900;
    private int height = 512;
    private int ramAmount = 4096;
    private double volume = 85.0d;
    private boolean enableSound = true;
    private boolean backgroundMusic = true;

    @ConfigKey("logs.level")
    private String logLevel = "DEBUG";

    private String login = "";
    private String password = "";
    private String homeDir = DEFAULT_HOME_DIR;
    private String authProvider = "WS";
    private String authEndpoint = "";

    private boolean autoEnter = false;
    private boolean fullScreen = false;
    private boolean loadNews = true;
    private boolean launchAC = true;
    private boolean discordRPC = true;
    private boolean backendBinding = true;
    private String backendWsUrl = "ws://127.0.0.1:18080/ws/launcher";
    private int backendHeartbeatSeconds = 15;
    private int backendMaxReconnectAttempts = 0;

    public Config(Engine engine) {
        super(engine.getConfigFiles(), Engine.LOGGER);
    }

    @Override
    public void processConfig() {
        initConfig("config", this.getClass());
    }

    @Override
    public void initConfig(String cfgName, Class<?> clazz) {
        configLock.writeLock().lock();
        try {
            Map<String, Object> defaults = buildDefaultsFromFields();
            Path configFile = getConfigFile();
            Files.createDirectories(configFile.getParent());

            Map<String, Object> loaded = readConfigFile(configFile);
            boolean configCreated = loaded.isEmpty() && !Files.exists(configFile);
            boolean updated = migrateLegacyHomeDir(loaded);

            for (Map.Entry<String, Object> entry : defaults.entrySet()) {
                if (!loaded.containsKey(entry.getKey())) {
                    loaded.put(entry.getKey(), entry.getValue());
                    updated = true;
                    Engine.getLOGGER().info("Added missing config entry from field default: {} -> {}", entry.getKey(), entry.getValue());
                }
            }

            config.clear();
            config.putAll(loaded);
            assignConfigValues(clazz);

            if (configCreated || updated) {
                writeCurrentConfig();
                Engine.getLOGGER().info("Launcher config {} at {}", configCreated ? "created" : "updated", configFile);
            }
        } catch (Exception e) {
            Engine.LOGGER.error("Launcher self-contained config init error", e);
            config.clear();
            config.putAll(buildDefaultsFromFieldsSafe());
            assignConfigValues(clazz);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    @Override
    public void addToConfig(Map<String, Object> inputData, List<?> values) {
        inputData.forEach((key, value) -> {
            if (values.contains(key)) {
                this.config.put(key, value);
            }
        });
    }

    @Override
    public void setConfigValue(String key, Object value) {
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
        configLock.readLock().lock();
        try {
            Path configFile = getConfigFile();
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                writer.write(configToJSON());
            }
        } catch (IOException e) {
            Engine.LOGGER.error("Error writing launcher config file.", e);
        } finally {
            configLock.readLock().unlock();
        }
    }

    @Override
    public String getFullPath() {
        String resolvedHome = resolvePlaceholders(homeDir);
        if (resolvedHome == null || resolvedHome.isBlank()) {
            resolvedHome = Path.of(System.getProperty("user.home", "."), ".KineticHorizons").toString();
        }
        return Path.of(resolvedHome).toAbsolutePath().normalize().toString() + java.io.File.separator;
    }

    private Path getConfigFile() {
        return Path.of(getFullPath(), "cache", "config", "config.json").toAbsolutePath().normalize();
    }

    private Map<String, Object> readConfigFile(Path configFile) {
        if (!Files.exists(configFile)) {
            return new LinkedHashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            Map<String, Object> loaded = GSON.fromJson(reader, new TypeToken<LinkedHashMap<String, Object>>() {}.getType());
            return loaded == null ? new LinkedHashMap<>() : new LinkedHashMap<>(loaded);
        } catch (Exception e) {
            Engine.LOGGER.error("Error reading launcher config file {}; defaults will be used", configFile, e);
            return new LinkedHashMap<>();
        }
    }

    private boolean migrateLegacyHomeDir(Map<String, Object> loaded) {
        Object loadedHomeDir = loaded.get("homeDir");
        if (!(loadedHomeDir instanceof String text)) {
            return false;
        }
        if (!text.contains(LEGACY_HOME_SUFFIX)) {
            return false;
        }
        loaded.put("homeDir", text.replace(LEGACY_HOME_SUFFIX, ".KineticHorizons"));
        Engine.getLOGGER().info("Migrated launcher homeDir to {}", DEFAULT_HOME_DIR);
        return true;
    }

    private Map<String, Object> buildDefaultsFromFieldsSafe() {
        try {
            return buildDefaultsFromFields();
        } catch (Exception e) {
            Engine.LOGGER.error("Could not build default launcher config from fields", e);
            return new LinkedHashMap<>();
        }
    }

    private Map<String, Object> buildDefaultsFromFields() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        for (Field field : getClass().getDeclaredFields()) {
            if (!isConfigField(field)) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(this);
                defaults.put(field.getName(), value);
            } catch (IllegalAccessException e) {
                Engine.LOGGER.warn("Could not read default config field: {}", field.getName(), e);
            }
        }
        return defaults;
    }

    private boolean isConfigField(Field field) {
        int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers)
                && !Modifier.isTransient(modifiers)
                && !NON_CONFIG_FIELDS.contains(field.getName());
    }

    private String resolvePlaceholders(String input) {
        if (input == null) {
            return null;
        }
        String appDir = Objects.toString(System.getProperty("AppDir", System.getenv("APPDATA")), "");
        return input.replace("SysVal{AppDir}", appDir);
    }

    public Map<String, Map<String, Object>> getAllCfgMaps() {
        return Map.of("config", config);
    }

    @Override
    public String configToJSON() {
        return GSON.toJson(config);
    }

    @Override
    public Map<String, Object> getConfig() {
        return config;
    }

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
    public boolean isBackendBinding() { return backendBinding; }
    public String getBackendWsUrl() { return backendWsUrl; }
    public int getBackendHeartbeatSeconds() { return backendHeartbeatSeconds; }
    public int getBackendMaxReconnectAttempts() { return backendMaxReconnectAttempts; }
    public int getSelectedServer() { return selectedServer; }
    public int getLoaderIndex() { return loaderIndex; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getHomeDir() { return homeDir; }
    public String getAuthProvider() { return authProvider; }
    public String getAuthEndpoint() { return authEndpoint; }
    public String getLogLevel() { return logLevel; }

    public static class LogsConfig {
        private String level;

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }
    }

    public void setVolume(double volume) {
        this.volume = volume;
        setConfigValue("volume", volume);
    }
}
