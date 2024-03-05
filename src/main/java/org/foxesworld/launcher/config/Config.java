package org.foxesworld.launcher.config;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.slider.Slider;
import org.foxesworld.engine.gui.components.slider.SliderListener;

import javax.swing.*;

public class Config implements SliderListener {
    private final Engine engine;

    public Config(Engine engine) {
        this.engine = engine;
        for (JComponent component : engine.getGuiBuilder().getComponentsMap().get("settingsFields")) {

            if (component instanceof Slider) {
                ((Slider) component).setSliderListener(this);
                //If is slider add listener here
            }
        }
    }
    @Override
    public void onSliderChange(float value) {
        engine.getCONFIG().setVolume(value);
        engine.getSOUND().changeActiveVolume(value / 100.0f);
    }
}
