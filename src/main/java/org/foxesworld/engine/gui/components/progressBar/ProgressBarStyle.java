package org.foxesworld.engine.gui.components.progressBar;

import org.foxesworld.engine.gui.components.ComponentFactory;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class ProgressBarStyle {
    private String background;
    private  String forgeground;
    private  String border;
    private ComponentFactory componentFactory;

    public ProgressBarStyle(ComponentFactory componentFactory) {
        this.componentFactory = componentFactory;
        this.background = componentFactory.style.backgroundImage;
        this.forgeground = componentFactory.style.background;
        this.border = componentFactory.style.borderColor;
    }

    public void apply(JProgressBar progressBar) {
        progressBar.setBackground(hexToColor(background));
        progressBar.setForeground(hexToColor(forgeground));
        progressBar.setBorder(BorderFactory.createLineBorder(hexToColor(border)));
        setTexture(progressBar, componentFactory.style.texture);
    }

    private static void setTexture(JProgressBar progressBar, String imagePath) {
        progressBar.setUI(new TexturedProgressBar(imagePath));
    }
}
