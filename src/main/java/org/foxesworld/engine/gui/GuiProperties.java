package org.foxesworld.engine.gui;

import org.foxesworld.engine.Engine;
import org.foxesworld.launcher.User.User;

import java.lang.reflect.Field;
import java.util.Map;

public class GuiProperties {
    private String frameTpl;
    private String mainFrame;
    private String localeFile;

    public GuiProperties(Engine engine){
        Map<String, Object> guiList = engine.getEngineData().getGui();
        for (Map.Entry<String, Object> guiEl : guiList.entrySet()) {
            try {
                Field field = GuiProperties.class.getDeclaredField(guiEl.getKey());
                if(field.hashCode()!= 0) {
                    field.set(this, guiEl.getValue());
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        }
    }

    public String getFrameTpl() {
        return frameTpl;
    }

    public String getMainFrame() {
        return mainFrame;
    }

    public String getLocaleFile() {
        return localeFile;
    }
}
