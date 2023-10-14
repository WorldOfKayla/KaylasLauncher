package org.foxesworld.newengine.gui;

import com.google.gson.Gson;
import org.foxesworld.newengine.AppFrame;
import org.foxesworld.newengine.gui.attributes.ComponentAttributes;
import org.foxesworld.newengine.gui.attributes.FrameAttributes;
import org.foxesworld.newengine.gui.components.Components;
import org.foxesworld.newengine.gui.components.frame.Frame;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class GuiBuilder {

    private final HashMap<String, List<Component>> componentsMap = new HashMap<>();
    private final Frame frame;
    private final Components components;

    public GuiBuilder(AppFrame appFrame) {
        this.frame = appFrame.getFrame();
        this.components = new Components(appFrame);
    }

    public void buildGui(String framePath, boolean inputStream) {
        Gson gson = new Gson();
        FrameAttributes frameAttributes;
        if(inputStream) {
            InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(Frame.class.getClassLoader().getResourceAsStream(framePath)), StandardCharsets.UTF_8);
            frameAttributes = gson.fromJson(reader, FrameAttributes.class);
        } else {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(framePath));
                frameAttributes = gson.fromJson(reader, FrameAttributes.class);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }


        if (framePath.endsWith("frame.json")) {
            frame.buildFrame(frameAttributes);
        } else {
            for (Map.Entry<String, List<ComponentAttributes>> entry : frameAttributes.groups.entrySet()) {
                String componentGroup = entry.getKey();
                JPanel groupPanel = new JPanel() {};
                List<ComponentAttributes> componentList = entry.getValue();
                for (ComponentAttributes componentAttributes : componentList) {
                    String componentType = componentAttributes.componentType;
                    JComponent component = this.components.createComponent(componentAttributes, componentType);
                    groupPanel.add(component);
                    this.addComponentToMap(componentGroup, component);
                }
                groupPanel.setOpaque(false);
                groupPanel.setLayout(null);
                frame.getContentPanel().add(groupPanel);
            }
            frame.getFrame().setVisible(true);
        }
    }

    private void addComponentToMap(String groupId, Component component) {
        if (!componentsMap.containsKey(groupId)) {
            componentsMap.put(groupId, new ArrayList<>());
        }
        componentsMap.get(groupId).add(component);
    }

    public HashMap<String, List<Component>> getComponentsMap() {
        return componentsMap;
    }
}