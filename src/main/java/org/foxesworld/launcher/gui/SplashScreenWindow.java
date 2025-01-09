package org.foxesworld.launcher.gui;

import org.foxesworld.animatix.AnimationFactory;
import org.foxesworld.animatix.animation.AnimationStatus;
import org.foxesworld.animatix.animation.config.AnimationConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class SplashScreenWindow extends JWindow {
    private AnimationFactory animationFactory;
    private final ImageIcon imageIcon;
    private final ImageIcon backgroundImage;
    private float opacity = 0.4f;
    private float scale = 0.8f;
    private final int fadeDuration = 600;
    private final int fadeInterval = 40;

    public SplashScreenWindow() {
        backgroundImage = new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("assets/ui/img/bg/launch.jpg")));
        imageIcon = new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("assets/ui/icons/fwBanner.png")));
        animationFactory = new AnimationFactory("bootAnimation.json5");

        SwingUtilities.invokeLater(() -> {

        });

        JPanel content = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                createBackgroundWithOverlayAndRoundedCorners(g, getWidth(), getHeight());

            }

            @Override
            public void setOpaque(boolean isOpaque) {
                super.setOpaque(false);

            }
        };


        getContentPane().add(content);
        setSize(600, 360);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));
        animationFactory.createAnimation(this);
    }

    private void createBackgroundWithOverlayAndRoundedCorners(Graphics g, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();
        Shape roundedRect = new RoundRectangle2D.Double(0, 0, width, height, 30, 30);
        g2d.setClip(roundedRect);
        g2d.drawImage(backgroundImage.getImage(), 0, 0, width, height, null);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2d.setColor(hexToColor("#1e201eeb"));
        g2d.fill(roundedRect);
        g2d.dispose();

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

    public AnimationFactory getAnimationFactory() {
        return animationFactory;
    }
}
