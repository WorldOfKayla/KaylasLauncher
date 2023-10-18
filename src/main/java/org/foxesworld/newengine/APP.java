package org.foxesworld.newengine;

import javax.swing.*;


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
