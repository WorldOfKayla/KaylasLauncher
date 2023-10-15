package org.foxesworld.newengine.gui.components.multiButton;

import org.foxesworld.newengine.gui.styles.StyleProvider;
import org.foxesworld.newengine.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class MultiButtonStyle {
    private BufferedImage[] splitImg;
    public int width;
    public int height;

    public MultiButtonStyle(StyleProvider.StyleAttributes style, int rowNum) {
        this.width = style.width;
        this.height = style.height;
        this.splitImg = splitImage(rowNum, ImageUtils.getLocalImage(style.texture));
    }

    public void apply(MultiButton multiButton) {
        multiButton.img1 = this.splitImg[0];
        multiButton.img2 = this.splitImg[1];
        multiButton.img3 = this.splitImg[2];
        multiButton.setVisible(true);
    }


    public BufferedImage[] splitImage(int row, BufferedImage texture) {
        int imgCount = 3;
        int width = texture.getWidth() / imgCount;
        int height = texture.getHeight() / 2;

        BufferedImage[] images = new BufferedImage[imgCount];

        for (int i = 0; i < imgCount; i++) {
            int xPos = i * width;
            int yPos = row * height; // Vertical position
            BufferedImage subImage = texture.getSubimage(xPos, yPos, width, height);
            images[i] = subImage;
        }

        return images;
    }

}

