package org.foxesworld.newengine.gui;

import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.gui.components.textfield.TextfieldStyle;
import org.foxesworld.newengine.locale.LanguageProvier;

import javax.swing.*;
import java.awt.*;

public class Frame extends JFrame {
    protected APP app;
    protected LanguageProvier LANG;
    private TextfieldStyle inputStyle = new TextfieldStyle( "assets/ui/inputBox.png", "Roboto-Black", 16.0f, Color.BLACK, Color.decode("0xA67A53"));

    public Frame(APP app) {
        this.LANG = app.getLANG();
        this.app = app;
        initialize();
    }

    private void initialize() {
        GuiBuilder guiBuilder = new GuiBuilder(app);
        guiBuilder.buildGui("interface.json");
    }
}
