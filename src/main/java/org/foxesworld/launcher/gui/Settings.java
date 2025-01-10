package org.foxesworld.launcher.gui;

import org.foxesworld.Launcher;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.checkbox.CheckBoxListener;
import org.foxesworld.engine.gui.components.checkbox.Checkbox;
import org.foxesworld.engine.gui.components.compositeSlider.CompositeSlider;
import org.foxesworld.engine.gui.components.compositeSlider.SliderListener;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.dropBox.DropBoxListener;
import org.foxesworld.engine.gui.components.fileSelector.FileSelector;
import org.foxesworld.engine.gui.components.slider.Slider;
import org.foxesworld.engine.gui.components.spinner.Spinner;
import org.foxesworld.engine.gui.components.spinner.SpinnerListener;
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

public class Settings extends ComponentsAccessor implements SliderListener, DropBoxListener, TextFieldListener, CheckBoxListener, SpinnerListener {
    private Launcher launcher;

    public Settings(Launcher launcher) {
        super(launcher.getGuiBuilder(), "settings", List.of(TextArea.class, JSpinner.class, Checkbox.class, DropBox.class, TextField.class, Slider.class, CompositeSlider.class, FileSelector.class));
        this.launcher = launcher;
    }

    public void applySettings(String panelId) {
        for (Map.Entry<String, Object> entry : this.collectFormCredentialsForPanel(panelId).entrySet()) {
            System.out.println(entry.getKey());
            Object value = determineValueType(entry.getValue());
            this.launcher.getConfig().setConfigValue(entry.getKey(), value);
        }

        this.launcher.getConfig().writeCurrentConfig();
        this.launcher.getSOUND().getSoundPlayer().stopAllSounds();
        this.launcher.getEngine().getFrame().dispose();
        this.launcher = new Launcher();
    }

    private Object determineValueType(Object value) {
        if (value instanceof String stringValue) {
            if ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
                return Boolean.parseBoolean(stringValue);
            }
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException ignored) {
            }
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException ignored) {
            }
            return stringValue;
        }
        return value;
    }


    public void addListeners() {
        for (JComponent component : this.getComponentsForPanel("settingsFields")) {
            if (component instanceof CompositeSlider) {
                ((CompositeSlider) component).setSliderListener(this);
            }

            if (component instanceof TextField) {
                ((TextField) component).setTextFieldListener(this);
            }

            if (component instanceof Checkbox) {
                ((Checkbox) component).setCheckBoxListener(this);
            }

            if (component instanceof DropBox) {
                String[] localeTransate = new String[this.launcher.getLANG().getLocalesSet().length];
                int num = 0;
                for (String lang : launcher.getLANG().getLocalesSet()) {
                    localeTransate[num] = this.launcher.getLANG().getString("general." + lang);
                    num += 1;
                }
                ((DropBox) component).setValues(localeTransate);
                ((DropBox) component).setSelectedIndex(launcher.getLANG().getLocaleIndex());
                ((DropBox) component).setScrollBoxListener(this);
            }

            if(component instanceof Spinner) {
                ((Spinner) component).setSpinnerListener(this);
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
    public void onScrollBoxCreated(DropBox dropBox) {

    }

    @Override
    public void onScrollBoxOpen(DropBox dropBox) {

    }

    @Override
    public void onScrollBoxClose(DropBox dropBox) {
        launcher.getLANG().setLocaleIndex(dropBox.getSelectedIndex());
        launcher.getFrame().getPanel().repaint();
    }

    @Override
    public void onServerHover(DropBox dropBox, int i) {

    }

    @Override
    public void onTextChange(TextField textfield) {
        if (!textfield.getText().equals("")) {
            Slider slider = (Slider) this.getComponent(textfield.getName().replace("Text", "Slider"));
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

    @Override
    public void onSpinnerChange(Spinner customJSpinner) {
        ((Slider)this.getComponent(customJSpinner.getName().replace("Text", "Slider"))).setValue((Integer) customJSpinner.getValue());
    }

    @Override
    public void onSliderChange(CompositeSlider compositeSlider) {
        SwingUtilities.invokeLater(() -> {
            int value = compositeSlider.getValue();
            switch (compositeSlider.getName()) {
                case "volume" -> {
                    launcher.getConfig().setVolume(value);
                    launcher.getEngine().getConfig().getConfig().put("volume", value);
                    launcher.getSOUND().getSoundPlayer().changeActiveVolume(value / 100.0f - 0.15F);
                    //((JSpinner) this.getComponent("volumeText")).setValue(value);
                }

                case "ramAmount" -> {
                    //((JSpinner) this.getComponent("ramAmountText")).setValue(value);
                    //launcher.getEngine().getConfig().getConfig().put("ramAmount", value);
                }
            }
        });
    }
}