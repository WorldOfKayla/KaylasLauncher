package org.foxesworld.engine.gui.components.progressBar;

import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.image.BufferedImage;

class TexturedProgressBar extends BasicProgressBarUI {
    private final String textureImagePath;

    public TexturedProgressBar(String textureImagePath) {
        this.textureImagePath = textureImagePath;
    }

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        paintDeterminate(g, c);
    }

    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        int barWidth = progressBar.getWidth();
        int barHeight = progressBar.getHeight();

        BufferedImage texture = ImageUtils.genButton(barWidth, barHeight, ImageUtils.getLocalImage(textureImagePath));

        int progressWidth = (int) (barWidth * progressBar.getPercentComplete());

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(texture, 0, 0, progressWidth, barHeight, null);
        g2d.dispose();
    }
}
