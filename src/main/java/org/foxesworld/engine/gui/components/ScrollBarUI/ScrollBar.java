package org.foxesworld.engine.gui.components.ScrollBarUI;

import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class ScrollBar extends BasicScrollBarUI {
    private Image thumbImage;
    private Image trackImage;

    public ScrollBar() {
        thumbImage = ImageUtils.getLocalImage("assets/ui/scrollPane/thumb.png");
        trackImage = ImageUtils.getLocalImage("assets/ui/scrollPane/track.png");
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbImage != null) {
            g.translate(thumbBounds.x, thumbBounds.y);
            AffineTransform transform = AffineTransform.getScaleInstance(thumbBounds.width / (double) thumbImage.getWidth(null), thumbBounds.height / (double) thumbImage.getHeight(null));
            ((Graphics2D) g).drawImage(thumbImage, transform, null);
            g.translate(-thumbBounds.x, -thumbBounds.y);
        }
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        if (trackImage != null) {
            g.translate(trackBounds.x, trackBounds.y);
            AffineTransform transform = AffineTransform.getScaleInstance(trackBounds.width / (double) trackImage.getWidth(null), trackBounds.height / (double) trackImage.getHeight(null));
            ((Graphics2D) g).drawImage(trackImage, transform, null);
            g.translate(-trackBounds.x, -trackBounds.y);
        }
    }

    @Override
    protected void setThumbBounds(int x, int y, int width, int height) {
        super.setThumbBounds(x, y, width, height);
        scrollbar.repaint();
    }
}
