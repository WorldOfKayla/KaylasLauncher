package org.foxesworld.launcher.config;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.dropBox.DropBoxListener;
import org.foxesworld.engine.gui.components.slider.Slider;
import org.foxesworld.engine.gui.components.slider.SliderListener;

import javax.swing.*;

public class Config implements SliderListener, DropBoxListener {
    private final Engine engine;

    public Config(Engine engine) {
        this.engine = engine;
        this.engine.getLANG().setLocaleIndex(engine.getCONFIG().getLang());
        for (JComponent component : engine.getGuiBuilder().getComponentsMap().get("settingsFields")) {

            if (component instanceof Slider) {
                ((Slider) component).setSliderListener(this);
                //If is slider add listener here
            }

            if(component instanceof DropBox) {
                ((DropBox) component).setValues(engine.getLANG().getLocalesSet());
                ((DropBox) component).setSelectedIndex(engine.getLANG().getLocaleIndex());
                ((DropBox) component).setScrollBoxListener(this);
            }
        }
    }
    @Override
    public void onSliderChange(float value) {
        engine.getCONFIG().setVolume(value);
        engine.getSOUND().changeActiveVolume(value / 100.0f);
    }

    @Override
    public void onScrollBoxCreated(int i) {

    }

    @Override
    public void onScrollBoxOpen(int i) {

    }

    @Override
    public void onScrollBoxClose(int i) {
        engine.getLANG().setCurrentLang(engine.getLANG().getLocalesSet()[i]);
        engine.getFrame().getPanel().repaint();
    }

    @Override
    public void onServerHover(int i) {

    }
}
