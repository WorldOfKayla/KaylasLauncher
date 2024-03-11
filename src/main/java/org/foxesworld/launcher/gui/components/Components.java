package org.foxesworld.launcher.gui.components;

import org.foxesworld.engine.gui.components.ComponentAttributes;
import org.foxesworld.engine.gui.components.ComponentFactoryListener;
import org.foxesworld.Launcher;

public class Components implements ComponentFactoryListener {

    private final Launcher launcher;
    public Components(Launcher launcher) {
        this.launcher = launcher;
    }
    @Override
    public void onComponentCreation(ComponentAttributes componentAttributes) {
        if (componentAttributes.getInitialValue() != null) {
            this.getInitialData(componentAttributes);
        }
    }

    private void getInitialData(ComponentAttributes componentAttributes) {
        String[] splitValue = componentAttributes.getInitialValue().split("#");
        switch (splitValue[0]) {
            case "config" -> componentAttributes.setInitialValue(String.valueOf(this.launcher.getConfig().getCONFIG().get(splitValue[1])));
            case "user" -> componentAttributes.setInitialValue(this.launcher.getAuth().getAuthCredentials(splitValue[1]));
            case "version" -> componentAttributes.setInitialValue(this.launcher.getEngineData().getLauncherVersion());
        }
    }
}
