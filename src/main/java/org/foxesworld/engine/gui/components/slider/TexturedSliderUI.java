package org.foxesworld.engine.gui.components.slider;

import org.foxesworld.engine.gui.components.ComponentFactory;
import org.foxesworld.engine.gui.styles.StyleAttributes;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

public class TexturedSliderUI extends BasicSliderUI {
    private final BufferedImage thumbImageNormal;
    private final BufferedImage thumbImageHover;
    private final BufferedImage thumbImageDisabled;
    private final ImageUtils imageUtils;
    private final ImageIcon trackImage;
    private BufferedImage currentThumbImage;
    private final ComponentFactory componentFactory;
    BufferedImage thumbTexture;

    public TexturedSliderUI(ComponentFactory componentFactory, JSlider slider, StyleAttributes style) {
        super(slider);
        this.imageUtils = componentFactory.getEngine().getImageUtils();
        this.componentFactory = componentFactory;

        thumbTexture = componentFactory.getEngine().getImageUtils().getLocalImage(style.getThumbImage());
        int thumbWidth = thumbTexture.getWidth() / 3;
        int thumbHeight = thumbTexture.getHeight();

        this.thumbImageNormal = this.imageUtils.getTexture(thumbTexture, componentFactory.getStyle().getBorderRadius(),0, 0, thumbWidth, thumbHeight);
        this.thumbImageHover = this.imageUtils.getTexture(thumbTexture, componentFactory.getStyle().getBorderRadius(), thumbWidth, 0, thumbWidth, thumbHeight);
        this.thumbImageDisabled = this.imageUtils.getTexture(thumbTexture, componentFactory.getStyle().getBorderRadius(), thumbWidth * 2, 0, thumbWidth, thumbHeight);
        this.trackImage =  new ImageIcon(componentFactory.getEngine().getImageUtils().getScaledImage(componentFactory.getEngine().getImageUtils().getLocalImage(style.getTrackImage()), style.getWidth(), 10));
        this.currentThumbImage = thumbImageNormal;
        slider.setSize(style.getWidth(), style.getHeight());

        slider.addMouseListener(createMouseListener());
        slider.addMouseMotionListener((MouseMotionListener) createMouseListener());
    }

    private MouseListener createMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (slider.isEnabled()) {
                    currentThumbImage = thumbImageHover;
                    slider.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (slider.isEnabled()) {
                    currentThumbImage = thumbImageNormal;
                    slider.repaint();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (slider.isEnabled()) {
                    currentThumbImage = thumbImageHover;
                    slider.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (slider.isEnabled()) {
                    currentThumbImage = thumbImageNormal;
                    slider.repaint();
                }
            }
        };
    }

    @Override
    public void paintThumb(Graphics g) {
        Rectangle knobBounds = thumbRect;

        int thumbWidth = currentThumbImage.getWidth();
        int thumbHeight = currentThumbImage.getHeight();

        int x = knobBounds.x + (knobBounds.width - thumbWidth) / 2;
        int y = knobBounds.y + (knobBounds.height - thumbHeight) / 2;

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.drawImage(currentThumbImage, x, y, thumbWidth, thumbHeight, null);
        g2d.dispose();
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

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        if (!slider.isEnabled()) {
            currentThumbImage = thumbImageDisabled;
        }
    }

    @Override
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        slider.removeMouseListener(createMouseListener());
        slider.removeMouseMotionListener((MouseMotionListener) createMouseListener());
    }

    @Override
    public void setThumbLocation(int x, int y) {
        super.setThumbLocation(x, y);
        if (!slider.isEnabled()) {
            currentThumbImage = thumbImageDisabled;
        } else {
            currentThumbImage = thumbImageNormal;
        }
    }
}