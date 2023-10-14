package org.foxesworld.newengine.gui.components.checkbox;

public class StyledCheckbox extends Checkbox {

    public StyledCheckbox(String string, CheckboxStyle style) {
        super(string);
        style.apply(this);
    }
}
