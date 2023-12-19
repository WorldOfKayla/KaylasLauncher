package org.foxesworld.launcher.Config;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.slider.SliderListener;

public class Config implements SliderListener {

    private  Engine engine;

    public Config(Engine engine){
        this.engine = engine;
    }

    @Override
    public void onSliderChange(float value) {

    }
}
