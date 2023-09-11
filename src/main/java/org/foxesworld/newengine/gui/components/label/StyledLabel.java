package org.foxesworld.newengine.gui.components.label;

public class StyledLabel extends Label {

    public StyledLabel(String title, LabelStyle style) {
        super(title);
        style.apply(this);
    }
}
