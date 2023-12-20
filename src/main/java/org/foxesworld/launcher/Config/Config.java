package org.foxesworld.launcher.Config;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.slider.Slider;
import org.foxesworld.engine.gui.components.slider.SliderListener;

import javax.swing.*;

public class Config implements SliderListener {

    private  Engine engine;

    public Config(Engine engine){
        this.engine = engine;
        for(JComponent jSlider: engine.getGuiBuilder().getComponentsMap().get("settingsFields")){
            /*
            if(jSlider instanceof Slider){
                ((Slider) jSlider).setSliderListener(this);
                System.out.println("Adding listener "+ jSlider.getName());
            }
            */
        }
    }
    /* TODO
    *   Implement configuration listener of all fields here
    * */

    @Override
    public void onSliderChange(float value) {
        System.out.println(value);
    }
}
