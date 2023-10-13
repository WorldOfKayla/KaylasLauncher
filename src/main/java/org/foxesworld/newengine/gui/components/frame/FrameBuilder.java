package org.foxesworld.newengine.gui.components.frame;

import com.google.gson.Gson;
import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.gui.AppFrame;
import org.foxesworld.newengine.gui.components.button.ButtonStyle;
import org.foxesworld.newengine.gui.components.button.ButtonStyleFactory;
import org.foxesworld.newengine.gui.components.button.StyledButton;
import org.foxesworld.newengine.gui.components.label.LabelStyle;
import org.foxesworld.newengine.gui.components.label.LabelStyleFactory;
import org.foxesworld.newengine.gui.components.label.StyledLabel;
import org.foxesworld.newengine.gui.components.progressBar.ProgressBarStyle;
import org.foxesworld.newengine.gui.components.progressBar.ProgressBarStyleFactory;
import org.foxesworld.newengine.gui.components.progressBar.StyledProgressBar;
import org.foxesworld.newengine.gui.components.textfield.StyledTextfield;
import org.foxesworld.newengine.gui.components.textfield.TextfieldStyle;
import org.foxesworld.newengine.gui.components.textfield.TextfieldStyleFactory;
import org.foxesworld.newengine.locale.LanguageProvier;

import javax.swing.*;
import java.awt.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class FrameBuilder {
    private final HashMap<String, List<Component>> componentsMap = new HashMap<>();
    private ButtonStyleFactory buttonStyleFactory;
    private AppFrame appFrame;
    private Dimension screenSize;
    private TextfieldStyleFactory textfieldStyleFactory;
    private ProgressBarStyleFactory progressBarStyleFactory;
    private LabelStyleFactory labelStyleFactory;
    private JProgressBar progressBar;
    private JLabel progressLabel;
    private final JFrame frame;
    private final LanguageProvier LANG;

    public FrameBuilder(AppFrame appFrame) {
        APP app = appFrame.getApp();
        this.frame = appFrame.getFrame();
        this.appFrame = appFrame;
        this.LANG = app.getLANG();
    }

    public void buildGui(String framePath, boolean inputStream) {
        Gson gson = new Gson();
        InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(FrameBuilder.class.getClassLoader().getResourceAsStream(framePath)), StandardCharsets.UTF_8);
        FrameAttributes frameAttributes = gson.fromJson(reader, FrameAttributes.class);

        if (framePath.endsWith("assets/frames/frame.json")) {
            this.frameInit(frameAttributes);
        } else {

            for (Map.Entry<String, List<ComponentAttributes>> entry : frameAttributes.groups.entrySet()) {
                String componentGroup = entry.getKey();
                List<ComponentAttributes> components = entry.getValue();
                for (ComponentAttributes componentAttributes : components) {
                    String componentType = componentAttributes.componentType;
                    JComponent component = createComponent(componentAttributes, componentType);
                    this.addComponentToMap(componentGroup, component);
                }
            }
            frame.setVisible(true);
        }
    }

    private void frameInit(FrameAttributes frameAttributes){

        frame.setTitle(LANG.getString(frameAttributes.title));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(frameAttributes.width, frameAttributes.height);
        frame.setResizable(frameAttributes.resizable);
        frame.setUndecorated(frameAttributes.undecorated);
        frame.setContentPane(new JLabel(new ImageIcon("/assets/bg_summer.png")));

        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - frame.getWidth()) / 2;
        int y = (screenSize.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);
        frame.setLayout(null);
    }

    private JComponent createComponent(ComponentAttributes componentAttributes, String componentType) {
        switch (componentType) {
            case "progressBar" -> {
                progressBarStyleFactory = new ProgressBarStyleFactory(appFrame.getElementStyles().get("progressBar").get(componentAttributes.componentStyle));
                ProgressBarStyle progressBarStyle = this.progressBarStyleFactory.getProgressBarStyle();
                StyledProgressBar progressBar = new StyledProgressBar(progressBarStyle);
                progressBar.setBounds(componentAttributes.xPos, componentAttributes.yPos, componentAttributes.width, componentAttributes.height);
                return progressBar;
            }

            case "label" -> {
                labelStyleFactory = new LabelStyleFactory(appFrame.getElementStyles().get("label").get(componentAttributes.componentStyle));
                LabelStyle labelStyle = this.labelStyleFactory.getLabelStyle();
                StyledLabel label = new StyledLabel(LANG.getString(componentAttributes.localeKey), labelStyle);
                labelStyle.apply(label);
                label.setBounds(componentAttributes.xPos, componentAttributes.yPos, 90, 50);
                return label;
            }
            case "textField" -> {
                textfieldStyleFactory = new TextfieldStyleFactory(appFrame.getElementStyles().get("input").get(componentAttributes.componentStyle));
                TextfieldStyle textfieldStyle = this.textfieldStyleFactory.getTextfieldStyle();
                StyledTextfield textfield = new StyledTextfield(textfieldStyle);
                textfieldStyle.apply(textfield);
                textfield.setBounds(componentAttributes.xPos, componentAttributes.yPos, textfieldStyle.width, textfieldStyle.height);
                return textfield;
            }
            case "button" -> {
                buttonStyleFactory = new ButtonStyleFactory(appFrame.getElementStyles().get("button").get(componentAttributes.componentStyle));
                ButtonStyle buttonStyle = this.buttonStyleFactory.getButtonStyle();
                StyledButton button = new StyledButton(LANG.getString(componentAttributes.localeKey), buttonStyle);
                buttonStyle.apply(button);
                button.setBounds(componentAttributes.xPos, componentAttributes.yPos, buttonStyle.width, buttonStyle.height);
                button.addActionListener(e -> {
                });
                return button;
            }
            default -> throw new IllegalArgumentException("Unsupported component type: " + componentType);
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

    public void setProgressBar(JProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public void setProgressLabel(JLabel label) {
        this.progressLabel = label;
    }

    public JLabel getProgressLabel() {
        return this.progressLabel;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public Dimension getScreenSize() {
        return screenSize;
    }
}