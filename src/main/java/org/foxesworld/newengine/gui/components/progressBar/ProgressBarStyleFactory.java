package org.foxesworld.newengine.gui.components.progressBar;

import org.foxesworld.newengine.gui.styles.StyleProvider;

public class ProgressBarStyleFactory {
    private ProgressBarStyle progressBarStyle;

    public ProgressBarStyleFactory(StyleProvider.StyleAttributes styles) {
        this.createProgressBarStyle(styles.name, styles.background, styles.forgeground, styles.borderColor);
    }

    public void createProgressBarStyle(String styleName, String background, String forgeground, String border) {
        progressBarStyle = new ProgressBarStyle(background, forgeground, border);
    }

    public ProgressBarStyle getProgressBarStyle() {
        return progressBarStyle;
    }
}
