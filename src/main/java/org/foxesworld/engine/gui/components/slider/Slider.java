package org.foxesworld.engine.gui.components.slider;

import org.foxesworld.engine.gui.components.ComponentFactory;

import javax.swing.*;

public class Slider extends JSlider {
    private SliderListener sliderListener;
    private ComponentFactory componentFactory;

    public Slider(ComponentFactory componentFactory){
        super(componentFactory.getComponentAttribute().getMinValue(), componentFactory.getComponentAttribute().getMaxValue());
        this.componentFactory = componentFactory;

        /* TODO
        *   This should be made using interface not as hardcoded as here
        *   Engine in future will be a separate library and all it's methods will be
        *   run external without modifying it's code ;)
        */
        this.addChangeListener(e -> {
            //HARDCODING sliderListener.onSliderChange(this.getValue()); will be great!
            componentFactory.engine.getCONFIG().setVolume(this.getValue());
            componentFactory.engine.getSOUND().changeActiveVolume(this.getValue() / 100.0f);
        });
    }
    @Deprecated
    public void setSliderListener(SliderListener sliderListener) {
        this.sliderListener = sliderListener;
    }
}
