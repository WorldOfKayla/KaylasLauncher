package org.foxesworld;

import org.foxesworld.engine.Engine;

import javax.swing.*;


public class APP {
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        SwingUtilities.invokeLater(() -> new Engine("init.json"));
    }
}
