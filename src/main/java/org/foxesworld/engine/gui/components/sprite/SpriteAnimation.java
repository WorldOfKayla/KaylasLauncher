package org.foxesworld.engine.gui.components.sprite;

import org.foxesworld.engine.gui.components.ComponentAttributes;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SpriteAnimation extends JComponent {

    private BufferedImage spriteSheet;
    private int frameWidth;
    private int frameHeight;

    private int totalFrames;

    private int animationDelay;
    private int currentFrame = 0;


    public SpriteAnimation(ComponentAttributes componentAttributes) {
        this.frameWidth = componentAttributes.iconWidth;
        this.frameHeight = componentAttributes.iconHeight;
        this.spriteSheet = ImageUtils.getLocalImage(componentAttributes.imageIcon);
        this.totalFrames = componentAttributes.totalFrames;
        this.animationDelay = componentAttributes.delay;
        //this.setPreferredSize(new Dimension(frameWidth, frameHeight));

        Timer timer = new Timer(animationDelay, e -> {
            currentFrame = (currentFrame + 1) % totalFrames;
            repaint();
            revalidate();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int k = 128;
        g.drawImage(ImageUtils.getByIndex(spriteSheet, k, currentFrame), this.getWidth() / 2 - frameWidth, this.getHeight() / 2 - frameHeight, frameWidth, frameHeight, this);
        g.dispose();
    }
}
