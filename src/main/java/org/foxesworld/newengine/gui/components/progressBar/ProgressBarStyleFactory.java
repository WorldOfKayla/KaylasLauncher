package org.foxesworld.newengine.gui.components.progressBar;

import org.foxesworld.newengine.gui.styles.StyleProvider;

public class ProgressBarStyleFactory {
    private ProgressBarStyle progressBarStyle;

    public ProgressBarStyleFactory(StyleProvider.StyleAttributes styles) {
        this.progressBarStyle = progressBarStyle = new ProgressBarStyle(styles);
    }

    public ProgressBarStyle getProgressBarStyle() {
        return progressBarStyle;
    }
}
