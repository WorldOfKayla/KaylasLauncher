package org.foxesworld.newengine.gui.components.textfield;

public class StyledTextfield extends  Textfield {

    public StyledTextfield(TextfieldStyle style) {
        super();
        style.apply(this);
    }
}
