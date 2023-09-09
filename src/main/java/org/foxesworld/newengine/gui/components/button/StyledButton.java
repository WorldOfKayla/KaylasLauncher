package org.foxesworld.newengine.gui.components.button;


public class StyledButton extends Button {

    public StyledButton(String text, ButtonStyle style) {
        super(text);
        style.apply(this);
    }
}
