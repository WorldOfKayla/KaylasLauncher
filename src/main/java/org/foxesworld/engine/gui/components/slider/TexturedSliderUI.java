package org.foxesworld.engine.gui.components.slider;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;

public class TexturedSliderUI extends BasicSliderUI {
    private ImageIcon thumbImage;
    private ImageIcon trackImage;

    public TexturedSliderUI(JSlider slider, String thumbImage, String trackImage) {
        super(slider);
        this.thumbImage = new ImageIcon(ImageUtils.getScaledImage(ImageUtils.getLocalImage(thumbImage), 14, 14));
        this.trackImage = new ImageIcon(ImageUtils.getScaledImage(ImageUtils.getLocalImage(trackImage), slider.getWidth(), 8));
    }

    @Override
    public void paintThumb(Graphics g) {
        Rectangle knobBounds = thumbRect;
        int w = thumbImage.getIconWidth();
        int h = thumbImage.getIconHeight();

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.drawImage(thumbImage.getImage(), knobBounds.x, knobBounds.y, w, h, null);
        g2d.dispose();
    }

    @Override
    public void paintTrack(Graphics g) {
        Rectangle trackBounds = trackRect;
        int w = trackImage.getIconWidth();
        int h = trackImage.getIconHeight();

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.drawImage(trackImage.getImage(), trackBounds.x, trackBounds.y, w, h, null);
        g2d.dispose();
    }

    @Override
    public void paintFocus(Graphics g) {
    }
}
