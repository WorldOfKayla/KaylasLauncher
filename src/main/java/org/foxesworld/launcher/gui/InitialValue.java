package org.foxesworld.launcher.gui;

import org.foxesworld.Launcher;
import org.foxesworld.engine.gui.components.ComponentAttributes;

public class InitialValue extends org.foxesworld.engine.gui.ComponentValue {

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
        //this.launcher.submitTask(() -> {
        String[] splitValue = String.valueOf(componentAttributes.getInitialValue()).split("#");
        switch (splitValue[0]) {
            case "config" -> componentAttributes.setInitialValue(String.valueOf(this.launcher.getConfig().getConfig().get(splitValue[1])));
            case "user" -> componentAttributes.setInitialValue(this.launcher.getAuth().getAuthCredentials(splitValue[1]));
            case "servers" -> componentAttributes.setInitialValue(this.launcher.getAuth().getUserServersArray());
            case "balance" -> componentAttributes.setInitialValue(String.valueOf(this.launcher.getAuth().getBalanceMap().get(splitValue[1])));
            case "version" -> componentAttributes.setInitialValue(this.launcher.getEngineData().getLauncherVersion());
            case "build" -> componentAttributes.setInitialValue(this.launcher.getEngineData().getLauncherBuild());

        }
            //        }, "setInitialData."+componentAttributes.getComponentId());
    }
}