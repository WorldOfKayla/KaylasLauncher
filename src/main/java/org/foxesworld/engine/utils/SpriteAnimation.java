package org.foxesworld.engine.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public final class SpriteAnimation extends JComponent {
    private final BufferedImage spriteSheet;
    private final int rows, columns;
    private int currentFrame = 0;
    private final Rectangle spriteRect;

    public SpriteAnimation(String path, int rows, int columns, int delay, Rectangle spriteRect) {
        this.spriteSheet = ImageUtils.getLocalImage(path);
        this.rows = rows;
        this.columns = columns;
        this.spriteRect = spriteRect;

        Timer timer = new Timer(delay, e -> {
            currentFrame = (currentFrame + 1) % (rows * columns);
            repaint();
        });
        timer.start();
    }

    public Rectangle getSpriteRect() {
        return spriteRect;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int scaledWidth = getWidth();
        int scaledHeight = getHeight();

        int frameWidth = spriteSheet.getWidth() / columns;
        int frameHeight = spriteSheet.getHeight() / rows;

        int row = currentFrame / columns;
        int column = currentFrame % columns;

        g.drawImage(
                spriteSheet.getSubimage(column * frameWidth, row * frameHeight, frameWidth, frameHeight),
                0,
                0,
                scaledWidth,
                scaledHeight,
                this
        );
    }
}
