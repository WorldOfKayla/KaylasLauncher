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
    private static org.foxesworld.newengine.APP APP;
    //private InputStream langFile = org.foxesworld.newengine.APP.class.getClassLoader().getResourceAsStream("assets/lang/locale.json");


    public static void main(String[] args) {


        //System.setProperty("log4j.saveDirectory", configReader.getFullPath());
        //LOGGER.info("APP started...");

        APP = new APP();
        SwingUtilities.invokeLater(() -> {
            new AppFrame(APP);
        });
    }
}
