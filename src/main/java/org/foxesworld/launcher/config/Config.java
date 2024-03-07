package org.foxesworld.launcher.config;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.dropBox.DropBoxListener;
import org.foxesworld.engine.gui.components.slider.Slider;
import org.foxesworld.engine.gui.components.slider.SliderListener;
import org.foxesworld.engine.gui.components.textfield.TextFieldListener;
import org.foxesworld.engine.gui.components.textfield.Textfield;

import javax.swing.*;

public class Config implements SliderListener, DropBoxListener, TextFieldListener {
    private Engine engine;

    public Config(Engine engine) {
        this.engine = engine;
        this.engine.getLANG().setLocaleIndex(engine.getCONFIG().getLang());
    }

    public void addListeners(){
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

            if(component instanceof  Textfield) {
                ((Textfield) component).setTextFieldListener(this);
            }
        }
    }
    @Override
    public void onSliderChange(Slider slider) {
        int value = slider.getValue();
        switch (slider.getName()){
            case "volume" -> {
                engine.getCONFIG().setVolume(value);
                engine.getSOUND().getSoundPlayer().changeActiveVolume(value / 100.0f);
                ((Textfield)engine.getGuiBuilder().getComponentById("volumeText")).setText(String.valueOf(value));
            }

            case "ramAmount" -> {
                ((Textfield)engine.getGuiBuilder().getComponentById("ramAmountText")).setText(String.valueOf(value));
            }
        }

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

    @Override
    public void onTextChange(Textfield textfield) {
        ((Slider)engine.getGuiBuilder().getComponentById(textfield.getName().replace("Text", ""))).setValue(Integer.parseInt(textfield.getText()));
        //System.out.println(textfield.getName().replace("text", "") + " " + textfield.getText());
    }
}
