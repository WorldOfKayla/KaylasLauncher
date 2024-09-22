package org.foxesworld.launcher.gui.components;

import org.foxesworld.Launcher;
import org.foxesworld.engine.gui.components.ComponentAttributes;
import org.foxesworld.engine.gui.components.ComponentFactoryListener;

import javax.swing.text.*;
import java.awt.*;

public class ComponentManager implements ComponentFactoryListener {

    private final Launcher launcher;
    public ComponentManager(Launcher launcher) {
        this.launcher = launcher;
    }

    @Override
    public void onComponentCreation(ComponentAttributes componentAttributes) {
        if (componentAttributes.getInitialValue() != null) {
            this.getInitialData(componentAttributes);
        }
    }

    protected void getInitialData(ComponentAttributes componentAttributes) {
        StyleContext sc = new StyleContext();
        Style textStyle = sc.addStyle("Text", null);
        StyleConstants.setForeground(textStyle, Color.BLACK);
        Style dotStyle = sc.addStyle("Dot", null);
        StyleConstants.setForeground(dotStyle, Color.RED);
        Style numberStyle = sc.addStyle("Number", null);
        StyleConstants.setForeground(numberStyle, Color.BLUE);

        String[] splitValue = componentAttributes.getInitialValue().split("#");
        switch (splitValue[0]) {
            case "config" -> componentAttributes.setInitialValue(String.valueOf(this.launcher.getConfig().getCONFIG().get(splitValue[1])));
            case "user" -> componentAttributes.setInitialValue(this.launcher.getAuth().getAuthCredentials(splitValue[1]));
            case "balance" -> componentAttributes.setInitialValue(String.valueOf(this.launcher.getAuth().getBalanceMap().get(splitValue[1])));
            case "version" -> componentAttributes.setInitialValue(this.launcher.getEngineData().getLauncherVersion());
        }
    }
}