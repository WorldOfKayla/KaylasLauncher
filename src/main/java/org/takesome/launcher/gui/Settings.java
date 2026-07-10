package org.takesome.launcher.gui;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.gui.componentAccessor.Component;
import org.takesome.kaylasEngine.gui.componentAccessor.ComponentsAccessor;
import org.takesome.kaylasEngine.gui.components.checkbox.CheckBoxListener;
import org.takesome.kaylasEngine.gui.components.checkbox.Checkbox;
import org.takesome.kaylasEngine.gui.components.combobox.Combobox;
import org.takesome.kaylasEngine.gui.components.combobox.ComboboxListener;
import org.takesome.kaylasEngine.gui.components.constructor.ConstructedCompositeComponent;
import org.takesome.kaylasEngine.gui.components.textArea.TextArea;
import org.takesome.kaylasEngine.utils.DataInjector;
import org.takesome.launcher.LauncherValidator;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Settings extends ComponentsAccessor implements ComboboxListener, CheckBoxListener {
    private static final String LANGUAGE_COMBOBOX_ID = "lang";
    private static final String FLAG_ICON_ROOT = "assets/ui/icons/flags/";
    private static final String FLAG_ICON_EXTENSION = ".svg";
    private static final int FLAG_ICON_SIZE = 20;
    private static final LauncherUiProvider UI_PROVIDER = LauncherUiProvider.load();
    private Launcher launcher;
    private final LauncherUiProvider ui;
    @Component
    @SuppressWarnings("unused")
    private TextArea settingsInfo;

    public Settings(Launcher launcher) {
        super(launcher.getGuiBuilder(), UI_PROVIDER.panels().settings(), List.of(
                TextArea.class,
                Checkbox.class,
                Combobox.class,
                ConstructedCompositeComponent.class
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
            if (component instanceof Checkbox checkbox) {
                checkbox.setCheckBoxListener(this);
            }

            if (component instanceof Combobox combobox) {
                configureLanguageCombobox(combobox);
            }

        }
    }


    private void configureLanguageCombobox(Combobox combobox) {
        String[] locales = launcher.getLANG().getLocalesSet();
        String[] localeTranslate = new String[locales.length];
        for (int index = 0; index < locales.length; index++) {
            localeTranslate[index] = launcher.getLANG().getString("general." + locales[index]);
        }
        combobox.setValues(localeTranslate);
        combobox.setSelectedIndex(launcher.getLANG().getLocaleIndex());
        if (LANGUAGE_COMBOBOX_ID.equals(combobox.getName())) {
            combobox.setIcons(loadLanguageFlagIcons(locales));
        }
        combobox.setComboboxListener(this);
    }

    private BufferedImage[] loadLanguageFlagIcons(String[] locales) {
        if (locales == null || locales.length == 0) {
            return new BufferedImage[0];
        }

        BufferedImage[] flags = new BufferedImage[locales.length];
        for (int index = 0; index < locales.length; index++) {
            String locale = locales[index];
            if (locale == null || locale.isBlank()) {
                continue;
            }
            String flagPath = FLAG_ICON_ROOT + locale.toLowerCase(Locale.ROOT) + FLAG_ICON_EXTENSION;
            flags[index] = launcher.getIconUtils().getVectorImage(flagPath, FLAG_ICON_SIZE, FLAG_ICON_SIZE);
        }
        return flags;
    }

    public void openGameFolder() {
        Path homeDirectory = Path.of(this.launcher.getConfig().getFullPath());
        this.launcher.getSystemFileManager()
                .openDirectory(homeDirectory)
                .whenComplete((openedDirectory, error) -> {
                    if (error != null) {
                        Launcher.LOGGER.error("Unable to open launcher home directory {}", homeDirectory, error);
                    } else {
                        Launcher.LOGGER.debug("Launcher home directory opened in system file manager: {}", openedDirectory);
                    }
                });
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

}
