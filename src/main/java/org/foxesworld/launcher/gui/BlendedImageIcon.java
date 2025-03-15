package org.foxesworld.launcher.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BlendedImageIcon extends ImageIcon {
    public BlendedImageIcon(Image img1, Image img2, float alpha) {
        super(blendImages(img1, img2, alpha));
    }

    private static Image blendImages(Image img1, Image img2, float alpha) {
        BufferedImage bImg1 = toBufferedImage(img1);
        BufferedImage bImg2 = toBufferedImage(img2);

        int width = Math.max(bImg1.getWidth(), bImg2.getWidth());
        int height = Math.max(bImg1.getHeight(), bImg2.getHeight());

        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - alpha));
        g.drawImage(bImg1, 0, 0, null);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(bImg2, 0, 0, null);
        g.dispose();

        return combined;
    }

    private static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        BufferedImage bImage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bImage.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return bImage;
    }
}
