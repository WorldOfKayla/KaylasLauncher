package org.foxesworld.newengine.gui.components.multiButton;

import org.foxesworld.newengine.gui.attributes.ComponentAttributes;
import org.foxesworld.newengine.gui.styles.StyleProvider;
import org.foxesworld.newengine.utils.ImageUtils;

import java.awt.image.BufferedImage;

public class MultiButtonStyle {
    private BufferedImage[] splitImg;
    public int width;
    public int height;

    public MultiButtonStyle(StyleProvider.StyleAttributes style, ComponentAttributes componentAttributes) {
        this.width = style.width;
        this.height = style.height;
        this.splitImg = splitImage(componentAttributes.rowNum, componentAttributes.imgCount, ImageUtils.getLocalImage(style.texture));
    }

    public void apply(MultiButton multiButton) {
        for(BufferedImage img: this.splitImg){
            multiButton.img.add(img);
        }
        //multiButton.img1 = this.splitImg[0];
        //multiButton.img2 = this.splitImg[1];
        //multiButton.img3 = this.splitImg[2];
    }


    public BufferedImage[] splitImage(int row, int imgCount, BufferedImage texture) {
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

