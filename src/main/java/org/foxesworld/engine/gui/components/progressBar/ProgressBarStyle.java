package org.foxesworld.engine.gui.components.progressBar;

import org.foxesworld.engine.gui.components.ComponentFactory;

import javax.swing.*;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class ProgressBarStyle {
    private String background;
    private  String forgeground;
    private  String border;
    private ComponentFactory componentFactory;

    public ProgressBarStyle(ComponentFactory componentFactory) {
        this.componentFactory = componentFactory;
        this.background = componentFactory.style.getBackgroundImage();
        this.forgeground = componentFactory.style.getBackground();
        this.border = componentFactory.style.getBorderColor();
    }

    public void apply(JProgressBar progressBar) {
        progressBar.setBackground(hexToColor(background));
        progressBar.setForeground(hexToColor(forgeground));
        progressBar.setBorder(BorderFactory.createLineBorder(hexToColor(border)));
        setTexture(progressBar, componentFactory.style.getTexture());
    }

    private static void setTexture(JProgressBar progressBar, String imagePath) {
        progressBar.setUI(new TexturedProgressBar(imagePath));
    }
}
