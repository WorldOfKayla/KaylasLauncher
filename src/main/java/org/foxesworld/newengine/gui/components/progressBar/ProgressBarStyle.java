package org.foxesworld.newengine.gui.components.progressBar;

import org.foxesworld.newengine.gui.styles.StyleProvider;

import javax.swing.*;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;

public class ProgressBarStyle {
    private String background;
    private  String forgeground;
    private  String border;

    public ProgressBarStyle(StyleProvider.StyleAttributes styles) {
        this.background = styles.backgroundImage;
        this.forgeground = styles.background;
        this.border = styles.borderColor;
    }

    public void apply(JProgressBar progressBar) {
        progressBar.setBackground(hexToColor(background));
        progressBar.setForeground(hexToColor(forgeground));
        progressBar.setBorder(BorderFactory.createLineBorder(hexToColor(border)));
    }
}
