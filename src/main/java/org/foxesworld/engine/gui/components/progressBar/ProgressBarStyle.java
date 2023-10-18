package org.foxesworld.engine.gui.components.progressBar;

import org.foxesworld.engine.gui.components.Components;

import javax.swing.*;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class ProgressBarStyle {
    private String background;
    private  String forgeground;
    private  String border;

    public ProgressBarStyle(Components components) {
        this.background = components.style.backgroundImage;
        this.forgeground = components.style.background;
        this.border = components.style.borderColor;
    }

    public void apply(JProgressBar progressBar) {
        progressBar.setBackground(hexToColor(background));
        progressBar.setForeground(hexToColor(forgeground));
        progressBar.setBorder(BorderFactory.createLineBorder(hexToColor(border)));
    }
}
