package org.foxesworld.newengine.gui.components.label;

import org.foxesworld.newengine.gui.styles.StyleProvider;

public class LabelStyleFactory {
    private LabelStyle labelStyle;

    public LabelStyleFactory(StyleProvider.StyleAttributes styles) {
        this. labelStyle = new LabelStyle(styles);
    }

    public LabelStyle getLabelStyle() {
        return labelStyle;
    }
}
