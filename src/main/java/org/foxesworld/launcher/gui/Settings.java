package org.foxesworld.launcher.gui;

import org.foxesworld.Launcher;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.checkbox.CheckBoxListener;
import org.foxesworld.engine.gui.components.checkbox.Checkbox;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.dropBox.DropBoxListener;
import org.foxesworld.engine.gui.components.slider.Slider;
import org.foxesworld.engine.gui.components.slider.SliderListener;
import org.foxesworld.engine.gui.components.textArea.TextArea;
import org.foxesworld.engine.gui.components.textfield.TextField;
import org.foxesworld.engine.gui.components.textfield.TextFieldListener;
import org.foxesworld.launcher.config.Config;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Settings extends ComponentsAccessor implements SliderListener, DropBoxListener, TextFieldListener, CheckBoxListener {
    private Launcher launcher;

    public Settings(Launcher launcher) {
        super(launcher.getGuiBuilder(), "settings", List.of(TextArea.class, Checkbox.class, DropBox.class, TextField.class, Slider.class));
        this.launcher = launcher;
        //this.launcher.getLANG().setLocaleIndex(this.launcher.getLANG().getLocalesSet()[this.launcher.getConfig().getLang()]);
    }

    public void applySettings(String panelId) {
        for (Map.Entry<String, String> map : this.collectFormCredentialsForPanel(panelId).entrySet()) {
            Class<Config> clazz = Config.class;
            try {
                clazz.getDeclaredField(map.getKey());
                System.out.println(map.getKey() + ' ' + map.getValue());
                this.launcher.getConfig().setConfigValue(map.getKey(), map.getValue());
            } catch (NoSuchFieldException ignored) {
            }
        }
        this.launcher.getConfig().writeCurrentConfig();
        this.launcher.getSOUND().getSoundPlayer().stopAllSounds();
        this.launcher.getEngine().getFrame().dispose();
        this.launcher = new Launcher();
    }

    public void addListeners() {
        for(JComponent component: this.getComponentsForPanel("settingsFields")){
            if (component instanceof Slider) {
                ((Slider) component).setSliderListener(this);
            }

            if (component instanceof TextField) {
                ((TextField) component).setTextFieldListener(this);
            }

            if (component instanceof Checkbox) {
                ((Checkbox) component).setCheckBoxListener(this);
            }

            if (component instanceof DropBox) {
                ((DropBox) component).setValues(launcher.getLANG().getLocalesSet());
                ((DropBox) component).setSelectedIndex(launcher.getLANG().getLocaleIndex());
                ((DropBox) component).setScrollBoxListener(this);
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
                    launcher.getEngine().getConfig().getCONFIG().put("volume", value);
                    launcher.getSOUND().getSoundPlayer().changeActiveVolume(value / 100.0f - 0.15F);
                    ((TextField) this.getComponent("volumeText")).setText(String.valueOf(value));
                }

                case "ramAmount" -> ((TextField) this.getComponent("ramAmountText")).setText(String.valueOf(value));
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
        launcher.getLANG().setLocaleIndex(i);
        launcher.getFrame().getPanel().repaint();
    }

    @Override
    public void onServerHover(int i) {

    }

    @Override
    public void onTextChange(TextField textfield) {
        if (!textfield.getText().equals("")) {
            Slider slider = (Slider) this.getComponent(textfield.getName().replace("Text", ""));
            if (slider != null) {
                slider.setValue(Integer.parseInt(textfield.getText()));
            }
        }
    }

    @Override
    public void onHover(JCheckBox jCheckBox) {
        TextArea infoArea = (TextArea) this.getComponent("settingsInfo");
        infoArea.setWrapStyleWord(true);
        infoArea.setText(this.launcher.getLANG().getString("settings." + jCheckBox.getName() + "-desc"));
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