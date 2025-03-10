package org.foxesworld.launcher.auth;

import org.foxesworld.Launcher;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.passfield.PassField;
import org.foxesworld.engine.gui.components.textfield.TextField;

import javax.swing.*;
import java.util.Arrays;

public class FormAuth extends ComponentsAccessor {
    private final Launcher launcher;
    public FormAuth(Launcher launcher) {
        super(launcher.getGuiBuilder(), "authForm", Arrays.asList(TextField.class, PassField.class, JCheckBox.class));
        this.launcher = launcher;
    }
}