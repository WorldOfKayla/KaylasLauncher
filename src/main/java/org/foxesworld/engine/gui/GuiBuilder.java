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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

public class GuiBuilder implements ComponentFactoryListener {

    private final Engine engine;
    private final HashMap<String, List<JComponent>> componentsMap = new HashMap<>();
    private final HashMap<String, JPanel> panelsMap = new HashMap<>();
    private final HashMap<JPanel, JComponent> panelsComponents = new HashMap<>();
    private final HashMap<String, JPanel> loadPanels = new HashMap<>();
    private final HashMap<String, List<String>> childsNparents = new HashMap<>();
    private final FrameConstructor frameConstructor;
    private final ComponentFactory componentFactory;
    private GuiBuilderListener guiBuilderListener;
    private boolean additionalPanelsBuilt = false;
    public GuiBuilder(Engine engine) {
        this.engine = engine;
        engine.getLOGGER().debug("=== GUI BUILDER ===");
        this.frameConstructor = engine.getFrame();
        this.componentFactory = new ComponentFactory(engine);
    }

    /*
     * Method for building GUI based on a JSON file
     * Accepts the path to the file and the InputStream flag to specify the data source (resources or file)
     */
    public void buildGui(String framePath, JPanel parent) {
        FrameAttributes frameAttributes;
        InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(FrameConstructor.class.getClassLoader().getResourceAsStream(framePath)), StandardCharsets.UTF_8);
        frameAttributes = new Gson().fromJson(reader, FrameAttributes.class);
        // Building component group
        buildPanels(frameAttributes.getGroups(), parent);
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

    @Deprecated
    public void setLabelText(String componentId, String text) {
        JComponent component = this.getComponentById(componentId);
        if (component instanceof JLabel) {
            ((JLabel) component).setText(text);
        }
    }

    /*
     * Method for building componentFactory based on a JSON structure
     */
    private void buildPanels(Map<String, OptionGroups> groups, JPanel parentPanel) {
        if (groups != null) {
            for (Map.Entry<String, OptionGroups> entry : groups.entrySet()) {
                String componentGroup = entry.getKey();
                OptionGroups optionGroups = entry.getValue();
                JPanel thisPanel = frameConstructor.getPanel().createGroupPanel(optionGroups.getPanelOptions(), componentGroup);
                thisPanel.setName(componentGroup);
                thisPanel.setVisible(optionGroups.getPanelOptions().isVisible());
                //All childElements are built here
                this.processChildComponents(optionGroups, thisPanel);

                //If panel with {nanme} is not already added
                if (!this.getPanelsMap().containsKey(componentGroup)) {
                    addPanelGroup(parentPanel, thisPanel);
                    guiBuilderListener.onPanelBuild(groups, componentGroup, parentPanel);
                }
                buildPanels(optionGroups.getGroups(), thisPanel); // Recursive call for nested groups
                childsNparents.computeIfAbsent(parentPanel.getName(), k -> new ArrayList<>()).add(thisPanel.getName());
            }
        }
    }

    /*
     * Method for building componentFactory based on a JSON structure
     */
    private void processChildComponents(OptionGroups optionGroups, JPanel parentPanel) {
        this.componentFactory.setComponentFactoryListener(this);
        for (ComponentAttributes componentAttributes : optionGroups.getChildComponents()) {
            if (componentAttributes.getComponentType() != null) {
                JComponent component = this.componentFactory.createComponent(componentAttributes, parentPanel);
                parentPanel.add(component);
                this.panelsComponents.put(parentPanel, component);
                this.addComponentToMap(parentPanel.getName(), component);
            } else if (componentAttributes.getGroups() != null) {
                // Handle nested groups
                buildPanels(componentAttributes.getGroups(), parentPanel);
            } else if (componentAttributes.getReadFrom() != null) {
                // Handle reading from another JSON file
                buildGui(componentAttributes.getReadFrom(), parentPanel);
            } else if(componentAttributes.getLoadPanel() != null && !componentAttributes.getLoadPanel().isEmpty()){
                //Saving to map to process when all panels are built
                this.loadPanels.put(componentAttributes.getLoadPanel(), parentPanel);
            }
        }
    }

    public void buildAdditionalPanels() {
        if (!additionalPanelsBuilt) {
            guiBuilderListener.onPanelsBuilt();
            Engine.LOGGER.debug(" == BUILDING ADDITIONAL PANELS ==");

            for (Map.Entry<String, JPanel> additional : this.loadPanels.entrySet()) {
                Engine.LOGGER.debug("Processing {}", additional.getKey());
                JPanel loadingPanel = this.panelsMap.get(additional.getKey());

                if (loadingPanel != null) {
                    this.addPanelGroup(additional.getValue(), loadingPanel);
                }
            }
            additionalPanelsBuilt = true;
        } else {
            Engine.LOGGER.error("Additional panels are already built!");
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
     * Will do something with hardCoded scrollBox
     * */
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
                            if (selectedIndex != null)
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
     * Getting a list of components attached to JPanel by group name
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

    public void addPanelToMap(JPanel panel) {
        this.panelsMap.put(panel.getName(), panel);
    }

    private void addPanelGroup(JPanel parent, JPanel child){
        parent.add(child);
        getPanelsMap().put(child.getName(), child);
    }

    public HashMap<String, List<String>> getChildsNparents() {
        return childsNparents;
    }

    public void setGuiBuilderListener(GuiBuilderListener guiBuilderListener) {
        this.guiBuilderListener = guiBuilderListener;
    }
}