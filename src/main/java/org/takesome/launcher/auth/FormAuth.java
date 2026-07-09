package org.takesome.launcher.auth;

import org.takesome.kaylasEngine.gui.componentAccessor.ComponentsAccessor;
import org.takesome.kaylasEngine.gui.components.passfield.PassField;
import org.takesome.kaylasEngine.gui.components.textfield.TextField;
import org.takesome.launcher.gui.LauncherUiProvider;

import javax.swing.*;
import java.util.Arrays;

public class FormAuth extends ComponentsAccessor {


    public FormAuth(Auth auth) {
        super(auth.getEngine().getGuiBuilder(), LauncherUiProvider.load().authFormPanelId(), Arrays.asList(TextField.class, PassField.class, JCheckBox.class));
    }
}