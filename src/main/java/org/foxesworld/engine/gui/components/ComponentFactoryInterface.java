package org.foxesworld.engine.gui.components;

import javax.swing.*;

public interface ComponentFactoryInterface {

    JComponent onComponentCreation(ComponentAttributes componentAttributes);
}
