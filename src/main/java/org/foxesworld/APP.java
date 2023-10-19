package org.foxesworld;

import org.foxesworld.engine.AppFrame;

import javax.swing.*;


public class APP {

    private String mainFrame = "assets/frames/mainFrame.json";
    private static org.foxesworld.APP APP;
    public static void main(String[] args) {
        APP = new APP();
        SwingUtilities.invokeLater(() -> new AppFrame(APP));
    }

    public String getMainFrame() {
        return mainFrame;
    }
}
