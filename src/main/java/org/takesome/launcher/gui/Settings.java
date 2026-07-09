package org.takesome.launcher.gui;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.gui.componentAccessor.Component;
import org.takesome.kaylasEngine.gui.componentAccessor.ComponentsAccessor;
import org.takesome.kaylasEngine.gui.components.checkbox.CheckBoxListener;
import org.takesome.kaylasEngine.gui.components.checkbox.Checkbox;
import org.takesome.kaylasEngine.gui.components.combobox.Combobox;
import org.takesome.kaylasEngine.gui.components.combobox.ComboboxListener;
import org.takesome.kaylasEngine.gui.components.compositeSlider.CompositeSlider;
import org.takesome.kaylasEngine.gui.components.compositeSlider.SliderListener;
import org.takesome.kaylasEngine.gui.components.fileSelector.FileSelector;
import org.takesome.kaylasEngine.gui.components.slider.Slider;
import org.takesome.kaylasEngine.gui.components.spinner.Spinner;
import org.takesome.kaylasEngine.gui.components.spinner.SpinnerListener;
import org.takesome.kaylasEngine.gui.components.textArea.TextArea;
import org.takesome.kaylasEngine.gui.components.textfield.TextField;
import org.takesome.kaylasEngine.gui.components.textfield.TextFieldListener;
import org.takesome.kaylasEngine.utils.DataInjector;
import org.takesome.launcher.LauncherValidator;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Settings extends ComponentsAccessor implements SliderListener, ComboboxListener, TextFieldListener, CheckBoxListener, SpinnerListener {
    private static final LauncherUiProvider UI_PROVIDER = LauncherUiProvider.load();
    private Launcher launcher;
    private final LauncherUiProvider ui;
    @Component
    @SuppressWarnings("unused")
    private TextArea settingsInfo;

    public Settings(Launcher launcher) {
        super(launcher.getGuiBuilder(), UI_PROVIDER.panels().settings(), List.of(
                TextArea.class,
                JSpinner.class,
                Checkbox.class,
                Combobox.class,
                TextField.class,
                Slider.class,
                CompositeSlider.class,
                FileSelector.class
        ));
        this.launcher = launcher;
        this.ui = UI_PROVIDER;
        this.ui.validate();
        this.addListeners();
    }

    public void applySettings(String panelId) {
        LauncherValidator.closeSocket();
        for (Map.Entry<String, Object> entry : this.collectFormCredentialsForPanel(panelId).entrySet()) {
            Object value = determineValueType(entry.getValue());
            this.launcher.getConfig().setConfigValue(entry.getKey(), value);
        }

        this.launcher.getConfig().writeCurrentConfig();
        this.launcher.getSOUND().getSoundPlayer().stopAllSounds();
        this.launcher.getEngine().getFrame().dispose();
        this.launcher = new Launcher(this.launcher.getFrame().getBounds());
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
        for (JComponent component : this.getComponentsForPanel(ui.forms().settingsFields())) {
            if (component instanceof CompositeSlider compositeSlider) {
                compositeSlider.setSliderListener(this);
            }

            if (component instanceof TextField textField) {
                textField.setTextFieldListener(this);
            }

            if (component instanceof Checkbox checkbox) {
                checkbox.setCheckBoxListener(this);
            }

            if (component instanceof Combobox combobox) {
                String[] localeTranslate = new String[this.launcher.getLANG().getLocalesSet().length];
                int num = 0;
                for (String lang : launcher.getLANG().getLocalesSet()) {
                    localeTranslate[num] = this.launcher.getLANG().getString("general." + lang);
                    num += 1;
                }
                combobox.setValues(localeTranslate);
                combobox.setSelectedIndex(launcher.getLANG().getLocaleIndex());
                combobox.setComboboxListener(this);
            }

            if (component instanceof Spinner spinner) {
                spinner.setSpinnerListener(this);
            }
        }
    }

    public void openGameFolder() {
        try {
            Desktop d = Desktop.getDesktop();
            d.browse(new URI(this.launcher.getConfig().getHomeDir().replaceAll(Pattern.quote("\\"), "/")));
        } catch (IOException | URISyntaxException ignored) {
        }
    }

    @Override
    public void onComboboxCreated(Combobox combobox) {
    }

    @Override
    public void onComboboxOpen(Combobox combobox) {
    }

    @Override
    public void onComboboxClose(Combobox combobox) {
        launcher.getLANG().setLocaleIndex(combobox.getSelectedIndex());
        launcher.getFrame().getPanel().repaint();
    }

    @Override
    public void onComboboxHover(Combobox combobox, int hoverIndex) {
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
        DataInjector<String> descInjector = new DataInjector<>();
        descInjector.addListener(desc -> SwingUtilities.invokeLater(() -> {
            settingsInfo.setWrapStyleWord(true);
            settingsInfo.setText(desc);
        }));
        launcher.getExecutorServiceProvider().submitTask(() -> {
            String desc = launcher.getLANG().getString("settings." + jCheckBox.getName() + "-desc");
            descInjector.setContent(desc);
        }, "loadSettingsDesc-" + jCheckBox.getName());
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
        ((Slider) this.getComponent(customJSpinner.getName().replace("Text", "Slider"))).setValue((Integer) customJSpinner.getValue());
    }

    @Override
    public void onSliderChange(CompositeSlider compositeSlider) {
        SwingUtilities.invokeLater(() -> {
            int value = (int) compositeSlider.getValue();
            switch (compositeSlider.getName()) {
                case "volume" -> {
                    launcher.getConfig().setVolume(value);
                    launcher.getEngine().getConfig().getConfig().put("volume", value);
                    launcher.getSOUND().getSoundPlayer().changeActiveVolume(value / 100.0f - 0.15F);
                }
                case "ramAmount" -> {
                    // Reserved for future RAM slider-specific behavior.
                }
            }
        });
    }
}
