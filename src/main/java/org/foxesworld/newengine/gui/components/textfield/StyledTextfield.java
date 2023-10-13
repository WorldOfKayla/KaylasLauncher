package org.foxesworld.newengine.gui.components.textfield;

public class StyledTextfield extends  Textfield {

    public StyledTextfield(String placeholder, TextfieldStyle style) {
        super(placeholder);
        style.apply(this);
    }
}
