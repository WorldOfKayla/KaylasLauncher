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
        this.frameWidth = componentAttributes.getIconWidth();
        this.frameHeight = componentAttributes.getIconHeight();
        this.spriteSheet = ImageUtils.getLocalImage(componentAttributes.getImageIcon());
        this.totalFrames = componentAttributes.getTotalFrames();
        this.animationDelay = componentAttributes.getDelay();

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

        int scaledWidth = getWidth();  // Use the width of the component as the new width
        int scaledHeight = getHeight();  // Use the height of the component as the new height

        // Calculate the scaling factors
        double scaleX = (double) scaledWidth / frameWidth;
        double scaleY = (double) scaledHeight / frameHeight;

        // Draw the scaled image
        g.drawImage(
                ImageUtils.getByIndex(spriteSheet, 128, currentFrame),
                0,  // X-coordinate in the destination
                0,  // Y-coordinate in the destination
                scaledWidth,  // Width of the destination
                scaledHeight,  // Height of the destination
                this
        );

        g.dispose();
    }
}
