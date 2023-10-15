package org.foxesworld.newengine.gui;

import com.google.gson.Gson;
import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.AppFrame;
import org.foxesworld.newengine.gui.attributes.ComponentAttributes;
import org.foxesworld.newengine.gui.attributes.FrameAttributes;
import org.foxesworld.newengine.gui.attributes.OptionGroups;
import org.foxesworld.newengine.gui.components.Components;
import org.foxesworld.newengine.gui.components.frame.Frame;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

public class GuiBuilder {

    private final HashMap<String, List<Component>> componentsMap = new HashMap<>();
    private final HashMap<String, JPanel> panelsMap = new HashMap<>();
    private final Frame frame;
    private final Components components;

    public GuiBuilder(AppFrame appFrame) {
        APP.LOGGER.debug("=== GUI BUILDER ===");
        this.frame = appFrame.getFrame();
        this.components = new Components(appFrame);
    }

    /*
     * Current method is indexing a form file
     * accepting args (String framePath, boolean streamType)
     * */
    public void buildGui(String framePath, boolean inputStream) {
        Gson gson = new Gson();
        FrameAttributes frameAttributes;
        if (inputStream) {
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

        //WIP
        if (framePath.endsWith("frame.json")) {
            //BUILDING A FRAME
            frame.buildFrame(frameAttributes);
        } else {
            //BUILDING FRAME GROUPS
            this.buildComponents(frameAttributes.groups);
        }
    }

    /*
    * Method for building components, requires a components Map
    * */
    private void buildComponents(Map<String, OptionGroups> groups) {
        if (groups != null) {
            for (Map.Entry<String, OptionGroups> entry : groups.entrySet()) {
                String componentGroup = entry.getKey();
                APP.LOGGER.debug("Building "+componentGroup + " group");
                OptionGroups optionGroups = entry.getValue();
                JPanel parentPanel = frame.getPanel().createGroupPanel(optionGroups.panelOptions, componentGroup);

                this.createComponents(optionGroups.childrenComponents, parentPanel, componentGroup);

                parentPanel.setVisible(false);
                frame.getContentPanel().add(parentPanel);
                panelsMap.put(componentGroup, parentPanel);
            }
            frame.getFrame().setVisible(true);
        }
    }


    /*
     * This method builds a list of components with
     * ComponentAttributes and adding them all to a parent panel
     * */
    private void createComponents(List<ComponentAttributes> componentList, JPanel parentPanel, String parentGroupName) {
        for (ComponentAttributes componentAttributes : componentList) {
            JComponent component = this.components.createComponent(componentAttributes, componentAttributes.componentType);
            parentPanel.add(component);
            this.addComponentToMap(parentGroupName, component);
        }
    }


    private void addComponentToMap(String groupId, Component component) {
        if (!componentsMap.containsKey(groupId)) {
            componentsMap.put(groupId, new ArrayList<>());
        }
        componentsMap.get(groupId).add(component);
    }

    public List<Component> getComponentsMap(String key) {
        return componentsMap.get(key);
    }

    public HashMap<String, JPanel> getPanelsMap() {
        return panelsMap;
    }
}