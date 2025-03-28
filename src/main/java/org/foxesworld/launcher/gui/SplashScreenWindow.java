package org.foxesworld.launcher.gui;


import org.foxesworld.ascendix.Ascendix;
import org.foxesworld.ascendix.lottie.LottieLoader;

import javax.swing.*;
import java.awt.*;

public class SplashScreenWindow extends JWindow {
    private final Ascendix lottieSwingEngine;

    public SplashScreenWindow() {
        System.setProperty("log.dir", System.getProperty("user.dir"));
        System.setProperty("log.level", "INFO");
        lottieSwingEngine = new Ascendix(LottieLoader.loadAnimation("assets/animation.json"));

        getContentPane().add(lottieSwingEngine.getAnimationPanel());
        setSize(640, 360);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));
        lottieSwingEngine.getAnimationPanel().setOpaque(false);
        this.add(lottieSwingEngine.getAnimationPanel());
}

    public void showSplashScreen() {
        setVisible(true);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SplashScreenWindow splashScreen = new SplashScreenWindow();
            splashScreen.showSplashScreen();
        });
    }

    public Ascendix getLottieSwingEngine() {
        return lottieSwingEngine;
    }
}
