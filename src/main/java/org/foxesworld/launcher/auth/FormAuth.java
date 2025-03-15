package org.foxesworld.launcher.auth;

import org.foxesworld.Launcher;
import org.foxesworld.engine.gui.componentAccessor.Component;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.button.Button;
import org.foxesworld.engine.gui.components.passfield.PassField;
import org.foxesworld.engine.gui.components.textfield.TextField;
import org.foxesworld.engine.utils.HTTP.RequestState;

import javax.swing.*;
import java.util.Arrays;

public class FormAuth extends ComponentsAccessor {

    @Component("authForm>submit")
    private Button authSubmit;
    public FormAuth(Auth auth) {
        super(auth.getEngine().getGuiBuilder(), "authForm", Arrays.asList(TextField.class, PassField.class, JCheckBox.class));
        if(auth.getAuthRequest().getRequestState() == RequestState.PENDING) {
            authSubmit.setEnabled(false);
        }
    }
}