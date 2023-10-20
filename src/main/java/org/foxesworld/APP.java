package org.foxesworld;

import org.foxesworld.engine.Engine;

import javax.swing.*;


public class APP {

    private String mainFrame = "assets/frames/mainFrame.json";
    private String localeFile = "/assets/lang/locale.json";
    private String engineVars = "engine.json";
    private static org.foxesworld.APP APP;
    public static void main(String[] args) {
        APP = new APP();
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
}
