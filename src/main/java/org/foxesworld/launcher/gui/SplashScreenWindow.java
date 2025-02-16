package org.foxesworld.launcher.gui;


import org.foxesworld.ascendix.LottieSwingEngine;
import org.foxesworld.ascendix.lottie.AnimationConfig;
import org.foxesworld.ascendix.lottie.LottieLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class SplashScreenWindow extends JWindow {
    private LottieSwingEngine lottieSwingEngine;
    private AnimationConfig config;

    public SplashScreenWindow() {
        config = new AnimationConfig();
        config.setFps(144);
        config.setLoop(false);
        config.setBackgroundColor(Color.WHITE);
        config.setScale(1.0f);
        config.setTotalFrames(256);
        lottieSwingEngine = new LottieSwingEngine(LottieLoader.loadAnimation(SplashScreenWindow.class.getClassLoader().getResourceAsStream("assets/animation.json")), config);

        getContentPane().add(lottieSwingEngine.getAnimationPanel());
        setSize(600, 360);
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
}
