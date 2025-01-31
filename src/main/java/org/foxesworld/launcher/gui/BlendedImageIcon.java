package org.foxesworld.launcher.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BlendedImageIcon extends ImageIcon {
    private final BufferedImage combined;

    public BlendedImageIcon(BufferedImage img1, BufferedImage img2, float alpha) {
        combined = new BufferedImage(
                Math.max(img1.getWidth(), img2.getWidth()),
                Math.max(img1.getHeight(), img2.getHeight()),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = combined.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - alpha));
        g.drawImage(img1, 0, 0, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(img2, 0, 0, null);
        g.dispose();
    }

    @Override
    public Image getImage() {
        return combined;
    }
}