package org.takesome.launcher.gui;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.gui.ComponentValue;
import org.takesome.kaylasEngine.gui.components.ComponentAttributes;
import org.takesome.kaylasEngine.gui.components.ComponentFactoryListener;

public class InitialValue extends ComponentValue implements ComponentFactoryListener {

    private int count;
    private final Launcher launcher;
    public InitialValue(Launcher launcher) {
        super(launcher);
        this.launcher = launcher;
    }

    @Override
    public void onComponentCreation(ComponentAttributes componentAttributes) {
        if (componentAttributes.getInitialValue() != null) {
            this.setInitialData(componentAttributes);
        }
        count+=1;
    }

    @Override
    public void setInitialData(ComponentAttributes componentAttributes) {
        String[] splitValue = String.valueOf(componentAttributes.getInitialValue()).split("#");
        switch (splitValue[0]) {
            case "config" -> componentAttributes.setInitialValue(String.valueOf(this.launcher.getConfig().getConfig().get(splitValue[1])));
            case "user" -> componentAttributes.setInitialValue(this.launcher.getAuth().getAuthCredentials(splitValue[1]));
            case "servers" -> componentAttributes.setInitialValue(this.launcher.getAuth().getUserDataLoader().getUserServersArray());
            case "balance" -> componentAttributes.setInitialValue(String.valueOf(this.launcher.getAuth().getUserDataLoader().getBalanceMap().get(splitValue[1])));
            case "version" -> componentAttributes.setInitialValue(this.launcher.getEngineData().getLauncherVersion());
            case "build" -> componentAttributes.setInitialValue(this.launcher.getEngineData().getLauncherBuild());
        }
    }
}