package org.foxesworld;

import org.foxesworld.engine.Engine;

import javax.swing.*;


public class APP {

    private String LOCALE;
    private String frameTpl = "assets/frames/frame.json";
    private String mainFrame = "assets/frames/mainFrame.json";
    private String localeFile = "/assets/lang/locale.json";
    private String engineVars = "engine.json";
    private String[] configFiles = new String[]{"config"};
    private static org.foxesworld.APP APP;
    public static void main(String[] args) {
        APP = new APP();
        System.setProperty("file.encoding", "UTF-8");
        SwingUtilities.invokeLater(() -> new Engine(APP));
    }
    public String getMainFrame() {
        return mainFrame;
    }
    public String getEngineVars() {
        return engineVars;
    }
    public String getLocaleFile() {
        return localeFile;
    }
    public String[] getConfigFiles() {
        return configFiles;
    }
    public String getLOCALE() {
        return LOCALE;
    }
    public String getFrameTpl() {
        return frameTpl;
    }
    public void setLOCALE(String LOCALE) {
        this.LOCALE = LOCALE;
    }
}
