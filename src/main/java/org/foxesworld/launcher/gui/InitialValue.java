package org.foxesworld.launcher.gui;

import org.foxesworld.Launcher;
import org.foxesworld.engine.gui.ComponentValue;
import org.foxesworld.engine.gui.components.ComponentAttributes;
import org.foxesworld.engine.gui.components.ComponentFactory;
import org.foxesworld.engine.gui.components.ComponentFactoryListener;

public class InitialValue extends ComponentValue implements ComponentFactoryListener {

    private final Launcher launcher;
    public InitialValue(Launcher launcher) {
        this.launcher = launcher;
    }

    @Override
    public void onComponentCreation(ComponentAttributes componentAttributes) {
        if (componentAttributes.getInitialValue() != null) {
            this.setInitialData(componentAttributes);
        }
    }

    @Override
    public void setInitialData(ComponentAttributes componentAttributes) {
        String[] splitValue = String.valueOf(componentAttributes.getInitialValue()).split("#");
        switch (splitValue[0]) {
            case "config" -> componentAttributes.setInitialValue(String.valueOf(this.launcher.getConfig().getConfig().get(splitValue[1])));
            case "user" -> componentAttributes.setInitialValue(this.launcher.getAuth().getAuthCredentials(splitValue[1]));
            case "servers" -> componentAttributes.setInitialValue(this.launcher.getAuth().getUserServersArray());
            case "balance" -> componentAttributes.setInitialValue(String.valueOf(this.launcher.getAuth().getBalanceMap().get(splitValue[1])));
            case "version" -> componentAttributes.setInitialValue(this.launcher.getEngineData().getLauncherVersion());
            case "build" -> componentAttributes.setInitialValue(this.launcher.getEngineData().getLauncherBuild());
        }
    }
}