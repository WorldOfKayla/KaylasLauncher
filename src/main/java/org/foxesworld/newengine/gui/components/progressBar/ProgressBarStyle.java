package org.foxesworld.newengine.gui.components.progressBar;

import org.foxesworld.newengine.gui.components.label.Label;

import javax.swing.*;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;

public class ProgressBarStyle {
    private String background;
    private  String forgeground;
    private  String border;

    public ProgressBarStyle(String background, String foreground, String border) {
        this.background = background;
        this.forgeground = foreground;
        this.border = border;
    }

    public void apply(JProgressBar progressBar) {
        progressBar.setBackground(hexToColor(background));
        progressBar.setForeground(hexToColor(forgeground));
        progressBar.setBorder(BorderFactory.createLineBorder(hexToColor(border)));
    }
}
