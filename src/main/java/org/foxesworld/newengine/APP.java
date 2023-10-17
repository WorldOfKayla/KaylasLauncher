package org.foxesworld.newengine;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.foxesworld.newengine.config.ConfigReader;
import org.foxesworld.newengine.locale.LanguageProvider;
import org.foxesworld.newengine.locale.LanguageProvider;

import javax.swing.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class APP {
    public static Map<String, Object> CONFIG = new HashMap<>();
    public static final Logger LOGGER = LogManager.getLogger(APP.class);
    private static org.foxesworld.newengine.APP APP;
    private static LanguageProvider LANG;
    private static String LOCALE = "ru";
    private InputStream langFile = org.foxesworld.newengine.APP.class.getClassLoader().getResourceAsStream("assets/lang/locale.json");


    public static void main(String[] args) {
        ConfigReader configReader = new ConfigReader(new String[]{"config"});
        CONFIG = configReader.getCfgMaps().get("config");
        Configurator.setLevel(LOGGER.getName(), Level.valueOf((String) CONFIG.get("LogLevel")));
        LOCALE = String.valueOf(CONFIG.get("Lang"));
        LOGGER.info("APP started...");

        APP = new APP();
        LANG = new LanguageProvider(APP, "/assets/lang/locale.json");
        SwingUtilities.invokeLater(() -> {
            new AppFrame(APP);
        });
    }

    public String getLOCALE() {
        return LOCALE;
    }
    public LanguageProvider getLANG() {
        return LANG;
    }
    public InputStream getLangFile() {
        return langFile;
    }
}
