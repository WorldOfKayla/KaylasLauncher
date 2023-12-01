package org.foxesworld.engine.gui;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.ComponentAttributes;
import org.foxesworld.engine.gui.components.ComponentFactory;
import org.foxesworld.engine.gui.components.ComponentFactoryListener;
import org.foxesworld.engine.gui.components.frame.FrameAttributes;
import org.foxesworld.engine.gui.components.frame.FrameConstructor;
import org.foxesworld.engine.gui.components.frame.OptionGroups;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

public class GuiBuilder implements ComponentFactoryListener {

    private final HashMap<String, List<JComponent>> componentsMap = new HashMap<>();
    private final HashMap<String, JPanel> panelsMap = new HashMap<>();
    private final HashMap<String, List<String>> childsNparents = new HashMap<>();
    private final FrameConstructor frameConstructor;
    private final ComponentFactory componentFactory;
    private GuiBuilderListener guiBuilderListener;

    public GuiBuilder(Engine engine) {
        engine.getLOGGER().debug("=== GUI BUILDER ===");
        this.frameConstructor = engine.getFrame();
        this.componentFactory = new ComponentFactory(engine);
    }

    /*
     * Method for building an interface based on a JSON file
     * Accepts the path to the file and the InputStream flag to specify the data source (resources or file)
     */
    public void buildGui(String framePath, boolean inputStream, JPanel parent) {
        Gson gson = new Gson();
        FrameAttributes frameAttributes;
        if (inputStream) {
            InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(FrameConstructor.class.getClassLoader().getResourceAsStream(framePath)), StandardCharsets.UTF_8);
            frameAttributes = gson.fromJson(reader, FrameAttributes.class);
        } else {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(framePath));
                frameAttributes = gson.fromJson(reader, FrameAttributes.class);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        // Building component group
        buildComponents(frameAttributes.groups, parent);
    }

    public List<Component> getAllChildComponents(String parentPanel) {
        List<Component> components = new ArrayList<>();
        for (String thisChild : childsNparents.get(parentPanel)) {
            components.addAll(getComponentsMap().get(thisChild));
        }
        return components;
    }

    public JComponent getComponentById(String id) {
        for (Map.Entry<String, List<JComponent>> panelsMap : this.getComponentsMap().entrySet()) {
            String panelName = panelsMap.getKey();
            for (JComponent component : panelsMap.getValue()) {
                if (component.getName().equals(id)) {
                    return component;
                }
            }
        }
        return null;
    }

    public void setLabelText(String componentId, String text){
        JComponent component = this.getComponentById(componentId);
        if(component instanceof  JLabel) {
            ((JLabel) component).setText(text);
        }
    }

    /*
     * Method for building componentFactory based on a JSON structure
     */
    private void buildComponents(Map<String, OptionGroups> groups, JPanel parentPanel) {
        if (groups != null) {
            guiBuilderListener.onPanelBuild(groups, parentPanel);
            for (Map.Entry<String, OptionGroups> entry : groups.entrySet()) {
                String componentGroup = entry.getKey();
                this.frameConstructor.getAppFrame().getLOGGER().debug("Building group " + componentGroup + " with parent " + parentPanel.getName());
                OptionGroups optionGroups = entry.getValue();
                JPanel thisPanel = frameConstructor.getPanel().createGroupPanel(optionGroups.getPanelOptions(), componentGroup);
                thisPanel.setName(componentGroup);
                thisPanel.setVisible(optionGroups.getPanelOptions().isVisible());
                this.createComponents(optionGroups.getChildComponents(), thisPanel, thisPanel.getName());
                if (!this.getPanelsMap().containsKey(componentGroup)) {
                    parentPanel.add(thisPanel);
                    getPanelsMap().put(componentGroup, thisPanel);
                }
                buildComponents(optionGroups.getGroups(), thisPanel); // Recursive call for nested groups
                childsNparents.computeIfAbsent(parentPanel.getName(), k -> new ArrayList<>()).add(thisPanel.getName());
            }
        }
    }

    /*
     * Method for building componentFactory based on a JSON structure
     */
    private void createComponents(List<ComponentAttributes> componentList, JPanel parentPanel, String parentGroupName) {
        this.componentFactory.setComponentFactoryListener(this);
        for (ComponentAttributes componentAttributes : componentList) {
            if (componentAttributes.getComponentType() != null) {
                JComponent component = this.componentFactory.createComponent(componentAttributes);
                parentPanel.add(component);
                this.addComponentToMap(parentGroupName, component);
            } else if (componentAttributes.getGroups() != null) {
                // Handle nested groups
                buildComponents(componentAttributes.getGroups(), parentPanel);
            } else if (componentAttributes.getReadFrom() != null) {
                // Handle reading from another JSON file
                buildGui(componentAttributes.getReadFrom(), true, parentPanel);
            }
        }
    }

    @Override
    public void onComponentCreation(ComponentAttributes componentAttributes) {
        if (componentAttributes.getInitialValue() != null) {
            this.getInitialData(componentAttributes);
        }
    }

    /* INFO
     * An experimental solution
     * Will do something with hardCoded scrollBox */
    private void getInitialData(ComponentAttributes componentAttributes) {
        String[] splitValue = componentAttributes.getInitialValue().split("#");
        switch (splitValue[0]) {
            case "config" -> {
                componentAttributes.setInitialValue(String.valueOf(this.componentFactory.engine.getCONFIG().getCONFIG().get(splitValue[1])));
            }
            case "user" -> {
                componentAttributes.setInitialValue(this.componentFactory.engine.getAuth().getAuthCredentials(splitValue[1]));
            }

            //EXP
            case "scrollBox" -> {
                switch (splitValue[1]) {
                    case "servers" -> {
                        this.componentFactory.setScrollBoxArr(this.componentFactory.engine.getAuth().getUserServersArray());
                        if (this.componentFactory.engine.getCONFIG().getSelectedServer() != 0) {
                            Object selectedIndex = this.componentFactory.engine.getCONFIG().getSelectedServer();
                            if(selectedIndex != null)
                            componentAttributes.setSelectedIndex((int) selectedIndex);
                        }
                    }
                }
            }
        }
    }

    /*
     * Method for adding a component to the component map
     */
    private void addComponentToMap(String groupId, JComponent component) {
        if (!componentsMap.containsKey(groupId)) {
            componentsMap.put(groupId, new ArrayList<>());
        }
        componentsMap.get(groupId).add(component);
    }

    /*
     * Getting a list of componentFactory by group name
     */
    public HashMap<String, List<JComponent>> getComponentsMap() {
        return componentsMap;
    }

    /*
     * Getting a panel map
     */
    public HashMap<String, JPanel> getPanelsMap() {
        return panelsMap;
    }

    public HashMap<String, List<String>> getChildsNparents() {
        return childsNparents;
    }

    public void setGuiBuilderListener(GuiBuilderListener guiBuilderListener) {
        this.guiBuilderListener = guiBuilderListener;
    }
}