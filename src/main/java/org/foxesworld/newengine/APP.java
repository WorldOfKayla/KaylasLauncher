package org.foxesworld.newengine;

import org.foxesworld.newengine.gui.AppFrame;
import org.foxesworld.newengine.locale.LanguageProvier;
import org.foxesworld.newengine.utils.ImageUtils;

import javax.swing.*;
import java.io.InputStream;


public class APP {
    private static org.foxesworld.newengine.APP APP;
    private static LanguageProvier LANG;
    private String LOCALE = "ru";
    private static ImageUtils IMAGEUTILS;
    private InputStream langFile = org.foxesworld.newengine.APP.class.getClassLoader().getResourceAsStream("locale.json");


    public static void main(String[] args) {
        APP = new APP();
        LANG = new LanguageProvier(APP);
        IMAGEUTILS = new ImageUtils(APP);

        SwingUtilities.invokeLater(() -> {
            AppFrame appFrame = new AppFrame(APP);
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

    public ImageUtils getIMAGEUTILS() {
        return IMAGEUTILS;
    }
}
