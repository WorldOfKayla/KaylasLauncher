package org.foxesworld.launcher.auth;

import org.foxesworld.engine.gui.ComponentsAccessor;
import org.foxesworld.engine.gui.components.checkbox.Checkbox;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class FormAuth extends ComponentsAccessor {

    private  Map<String, String> authCredentials = new HashMap<>();
    public FormAuth(Auth auth) {
        super(auth.getEngine().getGuiBuilder(), "authForm");
        this.collectData();

    }
    private void collectData() {
        String name, value;
        for (JComponent component: this.getComponentList()) {
           name = component.getName();
           if(component instanceof JTextField) {
               value = ((JTextField)component).getText();
           } else if (component instanceof Checkbox) {
               value = String.valueOf(((Checkbox) component).isSelected());
           } else {
               value = "";
           }
           authCredentials.put(name, value);
        }
    }

    public Map<String, String> getAuthCredentials() {
        return authCredentials;
    }
}
