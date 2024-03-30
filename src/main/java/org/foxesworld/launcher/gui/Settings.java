package org.foxesworld.launcher.gui;

import org.foxesworld.Launcher;
import org.foxesworld.engine.gui.components.checkbox.CheckBoxListener;
import org.foxesworld.engine.gui.components.checkbox.Checkbox;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.dropBox.DropBoxListener;
import org.foxesworld.engine.gui.components.slider.Slider;
import org.foxesworld.engine.gui.components.slider.SliderListener;
import org.foxesworld.engine.gui.components.textfield.TextFieldListener;
import org.foxesworld.engine.gui.components.textfield.TextField;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class Settings implements SliderListener, DropBoxListener, TextFieldListener, CheckBoxListener {
    private final Launcher launcher;

    public Settings(Launcher launcher) {
        this.launcher = launcher;
        this.launcher.getLANG().setLocaleIndex(this.launcher.getConfig().getLang());
    }

    public void addListeners(){
        for (JComponent component : launcher.getGuiBuilder().getComponentsMap().get("settingsFields")) {

            if (component instanceof Slider) {
                ((Slider) component).setSliderListener(this);
            }

            if(component instanceof DropBox) {
                ((DropBox) component).setValues(launcher.getLANG().getLocalesSet());
                ((DropBox) component).setSelectedIndex(launcher.getLANG().getLocaleIndex());
                ((DropBox) component).setScrollBoxListener(this);
            }

            if(component instanceof  TextField) {
                ((TextField) component).setTextFieldListener(this);
            }

            if(component instanceof Checkbox) {
                ((Checkbox) component).setCheckBoxListener(this);
            }
        }
    }

    public static void openGameFolder() {
        try {
            Desktop d = Desktop.getDesktop();
            d.browse(new URI(Config.getFullPath().replaceAll(Pattern.quote("\\"), "/")));
        } catch (IOException | URISyntaxException ignored) {
        }
    }
    @Override
    public void onSliderChange(Slider slider) {
        SwingUtilities.invokeLater(() -> {
            int value = slider.getValue();
            switch (slider.getName()) {
                case "volume" -> {
                        launcher.getConfig().setVolume(value);
                        launcher.getSOUND().getSoundPlayer().changeActiveVolume(value / 100.0f);
                        ((TextField) launcher.getGuiBuilder().getComponentById("volumeText")).setText(String.valueOf(value));
                }

                case "ramAmount" -> ((TextField) launcher.getGuiBuilder().getComponentById("ramAmountText")).setText(String.valueOf(value));
            }
        });

    }

    @Override
    public void onScrollBoxCreated(int i) {

    }

    @Override
    public void onScrollBoxOpen(int i) {

    }

    @Override
    public void onScrollBoxClose(int i) {
        launcher.getLANG().setCurrentLang(launcher.getLANG().getLocalesSet()[i]);
        launcher.getFrame().getPanel().repaint();
    }

    @Override
    public void onServerHover(int i) {

    }

    @Override
    public void onTextChange(TextField textfield) {
        if(!textfield.getText().equals("")) {
            ((Slider) launcher.getGuiBuilder().getComponentById(textfield.getName().replace("Text", ""))).setValue(Integer.parseInt(textfield.getText()));
        }
    }

    @Override
    public void onHover(JCheckBox jCheckBox) {
        this.launcher.getGuiBuilder().setLabelText("settingsInfo", this.launcher.getEngine().getLANG().getString("settings."+jCheckBox.getName()+"-desc"), true);
    }

    @Override
    public void onClick(JCheckBox jCheckBox) {
    }

    @Override
    public void onActivate(JCheckBox jCheckBox) {
    }

    @Override
    public void onDisable(JCheckBox jCheckBox) {
    }
}
