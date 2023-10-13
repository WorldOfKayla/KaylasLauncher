package org.foxesworld.newengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.foxesworld.newengine.locale.LanguageProvier;

import javax.swing.*;
import java.io.InputStream;


public class APP {
    private static org.foxesworld.newengine.APP APP;
    private static LanguageProvier LANG;
    public static final Logger LOGGER = LogManager.getLogger(APP.class);
    private final String LOCALE = "ru";
    private InputStream langFile = org.foxesworld.newengine.APP.class.getClassLoader().getResourceAsStream("assets/lang/locale.json");


    public static void main(String[] args) {
        LOGGER.info("APP started...");
        APP = new APP();
        LANG = new LanguageProvier(APP);
        SwingUtilities.invokeLater(() -> {
            new AppFrame(APP);
        });
    }

    public String getLOCALE() {
        return LOCALE;
    }
    public LanguageProvier getLANG() {
        return LANG;
    }
    public InputStream getLangFile() {
        return langFile;
    }
}
