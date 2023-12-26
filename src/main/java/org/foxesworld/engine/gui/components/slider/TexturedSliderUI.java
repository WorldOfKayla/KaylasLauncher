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

        int thumbWidth = thumbImage.getIconWidth();
        int thumbHeight = thumbImage.getIconHeight();

        int x = knobBounds.x + (knobBounds.width - thumbWidth) / 2;
        int y = knobBounds.y + (knobBounds.height - thumbHeight) / 2;

        Graphics2D g2d = (Graphics2D) g.create();

        // Draw the entire track to cover the previous thumb position
        paintTrack(g2d);

        // Draw the thumb
        g2d.drawImage(thumbImage.getImage(), x, y, thumbWidth, thumbHeight, null);
        g2d.dispose();

        // Repaint the slider to ensure proper updating
        slider.repaint();
    }


    @Override
    public void paintTrack(Graphics g) {
        Rectangle trackBounds = trackRect;

        int trackWidth = trackImage.getIconWidth();
        int trackHeight = trackImage.getIconHeight();

        int x = trackBounds.x + (trackBounds.width - trackWidth) / 2;
        int y = trackBounds.y + (trackBounds.height - trackHeight) / 2;

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.drawImage(trackImage.getImage(), x, y, trackWidth, trackHeight, null);
        g2d.dispose();
    }

    @Override
    public void paintFocus(Graphics g) {
        // If you need to customize focus painting, implement it here
    }
}
