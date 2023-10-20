package org.foxesworld.engine.gui.components.progressBar;

import org.foxesworld.engine.gui.components.ComponentFactory;

import javax.swing.*;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class ProgressBarStyle {
    private String background;
    private  String forgeground;
    private  String border;

    public ProgressBarStyle(ComponentFactory componentFactory) {
        this.background = componentFactory.style.backgroundImage;
        this.forgeground = componentFactory.style.background;
        this.border = componentFactory.style.borderColor;
    }

    public void apply(JProgressBar progressBar) {
        progressBar.setBackground(hexToColor(background));
        progressBar.setForeground(hexToColor(forgeground));
        progressBar.setBorder(BorderFactory.createLineBorder(hexToColor(border)));
    }
}
