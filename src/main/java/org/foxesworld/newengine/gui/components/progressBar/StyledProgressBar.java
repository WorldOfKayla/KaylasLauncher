package org.foxesworld.newengine.gui.components.progressBar;

import javax.swing.*;

public class StyledProgressBar extends JProgressBar {
    public StyledProgressBar(ProgressBarStyle style){
        style.apply(this);
    }

}
